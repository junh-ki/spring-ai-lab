package com.example.springailab.etl;

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
public class EtlPipelineService {

    private final VectorStore vectorStore;

    public void importPdf(final Resource pdf) {
        // 1. Extract
        final TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(pdf);
        final List<Document> rawDocs = tikaDocumentReader.get();
        // 2. Transform
        final TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder().build();
        final List<Document> chunks = tokenTextSplitter.apply(rawDocs);
        // 3. Load: This triggers the embedding API calls and writes to the database
        this.vectorStore.add(chunks);
        log.info("Imported {} chunks.", chunks.size());
    }
}
