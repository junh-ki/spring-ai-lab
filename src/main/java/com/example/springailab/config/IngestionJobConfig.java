package com.example.springailab.config;

import com.example.springailab.etl.DocumentProcessor;
import com.example.springailab.etl.VectorStoreWriter;
import java.util.Arrays;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Defines the document ingestion job. {@link JobRepository} and {@link org.springframework.batch.core.launch.JobOperator}
 * come from Spring Boot batch auto-configuration. This project has no JDBC starter, so no
 * {@code DataSourceTransactionManager} is auto-configured - the step uses a resourceless TX manager
 * (typical for in-memory / metadata-only batch with Spring Batch's map/job repository).
 */
@Configuration
public class IngestionJobConfig {

    @Bean
    public PlatformTransactionManager batchTransactionManager() {
        return new ResourcelessTransactionManager();
    }

    @Bean
    public Job documentIngestionJob(final JobRepository jobRepository,
                                    final Step ingestionStep) {
        return new JobBuilder("documentIngestionJob", jobRepository)
            .start(ingestionStep)
            .build();
    }

    @Bean
    public Step ingestionStep(final JobRepository jobRepository,
                              final PlatformTransactionManager platformTransactionManager,
                              final ItemReader<Resource> itemReader,
                              final DocumentProcessor documentProcessor,
                              final VectorStoreWriter vectorStoreWriter) {
        return new StepBuilder("ingestionStep", jobRepository)
            .<Resource, List<Document>>chunk(10)
            .transactionManager(platformTransactionManager)
            .reader(itemReader)
            .processor(documentProcessor)
            .writer(vectorStoreWriter)
            .faultTolerant() // Skip bad PDFs
            .skip(Exception.class)
            .skipLimit(5)
            .build();
    }

    @Bean
    public ItemReader<Resource> pdfReader(@Value("classpath:docs/*.pdf") final Resource[] resources) {
        // Feeds the files one by one into the pipeline
        return new ListItemReader<>(
            Arrays.asList(resources)
        );
    }
}
