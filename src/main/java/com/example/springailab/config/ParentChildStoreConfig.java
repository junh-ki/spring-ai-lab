package com.example.springailab.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.ai.document.Document;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Only PoC. This should be replaced with something database-backed in prod.
 */
@Configuration
public class ParentChildStoreConfig {

    @Bean
    public Map<String, Document> parentDocStore() {
        return new ConcurrentHashMap<>();
    }
}
