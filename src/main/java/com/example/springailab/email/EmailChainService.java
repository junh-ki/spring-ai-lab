package com.example.springailab.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailChainService {

    private static final String ANALYST_PROMPT = """
        Analyze the following customer email.
        Identify the customer's sentiment (ANGRY, NEUTRAL, HAPPY)
        and extract a bulleted list of their specific complaints.
        Do not propose a solution yet. Just list the facts.
        
        EMAIL:
        {raw_email}
        """;
    private static final String WRITER_PROMPT = """
        You are a senior support agent.
        Draft a polite, concise response based ONLY on the following analysis.
        Apologize for the specific issues listed.
        
        ANALYSIS:
        {analysis_output}
        """;
    private final ChatClient chatClient;

    public String generateResponse(final String userEmail) {
        // --- LINK 1: The Analysis Step ---
        log.info("Step 1: Analyzing email...");
        final String analysis = this.chatClient.prompt()
            .user(promptUserSpec ->
                promptUserSpec
                    .text(ANALYST_PROMPT)
                    .param("raw_email", userEmail)
            )
            .call()
            .content();
        log.info("--- ANALYSIS RESULT ---");
        log.info(analysis);
        log.info("-----------------------");
        // --- LINK 2: The Drafting Step ---
        log.info("Step 2: Drafting response...");
        return this.chatClient.prompt()
            .user(promptUserSpec ->
                promptUserSpec
                    .text(WRITER_PROMPT)
                    .param(
                        "analysis_output",
                        StringUtils.isNotBlank(analysis)
                            ? analysis
                            : StringUtils.EMPTY
                    )
            )
            .call()
            .content();
    }
}
