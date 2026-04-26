package com.example.springailab.gateway.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HotelAgent implements DomainAgent {

    private final ChatClient chatClient;

    @Autowired
    public HotelAgent(final ChatClient.Builder chatClientBuilder,
                      final HotelToolService hotelToolService) {
        this.chatClient = chatClientBuilder
            .defaultSystem("""
                You are a Hotel Expert...
                """)
            .defaultTools(hotelToolService) // Only access hotel tools
            .build();
    }

    @Override
    public String getName() {
        return "HotelAgent";
    }

    @Override
    public String process(final String request) {
        return this.chatClient.prompt()
            .user(request)
            .call()
            .content();
    }
}
