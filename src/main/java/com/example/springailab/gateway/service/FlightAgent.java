package com.example.springailab.gateway.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FlightAgent implements DomainAgent {

    private final ChatClient chatClient;

    @Autowired
    public FlightAgent(final ChatClient.Builder chatClientBuilder,
                       final FlightToolService flightToolService) {
        this.chatClient = chatClientBuilder
            .defaultSystem("""
                You are a Flight Booking Specialist.
                You can search for flights and book seats.
                If the user asks about hotels, refuse politely.
                """)
            .defaultTools(flightToolService) // Only access flight tools
            .build();
    }

    @Override
    public String getName() {
        return "FlightAgent";
    }

    @Override
    public String process(final String userRequest) {
        return this.chatClient.prompt()
            .user(userRequest)
            .call()
            .content();
    }
}
