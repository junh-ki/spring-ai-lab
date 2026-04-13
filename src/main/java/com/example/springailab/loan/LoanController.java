package com.example.springailab.loan;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LoanController {

    private final LoanProcessor loanProcessor;

    @PostMapping("/assess")
    public LoanContext assess(@RequestBody final String applicationText) {
        return this.loanProcessor.processApplication(
            UUID.randomUUID().toString(),
            applicationText
        );
    }
}
