package com.example.springailab.complexity;

import java.util.Optional;
import org.apache.tika.utils.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

@Service
public class ComplexityAnalyzer {

    private static final String TRIAGE_PROMPT = """
        Analyze the complexity of the following user request.
        
        Classify as 'HIGH_COMPLEXITY' if it requires:
        - Complex reasoning or math
        - Coding or debugging
        - Creative writing or nuance
        - Legal or medical analysis
        
        Classify as 'LOW_COMPLEXITY' if it is:
        - A simple greeting
        - A factual lookup (e.g., 'Capital of France')
        - Text formatting or simple summarization
        
        Return ONLY the label.
        
        REQUEST: {input}
        """;
    private final ChatClient fastClient;

    public ComplexityAnalyzer(final ChatClient.Builder chatClientBuilder) {
        this.fastClient = chatClientBuilder
            .defaultOptions(
                OpenAiChatOptions.builder()
                    .model("gpt-4o-mini")
            )
            .build();
    }

    public boolean isComplex(final String userRequest) {
        return "HIGH_COMPLEXITY".equalsIgnoreCase(
            Optional.ofNullable(
                this.fastClient.prompt()
                    .user(promptUserSpec ->
                        promptUserSpec
                            .text(TRIAGE_PROMPT)
                            .param("input", userRequest)
                    )
                    .call()
                    .content()
                )
                .orElse(StringUtils.EMPTY)
                .trim()
        );
    }
}
