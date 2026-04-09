package com.example.springailab.contextwindow;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class ContextWindowController {

    private static final int DEFAULT_MAX_TOKENS = 2_000;
    private final ContextTrimmer contextTrimmer;
    private final HistoryPruner historyPruner;
    private final SmartContextManager smartContextManager;

    @PostMapping("/api/context/trim")
    public ContextMessagesResponse trim(@RequestBody final ContextMessagesRequest contextMessagesRequest) {
        return new ContextMessagesResponse(
            toContextMessages(
                this.contextTrimmer.trim(
                    toMessages(
                        contextMessagesRequest.messages()
                    )
                )
            )
        );
    }

    @PostMapping("/api/context/prune")
    public ContextMessagesResponse prune(@RequestBody final PruneRequest pruneRequest) {
        final List<Message> historicalMessages = toMessages(pruneRequest.messages());
        final int maxTokens = pruneRequest.maxTokens() != null
            ? pruneRequest.maxTokens()
            : DEFAULT_MAX_TOKENS;
        if (maxTokens <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxTokens must be greater than 0");
        }
        return new ContextMessagesResponse(
            toContextMessages(
                this.historyPruner.prune(
                    historicalMessages,
                    maxTokens
                )
            )
        );
    }

    @PostMapping("/api/context/compact")
    public ContextMessagesResponse compact(@RequestBody final ContextMessagesRequest contextMessagesRequest) {
        return new ContextMessagesResponse(
            toContextMessages(
                this.smartContextManager.compact(
                    toMessages(
                        contextMessagesRequest.messages()
                    )
                )
            )
        );
    }

    private List<Message> toMessages(final List<ContextMessage> contextMessages) {
        if (CollectionUtils.isEmpty(contextMessages)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "messages must not be empty");
        }
        return contextMessages.stream()
            .map(this::toMessage)
            .toList();
    }

    private Message toMessage(final ContextMessage contextMessage) {
        if (contextMessage == null || StringUtils.isBlank(contextMessage.text())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "each message must include non-blank text");
        }
        if ("system".equalsIgnoreCase(contextMessage.role())) {
            return new SystemMessage(contextMessage.text());
        }
        if ("assistant".equalsIgnoreCase(contextMessage.role())) {
            return new AssistantMessage(contextMessage.text());
        }
        if ("user".equalsIgnoreCase(contextMessage.role())) {
            return new UserMessage(contextMessage.text());
        }
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "role must be one of: system, user, assistant"
        );
    }

    private List<ContextMessage> toContextMessages(final List<Message> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return List.of();
        }
        return messages.stream()
            .map(message -> new ContextMessage(resolveRole(message), message.getText()))
            .toList();
    }

    private String resolveRole(final Message message) {
        if (message instanceof SystemMessage) {
            return "system";
        }
        if (message instanceof AssistantMessage) {
            return "assistant";
        }
        if (message instanceof UserMessage) {
            return "user";
        }
        return String.valueOf(message.getMessageType());
    }

    public record ContextMessagesRequest(List<ContextMessage> messages) {}
    public record PruneRequest(List<ContextMessage> messages,
                               Integer maxTokens) {}
    public record ContextMessagesResponse(List<ContextMessage> messages) {}
    public record ContextMessage(String role,
                                 String text) {}
}
