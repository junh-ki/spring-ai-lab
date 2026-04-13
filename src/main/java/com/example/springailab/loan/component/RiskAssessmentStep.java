package com.example.springailab.loan.component;

import com.example.springailab.loan.LoanContext;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Step 3: The Decision Maker
 * This final step looks at the state built by the previous two steps to make a judgment.
 * It doesn't need to see the raw text anymore, it trusts the income and CreditScore in the loan context.
 */
@Order(3)
@Component
@RequiredArgsConstructor
public class RiskAssessmentStep implements ChainStep {

    private static final String RISK_ASSESSMENT_PROMPT = """
        Act as a risk officer. Determine the risk level
        (LOW, MEDIUM, HIGH) based on this data:
        
        Income: {income}
        Credit Score: {score}
        
        Return ONLY the risk level string.
        """;
    private final ChatClient chatClient;

    @Override
    public void execute(final LoanContext loanContext) {
        loanContext.setRiskLevel(
            this.chatClient.prompt()
                .user(promptUserSpec ->
                    promptUserSpec
                        .text(RISK_ASSESSMENT_PROMPT)
                        .param("income", loanContext.getExtractedIncome())
                        .param("score", loanContext.getCreditScore())
                )
                .call()
                .content()
        );
    }
}
