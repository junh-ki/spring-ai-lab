package com.example.springailab.chat;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
public class ChatService {

    private final int retrieveSize;
    private final double temperature;
    private final ChatClient chatClient;

    @Autowired
    public ChatService(@Value("${app.chat.memory.retrieve-size:100}") final int retrieveSize,
                       @Value("${spring.ai.ollama.chat.options.temperature:0.7}") final double temperature,
                       final ChatClient chatClient) {
        this.retrieveSize = retrieveSize;
        this.temperature = temperature;
        this.chatClient = chatClient;
    }

    public String generateOutput(final String message,
                                 final String chatId) {
        log.debug("Chat completion request chatId={}", chatId);
        return this.chatClient.prompt()
            .user(message)
            .advisors(advisor ->
                advisor
                    .param(ChatMemoryConstant.CONVERSATION_ID, chatId)
                    .param(ChatMemoryConstant.RETRIEVE_SIZE, this.retrieveSize)
            )
            .call()
            .content();
    }

    public Flux<String> generateSecureStream(final String message) {
        log.debug("Chat stream request");
        return this.chatClient.prompt()
            .user(message)
            .options(
                OllamaChatOptions.builder()
                    .temperature(this.temperature)
                    .build()
            )
            .stream()
            .content()
            .doOnSubscribe(subscription -> {
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    throw new AccessDeniedException(
                        "No user context found"
                    );
                }
            })
            .doOnNext(token -> {
                final String userName = Objects.requireNonNull(
                    SecurityContextHolder.getContext().getAuthentication()
                ).getName();
                log.info("Token for {}: {}\n", userName, token);
            });
    }
}
