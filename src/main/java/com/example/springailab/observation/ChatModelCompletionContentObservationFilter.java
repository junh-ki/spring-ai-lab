package com.example.springailab.observation;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;
import java.util.List;
import java.util.Objects;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NullMarked;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.content.Content;
import org.springframework.ai.observation.ObservabilityHelper;
import org.springframework.stereotype.Component;

/**
 * Attaches the rendered prompt and the model completion to the chat-model OTel
 * span so Langfuse can populate the Generation observation's Input/Output and
 * the trace-level Input/Output columns.
 *
 * <p>Spring AI 2.0.0-M5 ships {@code ChatModelPromptContentObservationHandler}
 * and {@code ChatModelCompletionObservationHandler}, but those only emit
 * {@code logger.info(...)} — they do not mutate the {@link Observation.Context},
 * so nothing reaches the OTel exporter. The
 * {@code spring.ai.chat.observations.log-prompt} / {@code log-completion}
 * properties only toggle that SLF4J logging. To make the content visible in
 * Langfuse we have to enrich the span explicitly, which is what this filter
 * does, following Langfuse's Spring AI integration guide
 * (<a href="https://langfuse.com/integrations/frameworks/spring-ai">...</a>).
 *
 * <p>{@link ObservationFilter} is invoked just before each observation is
 * stopped, so the response is already on the context. We add the content as
 * high-cardinality KeyValues, which Micrometer's tracing bridge exports as
 * span attributes. Langfuse maps:
 * <ul>
 *   <li>{@code gen_ai.prompt} / {@code gen_ai.completion} → Generation
 *       observation Input/Output</li>
 *   <li>{@code langfuse.trace.input} / {@code langfuse.trace.output} →
 *       trace-level Input/Output columns (not auto-derived from the
 *       Generation)</li>
 * </ul>
 */
@NullMarked
@Component
public class ChatModelCompletionContentObservationFilter implements ObservationFilter {

    @Override
    public Observation.Context map(final Observation.Context observationContext) {
        if (!(observationContext instanceof ChatModelObservationContext chatModelObservationContext)) {
            return observationContext;
        }
        final String prompt = ObservabilityHelper.concatenateStrings(processPrompts(chatModelObservationContext));
        final String completion = ObservabilityHelper.concatenateStrings(processCompletion(chatModelObservationContext));
        chatModelObservationContext.addHighCardinalityKeyValue(KeyValue.of("gen_ai.prompt", prompt));
        chatModelObservationContext.addHighCardinalityKeyValue(KeyValue.of("gen_ai.completion", completion));
        chatModelObservationContext.addHighCardinalityKeyValue(KeyValue.of("langfuse.trace.input", prompt));
        chatModelObservationContext.addHighCardinalityKeyValue(KeyValue.of("langfuse.trace.output", completion));
        return chatModelObservationContext;
    }

    private List<String> processPrompts(final ChatModelObservationContext chatModelObservationContext) {
        if (CollectionUtils.isEmpty(chatModelObservationContext.getRequest().getInstructions())) {
            return List.of();
        }
        return chatModelObservationContext.getRequest().getInstructions().stream()
            .map(Content::getText)
            .filter(Objects::nonNull)
            .toList();
    }

    private List<String> processCompletion(final ChatModelObservationContext chatModelObservationContext) {
        final ChatResponse chatResponse = chatModelObservationContext.getResponse();
        if (chatResponse == null ||
            CollectionUtils.isEmpty(chatResponse.getResults())) {
            return List.of();
        }
        return chatResponse.getResults().stream()
            .filter(generation -> StringUtils.isNotBlank(generation.getOutput().getText()))
            .map(generation -> generation.getOutput().getText())
            .filter(Objects::nonNull)
            .toList();
    }
}
