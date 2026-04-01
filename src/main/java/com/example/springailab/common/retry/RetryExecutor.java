package com.example.springailab.common.retry;

import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RetryExecutor {

    public <T, E extends RuntimeException> T executeWithRetry(final Supplier<T> operationSupplier,
                                                              final Class<E> retryExceptionClass,
                                                              final int maxAttempts,
                                                              final Duration initialBackoff,
                                                              final Duration maxBackoff,
                                                              final Consumer<E> onRetryConsumer,
                                                              final Function<E, ? extends RuntimeException> onExhaustedFunction,
                                                              final String operationName) {
        final RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(this.retryPolicy(maxAttempts, retryExceptionClass));
        retryTemplate.setBackOffPolicy(this.exponentialBackOffPolicy(initialBackoff, maxBackoff));
        retryTemplate.registerListener(this.retryListener(operationName, retryExceptionClass, onRetryConsumer));
        return retryTemplate.execute(
            retryContext -> operationSupplier.get(),
            retryContext -> {
                final Throwable lastThrowable = retryContext.getLastThrowable();
                if (retryExceptionClass.isInstance(lastThrowable)) {
                    throw onExhaustedFunction.apply(retryExceptionClass.cast(lastThrowable));
                }
                if (lastThrowable instanceof RuntimeException lastRuntimeException) {
                    throw lastRuntimeException;
                }
                throw new IllegalStateException("Retry execution failed without RuntimeException", lastThrowable);
            }
        );
    }

    private <E extends RuntimeException> RetryPolicy retryPolicy(final int maxAttempts,
                                                                 final Class<E> retryExceptionClass) {
        return new SimpleRetryPolicy(maxAttempts, Map.of(retryExceptionClass, true), true);
    }

    private ExponentialBackOffPolicy exponentialBackOffPolicy(final Duration initialBackoff,
                                                              final Duration maxBackoff) {
        final ExponentialBackOffPolicy exponentialBackOffPolicy = new ExponentialBackOffPolicy();
        exponentialBackOffPolicy.setInitialInterval(initialBackoff.toMillis());
        exponentialBackOffPolicy.setMaxInterval(maxBackoff.toMillis());
        exponentialBackOffPolicy.setMultiplier(2.0d);
        return exponentialBackOffPolicy;
    }

    private <E extends RuntimeException> RetryListener retryListener(final String operationName,
                                                                     final Class<E> retryExceptionClass,
                                                                     final Consumer<E> onRetryConsumer) {
        return new RetryListener() {
            @Override
            public <T, EX extends Throwable> void onError(final RetryContext retryContext,
                                                          final RetryCallback<T, EX> retryCallback,
                                                          final Throwable throwable) {
                final String throwableMessage = throwable.getMessage();
                if (StringUtils.isNotBlank(throwableMessage)) {
                    log.warn("Retry attempt {} failed for {}: {}", retryContext.getRetryCount(), operationName, throwableMessage);
                } else {
                    log.warn("Retry attempt {} failed for {}", retryContext.getRetryCount(), operationName);
                }
                if (retryExceptionClass.isInstance(throwable)) {
                    onRetryConsumer.accept(retryExceptionClass.cast(throwable));
                }
            }
        };
    }
}
