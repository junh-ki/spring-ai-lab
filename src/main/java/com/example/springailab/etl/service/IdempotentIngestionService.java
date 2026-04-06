package com.example.springailab.etl.service;

import com.example.springailab.etl.IdempotentMetadataEnricher;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotentIngestionService {

    private final IdempotentMetadataEnricher idempotentMetadataEnricher;
    private final VectorStore vectorStore;

    public void ingest(final Resource file) {
        // 1. Extract
        final TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(file);
        final List<Document> rawDocs = tikaDocumentReader.get();
        // 2. Transform (Split + Hash IDs)
        final TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder().build();
        final List<Document> deduplicatedDocs = this.idempotentMetadataEnricher.apply(
            tokenTextSplitter.apply(rawDocs)
        );
        // 3. Load: This triggers the embedding API calls and writes to the database (Upsert)
        this.vectorStore.add(deduplicatedDocs); // If these IDs exist, the store updates them. If not, it inserts them. -> No duplicates
        log.info("Processed {} items safely.", deduplicatedDocs.size());
    }
}
