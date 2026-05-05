package com.example.springailab.config;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Manual OTel SDK + Micrometer Tracer wiring for Spring Boot 4.1.0-RC1, which
 * doesn't publish a -tracing-otel autoconfig module yet. Spring AI's observation
 * autoconfig requires a {@link Tracer} bean to convert ChatModel observations
 * into spans — without this class it picks the no-op path and Langfuse stays empty.
 */
@Configuration
public class TracingConfig {

    @Bean
    @ConditionalOnMissingBean
    public OpenTelemetry openTelemetry(@Value("${management.otlp.tracing.endpoint}") final String endpoint,
                                       @Value("${management.otlp.tracing.headers.Authorization:}") final String authorization,
                                       @Value("${spring.application.name:spring-ai-lab}") final String serviceName,
                                       @Value("${management.tracing.sampling.probability:1.0}") final double samplingProbability) {
        final OtlpHttpSpanExporterBuilder exporterBuilder = OtlpHttpSpanExporter.builder()
            .setEndpoint(endpoint);
        if (!authorization.isBlank()) {
            exporterBuilder.addHeader("Authorization", authorization);
        }
        // sampling=0 (e.g. CI without Langfuse) becomes alwaysOff, so no spans
        // enter the queue and the exporter never tries to reach the OTLP endpoint.
        return OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(
                        BatchSpanProcessor.builder(exporterBuilder.build())
                            .setScheduleDelay(1, TimeUnit.SECONDS)
                            .build()
                    )
                    .setResource(
                        Resource.create(Attributes.builder()
                            .put("service.name", serviceName)
                            .build())
                    )
                    .setSampler(Sampler.traceIdRatioBased(samplingProbability))
                    .build()
            )
            .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public Tracer micrometerTracer(final OpenTelemetry openTelemetry) {
        return new OtelTracer(
            openTelemetry.getTracer("spring-ai-lab"),
            new OtelCurrentTraceContext(),
            event -> {}
        );
    }
}
