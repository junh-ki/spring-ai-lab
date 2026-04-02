package com.example.springailab.etl;

import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.stereotype.Component;

@Component
public class MetadataEnricher implements DocumentTransformer {

    @Override
    public List<Document> apply(final List<Document> documents) {
        return documents.stream()
            .map(document -> {
                final Map<String, Object> metadata = document.getMetadata();
                final String source = (String) metadata.get("source");
                return new Document(
                    "Source: " + source + "\n" + document.getText(),
                    metadata
                );
            })
            .toList();
    }
}
