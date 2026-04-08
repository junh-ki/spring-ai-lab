package com.example.springailab.rag.retrieval.service;

import com.example.springailab.opensearch.OpenSearchService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HybridSearchService {

    private static final int DEFAULT_TOP_K = 10;
    private static final int DOMINANCE_AVOIDANCE_CONSTANT = 60;
    private final VectorStore vectorStore;
    private final OpenSearchService openSearchService;

    public List<Document> search(final String query) {
        // 1. Run searches in parallel
        final CompletableFuture<List<Document>> vectorFuture = CompletableFuture.supplyAsync(() ->
            this.vectorStore.similaritySearch(
                SearchRequest.builder()
                    .query(query)
                    .topK(DEFAULT_TOP_K)
                    .build()
            )
        );
        final CompletableFuture<List<Document>> keywordFuture = CompletableFuture.supplyAsync(() ->
            this.openSearchService.searchKeywords(
                query,
                DEFAULT_TOP_K
            )
        );
        // 2. Fuse results using RRF (Reciprocal Rank Fusion)
        return fuseResults(
            vectorFuture.join(),
            keywordFuture.join()
        );
    }

    private List<Document> fuseResults(final List<Document> vectorDocuments,
                                       final List<Document> keywordDocuments) {
        final Map<String, Document> docsById = new HashMap<>();
        final Map<String, Double> scores = new HashMap<>();
        processDocsToFuseScores(vectorDocuments, docsById, scores);
        processDocsToFuseScores(keywordDocuments, docsById, scores);
        // 3. Sort by final fused score
        return scores.entrySet().stream()
            .sorted(
                Map.Entry.<String, Double>comparingByValue()
                    .reversed() // Descending order
            )
            .map(entry -> docsById.get(entry.getKey()))
            .toList();
    }

    private void processDocsToFuseScores(final List<Document> documents,
                                         final Map<String, Document> docsById,
                                         final Map<String, Double> scores) {
        for (int index = 0; index < documents.size(); index++) {
            final Document document = documents.get(index);
            final String docId = document.getId();
            docsById.putIfAbsent(docId, document);
            final double score = 1.0 / (DOMINANCE_AVOIDANCE_CONSTANT + (index + 1));
            scores.merge(docId, score, Double::sum);
        }
    }
}
