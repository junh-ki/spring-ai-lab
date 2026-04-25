package com.example.springailab.routing;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@NoArgsConstructor
public class TechSupportService implements CustomerSupportService {

    public String handle(final String message) {
        log.info("Received message: {}", message);
        return "Handled tech support!";
    }
}
