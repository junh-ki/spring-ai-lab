package com.example.springailab.user;

import com.example.springailab.common.retry.RetryExecutor;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private static final int MAX_ATTEMPTS = 3;
    private static final Duration INITIAL_BACKOFF = Duration.ofMillis(100);
    private static final Duration MAX_BACKOFF = Duration.ofMillis(400);
    private final ChatClient chatClient;
    private final RetryExecutor retryExecutor;

    public Optional<UserRegistration> extract(final String text) {
        final BeanOutputConverter<UserRegistration> userRegistrationBeanOutputConverter = new BeanOutputConverter<>(UserRegistration.class);
        final String promptText = """
            Extract user details from the text.
            {format}
            """;
        final List<Message> historicalMessages = new ArrayList<>();
        historicalMessages.add(new UserMessage(text));
        historicalMessages.add(new SystemMessage(promptText.replace("{format}", userRegistrationBeanOutputConverter.getFormat())));
        return this.retryExecutor.executeWithRetry(
            () -> extractUserRegistration(historicalMessages, userRegistrationBeanOutputConverter),
            UserRegistrationExtractionException.class,
            MAX_ATTEMPTS,
            INITIAL_BACKOFF,
            MAX_BACKOFF,
            userRegistrationExtractionException -> appendRetryMessage(historicalMessages, userRegistrationExtractionException),
            userRegistrationExtractionException -> new UserRegistrationExtractionException(
                "Failed to extract data after " + MAX_ATTEMPTS + " attempts",
                userRegistrationExtractionException
            ),
            "extract user registration"
        );
    }

    private Optional<UserRegistration> extractUserRegistration(final List<Message> historicalMessages,
                                                               final BeanOutputConverter<UserRegistration> userRegistrationBeanOutputConverter) {
        try {
            final String responseContent = this.chatClient.prompt()
                .messages(historicalMessages)
                .call()
                .content();
            return Optional.ofNullable(responseContent)
                .map(userRegistrationBeanOutputConverter::convert);
        } catch (final RuntimeException runtimeException) {
            throw new UserRegistrationExtractionException(
                "Failed to convert model response into UserRegistration",
                runtimeException
            );
        }
    }

    private void appendRetryMessage(final List<Message> historicalMessages,
                                    final UserRegistrationExtractionException userRegistrationExtractionException) {
        historicalMessages.add(new AssistantMessage("failed_placeholder"));
        historicalMessages.add(new UserMessage(
            String.format(
                "Your previous response was invalid. Error: %s. Please correct the JSON and try again.",
                userRegistrationExtractionException.getMessage()
            )
        ));
    }
}
