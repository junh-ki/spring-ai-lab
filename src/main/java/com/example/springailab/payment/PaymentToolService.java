package com.example.springailab.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentToolService {

    private final TransactionManager transactionManager;

    @Tool(description = """
        Use this to initiate a money transfer.
        It returns a Transaction ID.
        DO NOT assume the transfer is done.
        You MUST present the ID to the user and ask for 'YES' to confirm.
        """)
    public String proposeTransfer(@ToolParam(description = "Recipient Name") final String recipient,
                                  @ToolParam(description = "Amount") final double amount) {
        return String.format(
            "Transfer of $%.2f to %s is DRAFTED. Transaction ID: %s. Status: PENDING_APPROVAL.",
            amount,
            recipient,
            this.transactionManager.createDraft(recipient, amount)
        );
    }

    @Tool(description = """
        Use this ONLY after the user explicitly approves a pending transaction ID.
        If the user says 'No' OR 'Cancel', do not call this.
        """)
    public String confirmTransfer(@ToolParam(description = "The Transaction ID") final String txId) {
        return this.transactionManager.getDraft(txId)
            .map(pendingTransaction -> {
                this.transactionManager.complete(txId);
                return "Success: Transaction " + txId + " processed.";
            })
            .orElseGet(() -> "Error: Transaction " + txId + " not found or expired.");
    }
}
