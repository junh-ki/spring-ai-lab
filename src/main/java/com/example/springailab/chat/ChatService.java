package com.example.springailab.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final double DEFAULT_TEMPERATURE = 0.7; // range 0-2
    private final ChatClient chatClient;

    public String generateOutput(final String message) {
        return this.chatClient.prompt()
            .user(message)
            .call()
            .content();
    }

    public Flux<ChatResponse> generateStream(final String message) {
        return this.chatClient.prompt()
            .user(message)
            .options(
                ChatOptions.builder()
                    .temperature(DEFAULT_TEMPERATURE)
                    .build()
            )
            .stream()
            .chatResponse();
    }
}
