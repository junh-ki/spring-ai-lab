package com.example.springailab.creativechat;

import com.example.springailab.config.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreativeChatService {

    private final AiProperties aiProperties;
    private final ChatClient chatClient;

    public String writePoem(final String topic) {
        return this.chatClient.prompt()
            .user("Write a poem about " + topic)
            .options(
                ChatOptions.builder()
                    .temperature(this.aiProperties.creativeTemperature())
                    .build()
            )
            .call()
            .content();
    }
}
