package com.example.springailab.advisor;

import com.example.springailab.config.AiProperties;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.stereotype.Component;

/**
 * Chat client advisor that appends a configurable style line to the system prompt on each request,
 * so the model keeps any existing instructions and adds persona-specific tone without replacing them.
 */
@Component
@NullMarked
@RequiredArgsConstructor
public class PersonaAdvisor implements BaseAdvisor {

    private final AiProperties aiProperties;

    @Override
    public ChatClientRequest before(final ChatClientRequest chatClientRequest,
                                    final AdvisorChain advisorChain) {
        return chatClientRequest.mutate()
            .prompt(
                chatClientRequest.prompt()
                    .augmentSystemMessage(systemMessage ->
                        systemMessage.mutate()
                            .text(systemMessage.getText() + "\nStyle: " + this.aiProperties.persona())
                            .build()
                    )
            )
            .build();
    }

    @Override
    public ChatClientResponse after(final ChatClientResponse chatClientResponse, final AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    @Override
    public String getName() {
        return "PersonaAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
