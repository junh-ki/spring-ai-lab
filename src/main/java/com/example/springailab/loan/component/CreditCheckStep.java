package com.example.springailab.loan.component;

import com.example.springailab.loan.LoanContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Step 2: The Rules Engine
 * Not every step needs to be an LLM.
 * We can mix Java logic into the chain easily.
 * This step simulates a database lookup.
 */
@Order(2)
@Component
public class CreditCheckStep implements ChainStep {

    @Override
    public void execute(final LoanContext loanContext) {
        // In reality, this would call a repository
        final int score = (int) (Math.random() * 300) + 550; // Mock 550-850
        // Update state
        loanContext.setCreditScore(score);
        loanContext.log("Credit Score retrieved: " + score);
    }
}
