package com.example.springailab.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    ChatClient chatClient(final ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder
            .defaultSystem("Your are a helpful assistant.")
            .build();
    }
}
