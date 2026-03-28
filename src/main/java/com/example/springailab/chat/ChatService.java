package com.example.springailab.chat;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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

    public Flux<ChatResponse> generateStream(final String message) {
        final Prompt prompt = new Prompt(new UserMessage(message));
        return this.chatModel.stream(prompt);
    }
}
