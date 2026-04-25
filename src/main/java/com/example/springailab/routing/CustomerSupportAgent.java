package com.example.springailab.routing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerSupportAgent {

    private final IntentRouter intentRouter;
    private final BillingService billingService;
    private final TechSupportService techSupportService;
    private final SalesService salesService;

    public String handleRequest(final String message) {
        final RoutingTarget routingTarget = this.intentRouter.route(message);
        log.info("Routing to: {}", routingTarget.name());
        return switch (routingTarget) {
            case BILLING -> this.billingService.handle(message);
            case TECHNICAL_SUPPORT -> this.techSupportService.handle(message);
            case SALES -> this.salesService.handle(message);
            case GENERAL_CHAT -> "Hello! I am the company AI assistant. I can help with Billing, Support, or Sales.";
            case UNKNOWN -> "I'm not sure which department handles that. Could you rephrase?";
        };
    }
}
