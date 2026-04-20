package com.example.springailab.marketing;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContentGuardrailService {

	private static final String GUARD_PROMPT = """
		Analyze the following request.
		Does it ask about our competitor, 'MegaCorp'?
		
		If YES, return exactly: BLOCKED
		If NO, return exactly: ALLOWED
		
		REQUEST: {input}
		""";
	private final ChatClient chatClient;

	public boolean isSafe(final String input) {
		return "ALLOWED".equalsIgnoreCase(
			Optional.ofNullable(
				this.chatClient.prompt()
					.user(promptUserSpec ->
						promptUserSpec
							.text(GUARD_PROMPT)
							.param("input", input)
					)
					.call()
					.content()
				).orElse(StringUtils.EMPTY).trim()
		);
	}
}
