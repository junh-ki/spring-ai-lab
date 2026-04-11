package com.example.springailab.banking;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class PublicBankingToolService extends BankingToolService {

    /**
     * In a real app, you get the user ID from SecurityContext
     */
    @Tool(description = "Get the current balance of the authenticated user's account.")
    public String getMyBalance() {
        return "Your balance is €5,430.00";
    }

    @Tool(description = "List the nearest ATM locations.")
    public String findAtms(@ToolParam(description = "City name") final String city) {
        return "Found 3 ATMs in " + city;
    }
}
