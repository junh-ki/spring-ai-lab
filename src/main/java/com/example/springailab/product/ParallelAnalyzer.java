package com.example.springailab.product;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ParallelAnalyzer {

    private final ChatClient chatClient;
    private final Executor aiExecutor;

    @Autowired
    public ParallelAnalyzer(final ChatClient chatClient,
                            @Qualifier("aiExecutor") final Executor aiExecutor) {
        this.chatClient = chatClient;
        this.aiExecutor = aiExecutor;
    }

    public AnalysisResult analyzeReview(final String reviewText) {
        // --- Task 1: Summarization ---
        final CompletableFuture<String> summaryFuture = CompletableFuture.supplyAsync(() ->
            this.chatClient.prompt()
                .user(promptUserSpec ->
                    promptUserSpec
                        .text("Summarize this in 1 sentence: {text}")
                        .param("text", reviewText)
                )
                .call()
                .content(),
            this.aiExecutor
        );
        // --- Task 2: Sentiment Analysis ---
        final CompletableFuture<Integer> sentimentFuture = CompletableFuture.supplyAsync(() -> {
            final String score = Optional.ofNullable(this.chatClient.prompt()
                .user(promptUserSpec ->
                    promptUserSpec
                        .text("Rate sentiment 1-10 (number only): {text}")
                        .param("text", reviewText)
                )
                .call()
                .content())
                .orElse(StringUtils.EMPTY);
            return Integer.parseInt(score.trim());
        }, this.aiExecutor);
        // --- Task 3: Feature Extraction ---
        final CompletableFuture<List<String>> featuresFuture = CompletableFuture.supplyAsync(() ->
            List.of(Optional.ofNullable(this.chatClient.prompt()
                    .user(promptUserSpec ->
                        promptUserSpec
                            .text("List features mentioned (comma sep): {text}")
                            .param("text", reviewText)
                    )
                    .call()
                    .content())
                .orElse(StringUtils.EMPTY)
                .split(",")),
            this.aiExecutor
        );
        // --- Task 4: Safety Check ---
        final CompletableFuture<Boolean> safetyFuture = CompletableFuture.supplyAsync(() ->
            Optional.ofNullable(this.chatClient.prompt()
                .user(promptUserSpec ->
                    promptUserSpec
                        .text("Is this safe? YES/NO: {text}")
                        .param("text", reviewText)
                )
                .call()
                .content())
                .map(String::trim)
                .orElse(StringUtils.EMPTY)
                .equalsIgnoreCase("YES"),
            this.aiExecutor
        );
        // --- Synchronization Point ---
        CompletableFuture
            .allOf(
                summaryFuture,
                sentimentFuture,
                featuresFuture,
                safetyFuture
            )
            .join();
        // --- Merge Results ---
        try { // At this point, we know all futures are done.
            return new AnalysisResult(
                summaryFuture.get(),
                sentimentFuture.get(),
                featuresFuture.get(),
                safetyFuture.get()
            );
        } catch (final InterruptedException | ExecutionException exception) {
            throw new RuntimeException("Analysis failed", exception);
        }
    }
}
