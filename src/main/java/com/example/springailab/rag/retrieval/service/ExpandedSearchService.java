package com.example.springailab.rag.retrieval.service;

import com.example.springailab.rag.retrieval.QueryExpander;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExpandedSearchService {

    private final QueryExpander queryExpander;
    private final VectorStore vectorStore;

    public List<Document> search(final String userQuery) {
        // 1. Generate variations, and add the original query to the mix just in case
        final List<String> queries = this.queryExpander.expand(userQuery);
        queries.add(userQuery);
        // 2. Execute searches (using parallel stream for speed). We use a Map to keep unique docs by ID
        final Map<String, Document> uniqueDocuments = new ConcurrentHashMap<>();
        queries.parallelStream()
            .forEach(query -> {
                final List<Document> resultDocuments = this.vectorStore.similaritySearch(
                    SearchRequest.builder()
                        .query(userQuery)
                        .topK(3)
                        .build()
                );
                resultDocuments.forEach(resultDocument ->
                    uniqueDocuments.put(resultDocument.getId(), resultDocument));
            });
        // 3. Return the deduplicated list
        return new ArrayList<>(uniqueDocuments.values());
    }
}
