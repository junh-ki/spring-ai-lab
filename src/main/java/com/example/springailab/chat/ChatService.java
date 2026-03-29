package com.example.springailab.chat;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final double DEFAULT_TEMPERATURE = 0.7; // range 0-2
    private static final int DEFAULT_RETRIEVE_SIZE = 100;
    private final ChatClient chatClient;

    public String generateOutput(final String message,
                                 final String chatId) {
        log.debug("Chat completion request chatId={}", chatId);
        return this.chatClient.prompt()
            .user(message)
            .advisors(advisor ->
                advisor
                    .param(ChatMemoryConstant.CONVERSATION_ID, chatId)
                    .param(ChatMemoryConstant.RETRIEVE_SIZE, DEFAULT_RETRIEVE_SIZE)
            )
            .call()
            .content();
    }

    public Flux<String> generateSecureStream(final String message) {
        log.debug("Chat stream request");
        return this.chatClient.prompt()
            .user(message)
            .options(
                ChatOptions.builder()
                    .temperature(DEFAULT_TEMPERATURE)
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
