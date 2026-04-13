package com.example.springailab.loan.component;

import com.example.springailab.loan.LoanContext;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Step 1: The Extractor
 * This step uses the LLM to convert the messy application text into structured numbers.
 */
@Order(1)
@Component
@RequiredArgsConstructor
public class IncomeExtractorStep implements ChainStep {

    private static final String ANNUAL_INCOME_EXTRACTION_PROMPT = """
        Extract the annual income from the text below.
        Return ONLY the number, no symbols or text.
        If unclear, return 0.
        
        TEXT:
        {text}
        """;
    private final ChatClient chatClient;

    @Override
    public void execute(final LoanContext loanContext) {
        final String result = Optional.ofNullable(
            this.chatClient.prompt()
                .user(promptUserSpec ->
                    promptUserSpec
                        .text(ANNUAL_INCOME_EXTRACTION_PROMPT)
                        .param("text", loanContext.getRawApplicationText())
                )
                .call()
                .content()
        ).orElse(StringUtils.EMPTY);
        try {
            loanContext.setExtractedIncome(
                Double.parseDouble(result.trim())
            );
        } catch (final NumberFormatException numberFormatException) {
            loanContext.setExtractedIncome(0.0);
            loanContext.log("Failed to parse income from AI response.");
        }
    }
}
