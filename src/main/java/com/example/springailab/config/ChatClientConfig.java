package com.example.springailab.config;

import com.example.springailab.advisor.PersonaAdvisor;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ChatClientConfig {

    private final PersonaAdvisor personaAdvisor;

    @Bean
    public ChatClient chatClient(final ChatClient.Builder chatClientBuilder,
                                 final ChatMemory chatMemory) {
        return chatClientBuilder
            .defaultSystem("You are a helpful assistant.")
            .defaultAdvisors(
                this.personaAdvisor, // persona line to be applied before memory runs
                MessageChatMemoryAdvisor
                    .builder(chatMemory)
                    .build(),
                new SimpleLoggerAdvisor()
            )
            .build();
    }
}
