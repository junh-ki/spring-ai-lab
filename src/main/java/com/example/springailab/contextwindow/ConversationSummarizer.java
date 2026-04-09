package com.example.springailab.contextwindow;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConversationSummarizer {

    private static final String SUMMARY_PROMPT_TEMPLATE = """
        Compress the following conversation history into a concise summary.
        Preserve key facts, user names, and specific constraints.
        
        HISTORY:
        {history}
        
        SUMMARY:
        """;
    private final ChatClient chatClient;

    public Message summarize(final List<Message> messages) {
        final String conversationText = messages.stream()
            .map(message -> message.getMessageType() + ": " + message.getText())
            .collect(Collectors.joining("\n"));
        final String summaryText = this.chatClient.prompt()
            .user(promptUserSpec ->
                promptUserSpec
                    .text(SUMMARY_PROMPT_TEMPLATE)
                    .param("history", conversationText)
            )
            .call()
            .content();
        return new SystemMessage(
            "PREVIOUS CONVERSATION SUMMARY: " + summaryText
        );
    }
}
