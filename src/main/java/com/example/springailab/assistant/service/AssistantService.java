package com.example.springailab.assistant.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssistantService {

    private final ChatClient chatClient;

    public String ask(final String userMessage) {
        return this.chatClient.prompt()
            .user(userMessage)
            .call()
            .content();
    }
}
