package com.example.springailab.advisor;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.beans.factory.annotation.Value;

/**
 * Chat client advisor that appends a configurable style line to the system prompt on each request,
 * so the model keeps any existing instructions and adds persona-specific tone without replacing them.
 */
@NullMarked
@RequiredArgsConstructor
public class PersonaAdvisor implements BaseAdvisor {

    private final String persona;

    @Value("${app.chat.persona:Be concise and friendly.}")
    private String personaConfig;

    public PersonaAdvisor() {
        this.persona = this.personaConfig;
    }

    @Override
    public ChatClientRequest before(final ChatClientRequest chatClientRequest,
                                    final AdvisorChain advisorChain) {
        return chatClientRequest.mutate()
            .prompt(
                chatClientRequest.prompt()
                    .augmentSystemMessage(systemMessage ->
                        systemMessage.mutate()
                            .text(systemMessage.getText() + "\nStyle: " + this.persona)
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
