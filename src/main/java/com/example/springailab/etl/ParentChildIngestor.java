package com.example.springailab.etl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParentChildIngestor {

    private final TokenTextSplitter parentSplitter = TokenTextSplitter.builder()
        .withChunkSize(2000)
        .withMinChunkSizeChars(400)
        .withMinChunkLengthToEmbed(10)
        .withMaxNumChunks(10000)
        .withKeepSeparator(true)
        .build();
    private final TokenTextSplitter childSplitter = TokenTextSplitter.builder()
        .withChunkSize(400)
        .withMinChunkSizeChars(80)
        .withMinChunkLengthToEmbed(10)
        .withMaxNumChunks(10000)
        .withKeepSeparator(true)
        .build();
    private final Map<String, Document> parentDocStore = new ConcurrentHashMap<>();
    private final VectorStore vectorStore;

    public void ingestDocument(final Resource resource) {
        final List<Document> rawDocuments = new TikaDocumentReader(resource).get();
        final List<Document> parentChunks = this.parentSplitter.apply(rawDocuments); // 1. Create Parent Chunks (Large Context)
        final List<Document> allChildChunks = parentChunks.stream()
            .flatMap(parentChunk -> {
                final String parentId = UUID.randomUUID().toString();
                parentChunk.getMetadata()
                    .put("doc_id", parentId); // Assign a unique ID to the parent
                this.parentDocStore.put(parentId, parentChunk); // Store the parent safely
                final List<Document> childChunks = this.childSplitter.apply(List.of(parentChunk)); // 2. Create Child Chunks (Searchable Fragments), derived strictly from THIS parent
                childChunks.forEach(childChunk -> childChunk.getMetadata().put("parent_id", parentId)); // Link Children back to Parent
                return childChunks.stream();
            })
            .toList();
        this.vectorStore.add(allChildChunks); // 3. Index ONLY the children in the Vector Store
        log.info(
            "Indexed {} children from {} parents.",
            allChildChunks.size(),
            parentChunks.size()
        );
    }
}
