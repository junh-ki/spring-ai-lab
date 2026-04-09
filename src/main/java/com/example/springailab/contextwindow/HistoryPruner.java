package com.example.springailab.contextwindow;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HistoryPruner {

    private final TokenEstimator tokenEstimator;

    public List<Message> prune(final List<Message> historicalMessages,
                               final int maxTokens) {
        final List<Message> prunedMessages = new ArrayList<>(historicalMessages);
        int currentTokenSum = calculateTokenSum(prunedMessages);
        while (currentTokenSum > maxTokens && CollectionUtils.isNotEmpty(prunedMessages)) {
            prunedMessages.removeFirst();
            currentTokenSum = calculateTokenSum(prunedMessages);
        }
        return prunedMessages;
    }

    private int calculateTokenSum(final List<Message> messages) {
        return messages.stream()
            .mapToInt(message -> this.tokenEstimator.estimate(message.getText()))
            .sum();
    }
}
