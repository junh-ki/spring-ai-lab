package com.example.springailab.agent;

import com.example.springailab.booking.BookingToolService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AgentController {

    private final ChatClient chatClient;
    private final BookingToolService bookingToolService;

    @GetMapping("/agent/chat")
    public String chat(@RequestParam final String message) {
        return this.chatClient.prompt()
            .user(message)
            .tools(this.bookingToolService) // Register the entire bean. Spring AI will scan it for @Tool methods.
            .call()
            .content();
    }
}
