package com.example.springailab.etl;

import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VectorStoreWriter implements ItemWriter<List<Document>> {

    private final VectorStore vectorStore;

    @Override
    public void write(@NonNull final Chunk<? extends List<Document>> chunk) {
        final List<Document> batchPayload = chunk.getItems().stream()
            .flatMap(Collection::stream)
            .toList(); // Flatten the list of lists
        if (CollectionUtils.isEmpty(batchPayload)) {
            return;
        }
        this.vectorStore.add(batchPayload); // Single call to the Embedding Model and Database
        log.info("Wrote batch of {} vectors\n", batchPayload.size());
    }
}
