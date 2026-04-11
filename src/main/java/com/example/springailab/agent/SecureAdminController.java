package com.example.springailab.agent;

import com.example.springailab.banking.AdminBankingToolService;
import com.example.springailab.banking.BankingToolService;
import com.example.springailab.banking.PublicBankingToolService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SecureAdminController {

    private final ChatClient chatClient;
    private final PublicBankingToolService publicBankingToolService;
    private final AdminBankingToolService adminBankingToolService;

    @GetMapping("/secure-chat")
    public String chat(@RequestParam final String message,
                       @RequestHeader(defaultValue = "USER") final String role) {
        // 1. Start building the tool list with common tools
        final List<BankingToolService> bankingToolServices = new ArrayList<>();
        bankingToolServices.add(this.publicBankingToolService);
        // 2. Conditionally add sensitive tools
        if ("ADMIN".equalsIgnoreCase(role)) {
            bankingToolServices.add(this.adminBankingToolService);
        }
        // 3. Execute the prompt with the filtered list
        return this.chatClient.prompt()
            .user(message)
            .tools(bankingToolServices.toArray()) // .tools() accepts a varargs array of beans
            .call()
            .content();
    }
}
