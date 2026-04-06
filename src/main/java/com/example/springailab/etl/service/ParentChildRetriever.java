package com.example.springailab.etl.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParentChildRetriever {

    private final Map<String, Document> parentDocStore;
    private final VectorStore vectorStore;

    public List<Document> retrieve(final String query) {
        final List<Document> similarChildDocs = this.vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(5)
                .build()
        );
        final Set<String> parentIds = similarChildDocs.stream()
            .map(childDoc -> (String) childDoc.getMetadata().get("parent_id"))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        return parentIds.stream()
            .map(this.parentDocStore::get)
            .filter(Objects::nonNull)
            .toList();
    }
}
