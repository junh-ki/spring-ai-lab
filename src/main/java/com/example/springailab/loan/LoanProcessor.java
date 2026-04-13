package com.example.springailab.loan;

import com.example.springailab.loan.component.ChainStep;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanProcessor {

    private final List<ChainStep> chainSteps;

    public LoanContext processApplication(final String appId,
                                          final String text) {
        // 1. Initialize the empty State Object
        final LoanContext loanContext = new LoanContext(appId, text);
        // 2. Pass it through the chain
        this.chainSteps.forEach(chainStep -> {
            log.info("Executing: {}", chainStep.getClass().getSimpleName());
            chainStep.execute(loanContext);
        });
        // 3. Return the fully populated result
        return loanContext;
    }
}
