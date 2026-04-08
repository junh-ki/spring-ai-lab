package com.example.springailab.rag.retrieval.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SupportController {

    private final ChatClient chatClient;

    @Autowired
    public SupportController(@Qualifier("ragChatClient") final ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/support")
    public String ask(@RequestParam final String question) {
        // The advisor automatically injects the context before the model sees the question.
        return this.chatClient.prompt()
            .user(question)
            .call()
            .content();
    }
}
