package com.example.springailab.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RoutingService {

    private final StrictRouter strictRouter;
    private final AccountAgent accountAgent;
    private final TransactionAgent transactionAgent;
    private final ChatClient generalChatClient;

    @Autowired
    public RoutingService(final StrictRouter strictRouter,
                          final AccountAgent accountAgent,
                          final TransactionAgent transactionAgent,
                          final FallbackToolService fallbackToolService,
                          final ChatClient.Builder chatClientBuilder) {
        this.strictRouter = strictRouter;
        this.accountAgent = accountAgent;
        this.transactionAgent = transactionAgent;
        this.generalChatClient = chatClientBuilder
            .defaultSystem("You are a helpful banking assistant. Guide the user to specific banking tasks.")
            .defaultTools(fallbackToolService)
            .build();
    }

    public String handleRequest(final String input) {
        final String route = this.strictRouter.route(input);
        log.info("Routed to: {}", route);
        return switch (route) {
            case "ACCOUNT_ACCESS" -> this.accountAgent.process(input);
            case "TRANSACTIONS" -> this.transactionAgent.process(input);
            case "UNKNOWN" -> handleFallback(input);
            default -> "System Error: Invalid route.";
        };
    }

    private String handleFallback(final String input) {
        return this.generalChatClient.prompt() // Instead of a hard error, we engage the user
            .system("""
                The user's intent was unclear or out of scope.
                Politely explain that you can only help with:
                1. Account Access
                2. Transactions
                
                If the user was just saying 'Hello', greet them back warmly and offer your menu of services.
                """)
            .user(input)
            .call()
            .content();
    }
}
