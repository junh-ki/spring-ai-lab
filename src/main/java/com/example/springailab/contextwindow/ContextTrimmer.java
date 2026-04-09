package com.example.springailab.contextwindow;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.stereotype.Component;

@Component
public class ContextTrimmer {

    private static final int TARGET_SIZE = 20; // We keep the system prompt and the last 10 exchanges

    public List<Message> trim(final List<Message> historicalMessages) {
        if (CollectionUtils.isEmpty(historicalMessages)
            || historicalMessages.size() <= TARGET_SIZE) {
            return historicalMessages;
        }
        final List<Message> trimmedHistoricalMessages = new ArrayList<>();
        // 1. Always preserve the System Message (Identify). Usually the first message in the list.
        final Message firstHistoricalMessage = historicalMessages.getFirst();
        if (firstHistoricalMessage instanceof SystemMessage) {
            trimmedHistoricalMessages.add(firstHistoricalMessage);
        }
        // 2. Calculate how many recent messages to keep. We take the sublist from (Size - Target) to End
        final int startIndex = Math.max(historicalMessages.size() - TARGET_SIZE, 1);
        final List<Message> recentMessages = historicalMessages.subList(
            startIndex,
            historicalMessages.size()
        );
        trimmedHistoricalMessages.addAll(recentMessages);
        return trimmedHistoricalMessages;
    }
}
