package com.example.springailab.gateway;

import com.example.springailab.gateway.service.GatewayToolService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GatewayController {

    private final ChatClient gatewayClient;

    @Autowired
    public GatewayController(final ChatClient.Builder chatClientBuilder,
                             final GatewayToolService gatewayToolService) {
        this.gatewayClient = chatClientBuilder
            .defaultTools("""
                You are a Travel Concierge Gateway.
                Your job is to coordinate between specialized agents.
                
                RULES:
                1. Analyze the user's request.
                2. If it's about flights, use 'consultFlightAgent'.
                3. If it's about hotels, use 'consultHotelAgent'.
                4. If it's about both, call BOTH tools and combine the answers.
                5. Do NOT try to answer booking questions yourself.
                """)
            .defaultTools(gatewayToolService)
            .build();
    }

    @GetMapping("/gateway/chat")
    public String chat(@RequestParam final String message) {
        return this.gatewayClient.prompt()
            .user(message)
            .call()
            .content();
    }
}
