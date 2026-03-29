package com.example.springailab.config;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.context.integration.Slf4jThreadLocalAccessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Micrometer context propagation: MDC + classpath {@link io.micrometer.context.ThreadLocalAccessor}
 * SPI (e.g. Spring Security), and a {@link ContextSnapshotFactory} for MVC reactive return types.
 */
@Configuration
public class ContextConfig {

    @Bean
    public ContextRegistry contextRegistry() {
        final ContextRegistry contextRegistry = ContextRegistry.getInstance();
        contextRegistry.registerThreadLocalAccessor(new Slf4jThreadLocalAccessor());
        contextRegistry.loadThreadLocalAccessors();
        return contextRegistry;
    }

    @Bean
    public ContextSnapshotFactory contextSnapshotFactory(final ContextRegistry contextRegistry) {
        return ContextSnapshotFactory.builder()
            .contextRegistry(contextRegistry)
            .build();
    }
}
