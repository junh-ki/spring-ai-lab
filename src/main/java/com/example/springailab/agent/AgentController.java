package com.example.springailab.agent;

import com.example.springailab.flight.FlightBookingToolService;
import com.example.springailab.flight.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AgentController {

    private final ChatClient chatClient;
    private final FlightBookingToolService flightBookingToolService;
    private final FlightService flightService;

    @GetMapping("/agent/chat")
    public String chat(@RequestParam final String message) {
        return this.chatClient.prompt()
            .user(message)
            .tools(
                this.flightBookingToolService,
                this.flightService
            ) // Register the entire beans. Spring AI will scan them for @Tool methods.
            .call()
            .content();
    }
}
