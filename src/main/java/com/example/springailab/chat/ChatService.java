package com.example.springailab.chat;

import com.example.springailab.config.AiProperties;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int DEFAULT_RETRIEVAL_LIMIT = 10;
    private static final ParameterizedTypeReference<Map<String, Double>> CATEGORIZE_TYPE_REF = new ParameterizedTypeReference<>() {};
    private final AiProperties aiProperties;
    private final ChatClient chatClient;

    public String generateOutput(final String message,
                                 final String chatId) {
        log.debug("Chat completion request chatId={}", chatId);
        return this.chatClient.prompt()
            .user(message)
            .advisors(advisor ->
                advisor
                    .param(ChatMemoryConstant.CONVERSATION_ID, chatId)
                    .param(ChatMemoryConstant.RETRIEVE_SIZE, this.aiProperties.memoryRetrieveSize())
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
                    .temperature(this.aiProperties.strictTemperature())
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

    public Map<String, Double> categorize(final String text) {
        return this.chatClient.prompt()
            .user(user ->
                user.text("Categorize this text: {text}")
                    .param("text", text)
            )
            .call()
            .entity(CATEGORIZE_TYPE_REF);
    }

    public String chat(final String userId,
                       final String message) {
        return this.chatClient.prompt()
            .user(message)
            .advisors(advisorSpec ->
                advisorSpec
                    .param(ChatMemoryConstant.CONVERSATION_ID, userId) // Pass the conversation ID dynamically at runtime. The advisor intercepts this, looks up the history for 'userId', and injects it.
                    .param(ChatMemoryConstant.RESPONSE_SIZE, DEFAULT_RETRIEVAL_LIMIT)
            )
            .call()
            .content();
    }
}
