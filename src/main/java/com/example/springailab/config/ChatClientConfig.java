package com.example.springailab.config;

import com.example.springailab.advisor.PersonaAdvisor;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@RequiredArgsConstructor
public class ChatClientConfig {

    private final PersonaAdvisor personaAdvisor;

    @Bean
    @Primary
    public ChatClient chatClient(final ChatClient.Builder chatClientBuilder,
                                 final ChatMemory chatMemory) {
        return chatClientBuilder
            .defaultSystem("You are a helpful assistant.")
            .defaultAdvisors(
                this.personaAdvisor, // persona line to be applied before memory runs
                MessageChatMemoryAdvisor
                    .builder(chatMemory)
                    .build(),
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
}
