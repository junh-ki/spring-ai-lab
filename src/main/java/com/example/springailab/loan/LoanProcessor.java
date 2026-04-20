package com.example.springailab.loan;

import com.example.springailab.loan.component.ChainStep;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanProcessor {

    private final List<ChainStep> chainSteps;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LoanContext processApplication(final String appId,
                                          final String text) {
        // 1. Initialize the empty State Object
        final LoanContext loanContext = new LoanContext(appId, text);
        // 2. Pass it through the chain
        this.chainSteps.forEach(chainStep -> {
            log.info("Executing: {}", chainStep.getClass().getSimpleName());
            chainStep.execute(loanContext);
            try {
                final String snapshot = this.objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(loanContext);
                log.info("--- STATE AFTER {} ---", chainStep.getClass().getSimpleName());
                log.info(snapshot);
            } catch (final JsonProcessingException jsonProcessingException) {
                log.error(Arrays.toString(jsonProcessingException.getStackTrace()));
                return;
            }
        });
        // 3. Return the fully populated result
        return loanContext;
    }
}
