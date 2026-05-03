package com.example.springailab.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TransactionAgent implements AgentInterface {

    @Override
    public String process(final String input) {
        log.info("TransactionAgent processing input...");
        return "result";
    }
}
