package com.example.springailab.banking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AdminBankingToolService extends BankingToolService {

    @Tool(description = "ADMIN ONLY: Freeze a specific account ID.")
    public String freezeAccount(@ToolParam(description = "The Account ID") final String accountId) {
        log.info("Admin action: Freezing account {}", accountId);
        return "Account " + accountId + " has been frozen.";
    }

    @Tool(description = "ADMIN ONLY: Process a refund transaction.")
    public String processRefund(@ToolParam(description = "Transaction ID") final String txId) {
        return "Refund initiated for "+ txId;
    }
}
