package com.example.springailab.token;

import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBudgetService {

    private static final long DAILY_TOKEN_LIMIT = 1_000_000; // Daily budget in tokens (e.g., 1 million tokens ~ $10 on GPT-4o)
    private final AtomicLong currentUsage = new AtomicLong(0); // Thread-safe counter

    public void recordUsage(final long tokensUsed) {
        final long total = this.currentUsage.addAndGet(tokensUsed);
        log.info("Current usage: {} / {}", total, DAILY_TOKEN_LIMIT);
    }

    public boolean isBudgetExceeded() {
        return this.currentUsage.get() >= DAILY_TOKEN_LIMIT;
    }

    /** Reset this daily via a Scheduled task */
    public void resetBudget() {
        this.currentUsage.set(0);
    }
}
