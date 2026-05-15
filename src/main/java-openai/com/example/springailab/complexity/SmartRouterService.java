package com.example.springailab.complexity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmartRouterService {

    private static final String MODEL_GPT_4O_MINI = "gpt-4o-mini";
    private static final String MODEL_GPT_4_TURBO = "gpt-4-turbo";
    private final ComplexityAnalyzer complexityAnalyzer;
    private final ChatClient chatClient;

    public String generateResponse(final String userRequest) {
        final boolean isComplex = this.complexityAnalyzer.isComplex(userRequest);
        final String selectedModel;
        final String reason;
        if (isComplex) {
            selectedModel = MODEL_GPT_4_TURBO;
            reason = "Reasoning required.";
        } else {
            selectedModel = MODEL_GPT_4O_MINI;
            reason = "Simple task.";
        }
        log.info("Routing to {} {}", selectedModel, reason);
        return this.chatClient.prompt()
            .user(userRequest)
            .options(
                OpenAiChatOptions.builder()
                    .model(selectedModel)
                    .temperature(
                        isComplex
                            ? 0.7
                            : 0.3
                    )
            )
            .call()
            .content();
    }

    public String generateHybridResponse(final String userRequest) {
        // Heuristic 1: If it's a code snippet, use the Smart Model
        if (userRequest.contains("public class")
            || userRequest.contains("def function")) {
            return callModel(userRequest, MODEL_GPT_4_TURBO);
        }
        // Heuristic 2: If the context is massive, use the 128k model
        if (userRequest.length() > 10_000) {
            return callModel(userRequest, MODEL_GPT_4_TURBO);
        }
        // Heuristic 3: If it's short, verify complexity with the Analyzer. This saves us from using the Analyzer on obvious cases.
        if (this.complexityAnalyzer.isComplex(userRequest)) {
            return callModel(userRequest, MODEL_GPT_4_TURBO);
        }
        // Default: Fast Model
        return callModel(userRequest, MODEL_GPT_4O_MINI);
    }

    private String callModel(final String userRequest,
                             final String model) {
        return this.chatClient.prompt()
            .user(userRequest)
            .options(
                OpenAiChatOptions.builder()
                    .model(model)
            )
            .call()
            .content();
    }
}
