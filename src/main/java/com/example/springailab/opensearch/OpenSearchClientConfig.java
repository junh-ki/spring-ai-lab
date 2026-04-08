package com.example.springailab.opensearch;

import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * HTTP transport to a local OpenSearch process (loopback). There is no JVM-embedded
 * OpenSearch node; &quot;in-memory&quot; dev usage means a single-node cluster on this host
 * (RAM-backed on the OpenSearch side), e.g. Docker:
 * <pre>
 * docker run -p 9200:9200 -e discovery.type=single-node -e DISABLE_SECURITY_PLUGIN=true \
 *   opensearchproject/opensearch:2.11.0
 * </pre>
 */
@Configuration
public class OpenSearchClientConfig {

    @Bean
    public OpenSearchClient openSearchClient(@Value("${app.opensearch.host}") final String host,
                                             @Value("${app.opensearch.port}") final int port,
                                             @Value("${app.opensearch.scheme}") final String scheme) {
        return new OpenSearchClient(
            ApacheHttpClient5TransportBuilder
                .builder(new HttpHost(scheme, host, port))
                .setMapper(new JacksonJsonpMapper())
                .build()
        );
    }
}
