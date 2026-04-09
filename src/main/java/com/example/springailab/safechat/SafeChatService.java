package com.example.springailab.safechat;

import com.example.springailab.contextwindow.TokenEstimator;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SafeChatService {

    private static final int MAX_INPUT_TOKENS = 6000; // Safety buffer: leave room for the AI's response.
    private final TokenEstimator tokenEstimator;
    private final ChatClient chatClient;

    public String generateSafeResponse(final String userMessage,
                                       final String history) {
        final int totalTokens = this.tokenEstimator.estimate(userMessage)
            + this.tokenEstimator.estimate(history);
        if (totalTokens > MAX_INPUT_TOKENS) {
            throw new RuntimeException(
                "Request exceeds token limit. Current: "
                + totalTokens + ", Max: " + MAX_INPUT_TOKENS
            );
        }
        return this.chatClient.prompt()
            .system(history)
            .user(userMessage)
            .call()
            .content();
    }
}
