package com.example.springailab.etl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.stereotype.Component;

@Component
public class MetadataEnricher implements DocumentTransformer {

    @Override
    public List<Document> apply(final List<Document> documents) {
        final String now = LocalDateTime.now().toString();
        return documents.stream()
            .map(document -> {
                final Map<String, Object> metadata = new HashMap<>(document.getMetadata()); // Create a mutable copy of the metadata
                metadata.putIfAbsent("ingested_at", now); // Add ingestion timestamp
                metadata.putIfAbsent("source", "unknown"); // Ensure a valid source exists
                return new Document(
                    document.getId(),
                    document.getText(),
                    metadata
                ); // Return new document with enriched metadata. Content remains unchanged.
            })
            .toList();
    }
}
