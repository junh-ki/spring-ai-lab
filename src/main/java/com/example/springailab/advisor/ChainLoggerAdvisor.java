package com.example.springailab.advisor;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NullMarked;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

@Slf4j
@NullMarked
@RequiredArgsConstructor
public class ChainLoggerAdvisor implements BaseAdvisor {

	private final String stepName;

	@Override
	public String getName() {
		return this.stepName;
	}

	@Override
	public int getOrder() {
		return 0; // To run first to capture everything
	}

	@Override
	public ChatClientRequest before(final ChatClientRequest chatClientRequest,
									final AdvisorChain advisorChain) {
		log.info(
			"[{}] >> PROMPT: {}",
			this.stepName,
			Optional.of(chatClientRequest.prompt().getUserMessage())
				.map(AbstractMessage::getText)
				.orElse(StringUtils.EMPTY)
		);
		log.debug(
			"[{}] >> PARAMS: {}",
			this.stepName,
			chatClientRequest.context()
		);
		return chatClientRequest;
	}

	@Override
	public ChatClientResponse after(final ChatClientResponse chatClientResponse,
									final AdvisorChain advisorChain) {
		log.info(
			"[{}] << RESPONSE: {}",
			this.stepName,
			Optional.ofNullable(chatClientResponse.chatResponse())
				.map(ChatResponse::getResult)
				.map(Generation::getOutput)
				.map(AbstractMessage::getText)
				.orElse(StringUtils.EMPTY)
		);
		return chatClientResponse;
	}
}
