package com.example.springailab.etl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentProcessor implements ItemProcessor<Resource, List<Document>> {

    private final IdempotentMetadataEnricher idempotentMetadataEnricher;
    private final TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder().build();

    @Override
    public List<Document> process(@NonNull final Resource resource) {
        // 1. Extract text using Tika: In a real batch job, handle exceptions gracefully to skip bad files without crashing
        final List<Document> rawDocuments = new TikaDocumentReader(resource).get();
        // 2. Transform (Hash IDs)
        final List<Document> deduplicatedDocs = this.idempotentMetadataEnricher.apply(rawDocuments);
        // 3. Split into chunks: This transforms 1 file into N chunks
        return this.tokenTextSplitter.apply(deduplicatedDocs);
    }
}
