package com.example.springailab.loan.component;

import com.example.springailab.loan.LoanContext;

public interface ChainStep {

    void execute(final LoanContext loanContext);
}
