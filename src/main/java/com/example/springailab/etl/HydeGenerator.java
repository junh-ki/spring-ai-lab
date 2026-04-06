package com.example.springailab.etl;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HydeGenerator {

    private static final String PROMPT_TEMPLATE = """
            Please write a short passage to answer the question:
            "{query}"
            
            Do not answer nicely.
            Write it as if it were a formal document or a textbook excerpt.
            Include relevant technical keywords.
            """;
    private final ChatClient chatClient;

    public String generateHypothesis(final String userQuery) {
        return this.chatClient.prompt()
            .user(promptUserSpec ->
                promptUserSpec
                    .text(PROMPT_TEMPLATE)
                    .param("query", userQuery)
            )
            .call()
            .content();
    }
}
