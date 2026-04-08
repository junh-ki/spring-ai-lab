package com.example.springailab.rag.retrieval.service;

import com.example.springailab.rag.retrieval.HydeGenerator;
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
public class HydeRetrieverService {

    private final HydeGenerator hydeGenerator;
    private final VectorStore vectorStore;

    public List<Document> search(final String originalQuery) {
        // 1. Generate the Hypothetical Document: This adds latency (one LLM call) but increases accuracy
        final String hypotheticalAnswer = this.hydeGenerator.generateHypothesis(originalQuery);
        log.info("Hypothesis: {}", hypotheticalAnswer);
        // 2. Search using the Hypothesis: We search for documents that look like the ANSWER, not documents that look like the QUESTION.
        return this.vectorStore.similaritySearch(
            SearchRequest.builder()
                .topK(5)
                .similarityThreshold(0.7) // We might use a slightly lower threshold as the hypothesis is an approximation
                .build()
        );
    }
}
