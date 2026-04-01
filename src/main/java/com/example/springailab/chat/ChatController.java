package com.example.springailab.chat;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/ai/generate")
    public Map<String, String> generate(
        @RequestParam(value = "message", defaultValue = "Tell me a joke") final String message,
        @RequestParam(value = "chatId", defaultValue = "default") final String chatId) {
        return Map.of(
            "generation",
            this.chatService.generateOutput(message, chatId)
        );
    }

    @GetMapping("/ai/generateStream")
    public Flux<String> generateStream(
        @RequestParam(value = "message", defaultValue = "Tell me a joke") final String message) {
        return this.chatService.generateSecureStream(message);
    }

    @GetMapping("/ai/categorize")
    public Map<String, Double> categorize(@RequestParam final String text) {
        return this.chatService.categorize(text);
    }
}
