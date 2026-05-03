package com.example.springailab.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AccountAgent implements AgentInterface {

    @Override
    public String process(final String input) {
        log.info("AccountAgent processing input...");
        return "result";
    }
}
