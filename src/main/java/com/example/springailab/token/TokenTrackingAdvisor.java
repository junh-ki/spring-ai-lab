package com.example.springailab.token;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;

@Slf4j
@NullMarked
@RequiredArgsConstructor
public class TokenTrackingAdvisor implements BaseAdvisor {

    private final TokenBudgetService tokenBudgetService;

    @Override
    public String getName() {
        return "TokenTracker";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public ChatClientRequest before(final ChatClientRequest chatClientRequest,
                                    final AdvisorChain advisorChain) {
        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(final ChatClientResponse chatClientResponse,
                                    final AdvisorChain advisorChain) {
        Optional.ofNullable(chatClientResponse.chatResponse())
            .map(chatResponse -> chatResponse.getMetadata().getUsage())
            .ifPresent(usage -> this.tokenBudgetService.recordUsage(usage.getTotalTokens()));
        return chatClientResponse;
    }
}
