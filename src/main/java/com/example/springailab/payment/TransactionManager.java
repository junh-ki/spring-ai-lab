package com.example.springailab.payment;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * In a real system, this would be a database table with a status column (PENDING, COMPLETED).
 * This is just a simple in-memory service as PoC.
 */
@Slf4j
@Service
public class TransactionManager {

    private final Map<String, PendingTx> pendingTransactions = new ConcurrentHashMap<>();

    public String createDraft(final String toAccount,
                              final double amount) {
        final String id = UUID.randomUUID().toString().substring(0, 5);
        this.pendingTransactions.put(id, new PendingTx(toAccount, amount));
        return id;
    }

    public Optional<PendingTx> getDraft(final String id) {
        return Optional.ofNullable(this.pendingTransactions.get(id));
    }

    public void complete(final String id) {
        this.pendingTransactions.remove(id);
        log.info("TRANSACTION {} COMMITTED.", id);
    }

    public record PendingTx(String toAccount,
                            double amount) {}
}
