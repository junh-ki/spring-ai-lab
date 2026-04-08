package com.example.springailab.rag.retrieval.service;

import com.example.springailab.rag.retrieval.reranking.RerankingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HighPrecisionSearchService {

    private final VectorStore vectorStore;
    private final RerankingService rerankingService;

    public List<Document> search(final String query) {
        final List<Document> candidateDocuments = this.vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(25)
                .build()
        );
        log.info("Initial candidates: {}", candidateDocuments.size());
        final List<Document> bestMatches = this.rerankingService.rerank(
            query,
            candidateDocuments,
            5
        );
        log.info("Reranked survivors: {}", bestMatches.size());
        return bestMatches;
    }
}
