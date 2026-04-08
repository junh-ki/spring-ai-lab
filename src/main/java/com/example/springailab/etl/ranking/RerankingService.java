package com.example.springailab.etl.ranking;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class RerankingService {

    private final String authorizationHeaderValue;
    private final RestClient restClient;

    @Autowired
    public RerankingService(@Value("${rerank.api.key:${RERANK_API_KEY:demo-free-rerank-key}}") final String apiKey,
                            final RestClient.Builder restClientBuilder) {
        this.authorizationHeaderValue = "Bearer " + apiKey;
        this.restClient = restClientBuilder
            .baseUrl("https://api.cohere.ai/v1/rerank")
            .build();
    }

    public List<Document> rerank(final String query,
                                 final List<Document> documentCandidates,
                                 final int topN) {
        if (CollectionUtils.isEmpty(documentCandidates)) {
            return Collections.emptyList();
        }
        // 1. Call the Reranker API
        final RerankResponse rerankResponse = this.restClient.post()
            .header("Authorization", this.authorizationHeaderValue)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                Map.of(
                    "model", "rerank-english-v3.0",
                    "query", query,
                    "documents", documentCandidates.stream()
                        .map(Document::getText)
                        .toList(), // Prepare the payload: We map our Documents to simple strings for the API
                    "top_n", topN
                )
            )
            .retrieve()
            .body(RerankResponse.class);
        // 2. Reconstruct the list in the new order: The API returns indices, we map them back to objects
        if (rerankResponse == null || CollectionUtils.isEmpty(rerankResponse.results())) {
            log.warn("The rerank response contains no results.");
            return Collections.emptyList();
        }
        return rerankResponse.results().stream()
            .map(result -> {
                final double relevanceScore = result.relevance_score();
                if (relevanceScore < 0.5) {
                    return null;
                }
                final Document originalDocument = documentCandidates.get(result.index());
                originalDocument.getMetadata().put("rerank_score", relevanceScore);
                return originalDocument;
            })
            .filter(Objects::nonNull)
            .toList();
    }

    private record RerankResponse(List<Result> results) {}
    private record Result(int index,
                          double relevance_score) {}
}
