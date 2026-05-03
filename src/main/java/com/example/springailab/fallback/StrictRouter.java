package com.example.springailab.fallback;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StrictRouter {

    private static final String ROUTER_PROMPT = """
        You are an intent classification system for a Bank.
        Classify the user input into ONE of these categories:
        
        1. ACCOUNT_ACCESS: Login, password reset, unlock account.
        2. TRANSACTIONS: Transfer money, check balance, view history.
        3. UNKNOWN: Use this for ANY input that does not strictly match the above. This includes:
           - Greetings (Hello, Hi)
           - Out of scope topics (Weather, Sports, Pizza)
           - Ambiguous requests (Help me, It's broken)
        Return ONLY the category name.
        
        INPUT: {input}
        """;
    private final ChatClient chatClient;

    public String route(final String input) {
        return Optional.ofNullable(this.chatClient.prompt()
                .user(promptUserSpec ->
                    promptUserSpec
                        .text(ROUTER_PROMPT)
                        .param("input", input)
                )
                .call()
                .content())
            .orElse(StringUtils.EMPTY)
            .trim();
    }
}
