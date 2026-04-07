package com.example.springailab.opensearch;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OpenSearchService {

    private final String indexName;
    private final OpenSearchClient openSearchClient;

    @Autowired
    public OpenSearchService(@Value("${app.opensearch.index-name}") final String indexName,
                             final OpenSearchClient openSearchClient) {
        this.indexName = indexName;
        this.openSearchClient = openSearchClient;
    }

    /**
     * Keyword search against the configured index ({@code content} and {@code title} fields).
     * Returns Spring AI {@link Document}s so IDs can align with the vector store for RRF in
     * {@link com.example.springailab.etl.service.HybridSearchService}.
     */
    public List<Document> searchKeywords(final String query, final int topK) {
        if (StringUtils.isBlank(query) || topK <= 0) {
            return List.of();
        }
        try {
            final SearchResponse<OpenSearchSource> searchResponse = this.openSearchClient.search(
                SearchRequest.of(builder -> builder
                    .index(this.indexName)
                    .size(topK)
                    .query(queryBuilder -> queryBuilder.multiMatch(match -> match
                        .query(query)
                        .fields("content", "title")
                        .fuzziness("AUTO")
                    ))
                ),
                OpenSearchSource.class
            );
            return searchResponse.hits().hits().stream()
                .map(hit -> {
                    final OpenSearchSource openSearchSource = hit.source();
                    final String text = extractText(openSearchSource);
                    final String id = StringUtils.isNotBlank(hit.id())
                        ? hit.id()
                        : UUID.randomUUID().toString();
                    final Map<String, Object> metadata = new HashMap<>();
                    metadata.put("source", "opensearch-keyword");
                    if (hit.score() != null) {
                        metadata.put("keywordScore", hit.score());
                    }
                    if (openSearchSource != null && StringUtils.isNotBlank(openSearchSource.title())) {
                        metadata.put("title", openSearchSource.title());
                    }
                    return new Document(id, text, metadata);
                })
                .toList();
        } catch (final IOException | OpenSearchException exception) {
            log.warn("OpenSearch keyword search failed: {}", exception.getMessage());
            return List.of();
        }
    }

    private String extractText(final OpenSearchSource source) {
        if (source == null || StringUtils.isBlank(source.content())) {
            return "";
        }
        return source.content();
    }

    private record OpenSearchSource(String content,
                                    String title) {}
}
