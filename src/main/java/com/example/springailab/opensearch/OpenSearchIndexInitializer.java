package com.example.springailab.opensearch;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Ensures the hybrid keyword index exists with a minimal text mapping. Fails soft if
 * the cluster is unreachable (keyword leg of hybrid search will be empty).
 */
@Slf4j
@Component
public class OpenSearchIndexInitializer {

    private final String indexName;
    private final OpenSearchClient openSearchClient;

    @Autowired
    public OpenSearchIndexInitializer(@Value("${app.opensearch.index-name}") final String indexName,
                                      final OpenSearchClient openSearchClient) {
        this.indexName = indexName;
        this.openSearchClient = openSearchClient;
    }

    @PostConstruct
    public void ensureIndex() {
        try {
            final boolean exists = this.openSearchClient.indices()
                .exists(builder -> builder.index(this.indexName))
                .value();
            if (exists) {
                return;
            }
            this.openSearchClient.indices()
                .create(create ->
                    create
                        .index(this.indexName)
                        .mappings(mappings -> mappings
                            .properties("content", property -> property.text(text -> text))
                            .properties("title", property -> property.text(text -> text))
                        )
                );
            log.info("Created OpenSearch index {}", this.indexName);
        } catch (final Exception exception) {
            log.warn(
                "Could not ensure OpenSearch index '{}': {}",
                this.indexName,
                exception.getMessage()
            );
        }
    }
}
