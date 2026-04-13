package com.example.springailab.loan;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class LoanContext {

    // Raw Inputs
    private final String applicationId;
    private final String rawApplicationText;
    private final List<String> auditLog = new ArrayList<>();
    // Derived State (Filled by Steps)
    private Double extractedIncome;
    private Integer creditScore;
    private String riskLevel;

    public void log(final String message) {
        this.auditLog.add(System.currentTimeMillis() + ": " + message);
    }
}
