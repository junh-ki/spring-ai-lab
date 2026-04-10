package com.example.springailab.embedding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NullMarked;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Caching decorator for {@link EmbeddingModel}.
 * For each embedding input, this component first checks Redis for a cached vector using a stable
 * hash key. On cache miss it delegates to the provider-specific embedding model (for example
 * Ollama/OpenAI), stores the produced vector in Redis with TTL, and returns it.
 * Because this bean is {@code @Primary}, any Spring AI component using the application
 * {@link EmbeddingModel} (including {@code VectorStore} query embedding) will pass through this cache.
 */
@NullMarked
@Slf4j
@Primary // Make this the default EmbeddingModel to inject
@Component
public class CachingEmbeddingModel implements EmbeddingModel {

    private static final Base64.Encoder BASE_64_ENCODER = Base64.getEncoder();
    private final EmbeddingModel delegateEmbeddingModel;
    private final RedisTemplate<String, List<Double>> redisTemplate;

    @Autowired
    public CachingEmbeddingModel(@Qualifier("ollamaEmbeddingModel") final EmbeddingModel ollamaEmbeddingModel,
                                 final RedisTemplate<String, List<Double>> redisTemplate) {
        this.delegateEmbeddingModel = ollamaEmbeddingModel;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Returns embedding vector for raw text using Redis read-through caching.
     * @param text source text to embed
     * @return embedding vector
     */
    @Override
    public float[] embed(final String text) {
        return getCachedVector(
            computeHash(text),
            text
        );
    }

    /**
     * Returns embedding vector for a document using Redis read-through caching.
     * The cache key is based on resolved embedding content (embedding content first, then document text fallback).
     * @param document source document
     * @return embedding vector
     */
    @Override
    public float[] embed(final Document document) {
        final String embeddingContent = resolveEmbeddingContent(document);
        return getCachedVector(
            computeHash(embeddingContent),
            embeddingContent
        );
    }

    // Helper to create a safe Redis key from arbitrary text
    private String computeHash(final String input) {
        try {
            return "embedding:" + BASE_64_ENCODER.encodeToString(
                MessageDigest.getInstance("SHA-256")
                    .digest(
                        input.getBytes(StandardCharsets.UTF_8)
                    )
            );
        } catch (final NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new RuntimeException(noSuchAlgorithmException);
        }
    }

    private String resolveEmbeddingContent(final Document document) {
        final String embeddingContent = getEmbeddingContent(document);
        if (StringUtils.isNotBlank(embeddingContent)) {
            return embeddingContent;
        }
        final String documentText = document.getText();
        if (StringUtils.isNotBlank(documentText)) {
            return documentText;
        }
        throw new IllegalArgumentException("Document content must not be blank");
    }

    private float[] getCachedVector(final String cacheKey,
                                    final String content) {
        final List<Double> cachedVector = this.redisTemplate.opsForValue().get(cacheKey);
        if (CollectionUtils.isNotEmpty(cachedVector)) {
            log.info("Cache Hit! Skipping API call.");
            return toFloatArray(cachedVector);
        }
        log.info("Cache Miss. Calling Provider...");
        final float[] vector = this.delegateEmbeddingModel.embed(content);
        this.redisTemplate.opsForValue()
            .set(
                cacheKey,
                toDoubleList(vector),
                Duration.ofHours(24)
            );
        return vector;
    }

    private float[] toFloatArray(final List<Double> vector) {
        final float[] floatArray = new float[vector.size()];
        for (int index = 0; index < vector.size(); index++) {
            floatArray[index] = vector.get(index).floatValue();
        }
        return floatArray;
    }

    private List<Double> toDoubleList(final float[] vector) {
        return IntStream.range(0, vector.length)
            .mapToDouble(index -> vector[index])
            .boxed()
            .toList();
    }

    // Required interface methods pass through to delegate...
    @Override
    public EmbeddingResponse call(final EmbeddingRequest embeddingRequest) {
        // For simplicity, this example focuses on single-string embedding.
        // A production implementation would handle batch requests by checking cache for each item in the batch.
        return this.delegateEmbeddingModel.call(embeddingRequest);
    }
}
