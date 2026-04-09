package com.example.springailab.contextwindow;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SmartContextManager {

    private static final int MAX_LIMIT = 20;
    private final ConversationSummarizer conversationSummarizer;
    private String currentSummary = ""; // PoC State: The running summary of everything prior

    public List<Message> compact(final List<Message> fullHistoricalMessages) {
        if (CollectionUtils.isEmpty(fullHistoricalMessages)
            || fullHistoricalMessages.size() <= MAX_LIMIT) {
            return fullHistoricalMessages;
        }
        // 1. Slice off the oldest chunk (excluding system): Let's say we compress messages from 1 to 10
        final List<Message> messagesToCompress = fullHistoricalMessages.subList(1, 11);
        // 2. Add the previous summary to the context so the AI can merge old + new facts
        messagesToCompress.addFirst(
            new SystemMessage(
                "Previous Summary: " + this.currentSummary
            )
        );
        // 3. Generate new summary
        final Message newSummaryMsg = this.conversationSummarizer.summarize(messagesToCompress);
        this.currentSummary = newSummaryMsg.getText();
        // 4. Construct the new lightweight history
        final List<Message> newHistoricalMessages = new ArrayList<>();
        newHistoricalMessages.add(fullHistoricalMessages.getFirst()); // Add original Identity
        newHistoricalMessages.add(newSummaryMsg); // Add updated memory
        newHistoricalMessages.addAll(
            fullHistoricalMessages.subList(11, fullHistoricalMessages.size())
        ); // add the remaining recent messages (11 to End)
        return newHistoricalMessages;
    }
}
