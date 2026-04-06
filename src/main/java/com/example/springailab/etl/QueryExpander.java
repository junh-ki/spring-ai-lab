package com.example.springailab.etl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QueryExpander {

    private static final String PROMPT_TEMPLATE_QUERY_EXPANSION = """
        You are an AI search assistant.
        Generate 3 different search queries based on the user question to retrieve relevant documents.
        
        Focus on different aspects: synonyms, technical terms, and related concepts.
        
        User Question: {query}
        
        {format}
        """;
    private static final String PROMPT_TEMPLATE_DECOMPOSITION = """
        You are an AI search assistant.
        Beak down this complex question into sub-questions that can be answered individually.
        
        User Question: {query}
        
        {format}
        """;
    private static final BeanOutputConverter<List<String>> BEAN_OUTPUT_CONVERTER =
        new BeanOutputConverter<>(new ParameterizedTypeReference<>() {});
    private final ChatClient chatClient;

    public List<String> expand(final String originalQuery) {
        return prompt(
            originalQuery,
            PROMPT_TEMPLATE_QUERY_EXPANSION
        );
    }

    public List<String> decompose(final String originalQuery) {
        return prompt(
            originalQuery,
            PROMPT_TEMPLATE_DECOMPOSITION
        );
    }

    private List<String> prompt(final String originalQuery,
                                final String promptTemplate) {
        return this.chatClient.prompt()
            .user(promptUserSpec ->
                promptUserSpec
                    .text(PROMPT_TEMPLATE_QUERY_EXPANSION)
                    .param("query", originalQuery)
                    .param("format", BEAN_OUTPUT_CONVERTER.getFormat())
            )
            .call()
            .entity(BEAN_OUTPUT_CONVERTER);
    }
}
