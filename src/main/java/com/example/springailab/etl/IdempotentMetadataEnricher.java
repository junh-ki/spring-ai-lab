package com.example.springailab.etl;

import com.example.springailab.common.util.HashUtils;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.stereotype.Component;

@Component
public class IdempotentMetadataEnricher implements DocumentTransformer {

    @Override
    public List<Document> apply(final List<Document> documents) {
        final String now = LocalDateTime.now().toString();
        return documents.stream()
            .map(document -> {
                final Map<String, Object> metadata = new HashMap<>(document.getMetadata()); // Create a mutable copy of the metadata
                metadata.putIfAbsent("ingested_at", now); // Add ingestion timestamp
                metadata.putIfAbsent("source", "unknown"); // Ensure a valid source exists
                final String documentContent = document.getText();
                final String uniqueDocContent = documentContent + metadata.get("source_filename"); // If unique field combinations matter
                return StringUtils.isNotBlank(documentContent)
                    ? new Document(
                        HashUtils.computeHash(uniqueDocContent), // Generate ID based on the text content
                        documentContent,
                        metadata
                    ) : null; // Return new document with enriched metadata. Content remains unchanged
            })
            .filter(Objects::nonNull)
            .toList();
    }
}
