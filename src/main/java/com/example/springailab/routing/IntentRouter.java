package com.example.springailab.routing;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IntentRouter {

    private static final String ROUTER_PROMPT = """
        You are a semantic classifier.
        Classify the user's input into exactly one of these categories:
        
        1. BILLING: Questions about invoices, payments, refunds, or pricing.
        2. TECHNICAL_SUPPORT: Bug reports, login issues, errors, system help.
        3. SALES: Pricing inquiries, demo requests, upgrade questions.
        4. GENERAL_CHAT: Greetings, small talk, or irrelevant inputs.

        Return ONLY the category name. Do not explain.
        
        USER INPUT: {input}
        """;
    private final ChatClient chatClient; // Use a fast, cheap model here (e.g., "gpt-4o-mini")

    public RoutingTarget route(final String userMessage) {
        final String response = Optional.ofNullable(
            this.chatClient.prompt()
                .user(promptUserSpec ->
                    promptUserSpec
                        .text(ROUTER_PROMPT)
                        .param("input", userMessage)
                )
                .call()
                .content()
        ).orElse(StringUtils.EMPTY);
        try {
            return RoutingTarget.valueOf(
                response.trim() // Clean the response (remove whitespace/punctuation)
                    .replace(".", StringUtils.EMPTY)
                    .toUpperCase()
            );
        } catch (final IllegalArgumentException illegalArgumentException) {
            return RoutingTarget.UNKNOWN; // Fallback if the LLM hallucinates a new category
        }
    }
}
