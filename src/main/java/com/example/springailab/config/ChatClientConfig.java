package com.example.springailab.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    ChatClient chatClient(final ChatClient.Builder chatClientBuilder,
                          final ChatMemory chatMemory) {
        return chatClientBuilder
            .defaultSystem("You are a helpful assistant.")
            .defaultAdvisors(
                MessageChatMemoryAdvisor
                    .builder(chatMemory)
                    .build(),
                new SimpleLoggerAdvisor()
            )
            .build();
    }
}
