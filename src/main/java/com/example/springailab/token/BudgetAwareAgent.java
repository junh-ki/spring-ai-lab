package com.example.springailab.token;

import com.example.springailab.llm.ModelCatalog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BudgetAwareAgent {

    private final ChatClient chatClient;
    private final TokenBudgetService tokenBudgetService;

    public BudgetAwareAgent(final ChatClient.Builder chatClientBuilder,
                            final TokenBudgetService tokenBudgetService) {
        this.tokenBudgetService = tokenBudgetService;
        this.chatClient =  chatClientBuilder
            .defaultAdvisors(new TokenTrackingAdvisor(this.tokenBudgetService)) // Attach the tracker globally to this client
            .build();
    }

    public String generate(final String prompt) {
        final String selectedModel;
        if (this.tokenBudgetService.isBudgetExceeded()) {
            log.info("Budget Exceeded! Downgrading to Economy Model.");
            selectedModel = ModelCatalog.GPT_4O_MINI; // Cheap, fast
        } else {
            log.info("Budget Healthy. Using Premium Model.");
            selectedModel = ModelCatalog.GPT_4_TURBO; // Expensive, smart
        }
        return this.chatClient.prompt()
            .user(prompt)
            .options(
                ChatOptions.builder()
                    .model(selectedModel)
            )
            .call()
            .content();
    }

    /*
    public String generateWithFailover(final String prompt) {
        try {
            return callOpenAI(prompt); // Try the primary provider (e.g., OpenAI)
        } catch (final OpenAiHttpException openAiHttpException) {
            if (openAiHttpException.statusCode() != 429) { // 429: Too many requests
                throw openAiHttpException // Rethrow other errors
            }
            // Rate Limit Hit -> Pivot to backup (e.g., Anthropic or Local)
            log.error("OpenAI Rate Limited. Switching to Bedrock.");
            return callBedrockBackup(prompt);
        }
    }
     */
}
