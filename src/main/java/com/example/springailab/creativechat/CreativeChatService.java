package com.example.springailab.creativechat;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreativeChatService {

    private static final double DEFAULT_TEMPERATURE = 0.9;
    private final ChatClient chatClient;

    public String writePoem(final String topic) {
        return this.chatClient.prompt()
            .user("Write a poem about " + topic)
            .options(
                ChatOptions.builder()
                    .temperature(DEFAULT_TEMPERATURE)
                    //.model()
                    .build()
            )
            .call()
            .content();
    }
}
