package com.example.springailab.config;

import com.example.springailab.advisor.PersonaAdvisor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ChatClientConfig {

    private final PersonaAdvisor personaAdvisor;

    /**
     * Demo / regression-test flag. When true, the primary {@link ChatClient}
     * skips the {@link MessageChatMemoryAdvisor}, simulating a memory regression
     * (e.g. an accidental refactor that drops the advisor). Used by
     * {@code promptfoo/regression-demo.sh} to produce a "broken" SUT against
     * which multi-turn promptfoo tests are expected to fail.
     * <p>
     * MUST remain {@code false} (default) in any production-like environment.
     */
    @Value("${app.demo.memory-disabled:false}")
    private boolean memoryDisabled;

    @Bean
    @Primary
    public ChatClient chatClient(final ChatClient.Builder chatClientBuilder,
                                 final ChatMemory chatMemory) {
        final ChatClient.Builder builder = chatClientBuilder
            .defaultSystem("You are a helpful assistant.");

        if (this.memoryDisabled) {
            log.warn(
                "Chat memory advisor DISABLED via app.demo.memory-disabled=true — "
                + "regression-demo mode. NEVER run with this flag in production."
            );
            return builder
                .defaultAdvisors(
                    this.personaAdvisor,
                    new SimpleLoggerAdvisor()
                )
                .build();
        }
        return builder
            .defaultAdvisors(
                this.personaAdvisor, // persona line to be applied before memory runs
                MessageChatMemoryAdvisor
                    .builder(chatMemory)
                    .build(), // The Advisor handles the heavy lifting of context management. It automatically retrieves history before the call and saves the new exchange after the call.
                new SimpleLoggerAdvisor()
            )
            .build();
    }

    @Bean
    public ChatClient ragChatClient(final ChatClient.Builder chatClientBuilder,
                                    final VectorStore vectorStore) {
        return chatClientBuilder
            .defaultSystem("You are a helpful assistant.")
            .defaultAdvisors(
                RetrievalAugmentationAdvisor.builder()
                    .documentRetriever(
                        VectorStoreDocumentRetriever.builder()
                            .vectorStore(vectorStore)
                            .topK(5)
                            .similarityThreshold(0.7)
                            .build() // Define the search logic once
                    )
                    .build()
            ) // The Advisor handles the "heavy lifting"
            .build();
    }

    @Bean
    public ChatClient customRagClient(final ChatClient.Builder chatClientBuilder,
                                      final VectorStore vectorStore,
                                      final QueryTransformer technicalDocQueryTransformer) {
        final String customPromptTemplate = """
            You are a generic support agent.
            Answer the user query using the following retrieved data when relevant.

            QUERY:
            {query}

            DATA:
            {question_answer_context}
            
            If the data is irrelevant, ignore it.
            """;
        return chatClientBuilder
            .defaultAdvisors(
                RetrievalAugmentationAdvisor.builder()
                    .queryTransformers(technicalDocQueryTransformer) // Rewrite
                    .documentRetriever(
                        VectorStoreDocumentRetriever.builder()
                            .vectorStore(vectorStore)
                            .topK(5)
                            .similarityThreshold(0.7)
                            .build() // Define the search logic once
                    )
                    .queryAugmenter(
                        ContextualQueryAugmenter.builder()
                            .promptTemplate(
                                PromptTemplate.builder()
                                    .template(customPromptTemplate)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            ) // The Advisors handle the "heavy lifting"
            .build();
    }

    @Bean
    public QueryTransformer technicalDocQueryTransformer() {
        return query -> {
            // Query has text + optional history/context. Keep history/context unless you need to change them.
            final String rewrittenText = query.text().trim() + " (technical documentation)";
            return Query.builder()
                .text(rewrittenText)
                .history(query.history())
                .context(query.context())
                .build();
        };
    }

    @Bean
    public ChatClient reasoningClient(final ChatClient.Builder chatClientBuilder,
                                      final VectorStore vectorStore) {
        return chatClientBuilder
            .defaultSystem("""
                You are an intelligent router agent.
                You have access to multiple overlapping tools.

                PROTOCOL:
                1. Analyze the user's specific terminology.
                2. THINK: "Does this request belong to domain A or B?"
                3. Select the most specific tool available.
                4. If no tool is a perfect match, ask the user for clarification.
                """)
            .defaultAdvisors(
                RetrievalAugmentationAdvisor.builder()
                    .documentRetriever(
                        VectorStoreDocumentRetriever.builder()
                            .vectorStore(vectorStore)
                            .topK(5)
                            .similarityThreshold(0.7)
                            .build() // Define the search logic once
                    )
                    .build()
            )
            .build();
    }
}
