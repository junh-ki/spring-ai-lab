package com.example.springailab.chat;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatModel chatModel;

    public String generateOutput(final String message) {
        final String chatModelOutput = this.chatModel.call(message);
        if (StringUtils.isBlank(chatModelOutput)) {
            throw new MissingChatOutputException();
        }
        return chatModelOutput;
    }
}
