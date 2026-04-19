package com.example.springailab.marketing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketingService {

	private final ContentGuardrailService contentGuardrailService;
	private final ChatClient chatClient;

	public String generateBlogPost(final String topic) {
		// --- Link 1: The Circuit Breaker ---
		log.info("Validating topic: {}", topic);
		if (!this.contentGuardrailService.isSafe(topic)) {
			log.info("Guardrail triggered. Halting.");
			return "I'm sorry, I cannot generate content related to specific competitors.";
		}
		// --- Link 2: The Generator (Only runs if safe) ---
		log.info("Topic approved. Generating content...");
		return this.chatClient.prompt()
			.user(promptUserSpec ->
				promptUserSpec
					.text("Write a blog post about: {topic}")
					.param("topic", topic)
			)
			.call()
			.content();
	}
}
