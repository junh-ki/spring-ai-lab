package com.example.springailab.rag.ingestion.controller;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BatchController {

    private final JobOperator jobOperator;
    private final Job documentIngestionJob;

    @Autowired
    public BatchController(final JobOperator jobOperator,
                           @Qualifier("documentIngestionJob") final Job documentIngestionJob) {
        this.jobOperator = jobOperator;
        this.documentIngestionJob = documentIngestionJob;
    }

    @PostMapping("/run-ingestion")
    public JobExecution runIngestion() throws Exception {
        return this.jobOperator.start(
            this.documentIngestionJob,
            new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters()
        );
    }
}
