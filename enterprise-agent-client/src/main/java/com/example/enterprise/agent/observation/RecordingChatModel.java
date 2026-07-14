package com.example.enterprise.agent.observation;

import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.MessageAggregator;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;

public class RecordingChatModel implements ChatModel {
    private static final Logger log = LoggerFactory.getLogger(RecordingChatModel.class);
    private static final String REASONING_CONTENT = "reasoningContent";
    private static final String SERIALIZATION_FAILURE = """
            {
              "text": "",
              "reasoningContent": "",
              "toolCalls": [],
              "finishReason": "SERIALIZATION_ERROR",
              "usage": {
                "promptTokens": 0,
                "completionTokens": 0,
                "totalTokens": 0,
                "cacheReadInputTokens": null
              }
            }
            """.strip();

    private final ChatModel delegate;
    private final AgentInteractionRecorder recorder;
    private final ObjectMapper objectMapper;

    public RecordingChatModel(ChatModel delegate, AgentInteractionRecorder recorder, ObjectMapper objectMapper) {
        this.delegate = delegate;
        this.recorder = recorder;
        this.objectMapper = objectMapper;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        var response = delegate.call(prompt);
        record(response);
        return response;
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        var promptSession = sessionFrom(prompt);
        return Flux.deferContextual(contextView -> {
            var contextSession = contextView.getOrDefault(
                    AgentInteractionRecorder.TOOL_CONTEXT_SESSION_KEY,
                    (AgentInteractionRecorder.InteractionSession) null);
            var session = promptSession == null ? contextSession : promptSession;
            return new MessageAggregator()
                    .aggregate(delegate.stream(prompt), response -> record(session, response));
        });
    }

    @Override
    public ChatOptions getOptions() {
        return delegate.getOptions();
    }

    private void record(ChatResponse response) {
        recorder.recordLlmRound(response.hasToolCalls(), serialize(response));
    }

    private void record(
            AgentInteractionRecorder.InteractionSession session, ChatResponse response) {
        if (session == null) {
            record(response);
            return;
        }
        recorder.recordLlmRound(session, response.hasToolCalls(), serialize(response));
    }

    private AgentInteractionRecorder.InteractionSession sessionFrom(Prompt prompt) {
        if (prompt.getOptions() instanceof ToolCallingChatOptions options) {
            return recorder.sessionFrom(options.getToolContext());
        }
        return null;
    }

    private String serialize(ChatResponse response) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(compact(response));
        } catch (RuntimeException exception) {
            log.warn(
                    "layer=recording_chat_model event=response_serialization_failure errorType={}",
                    exception.getClass().getSimpleName());
            return SERIALIZATION_FAILURE;
        }
    }

    private CompactChatResponse compact(ChatResponse response) {
        var generation = response.getResult();
        var usage = response.getMetadata().getUsage();
        if (generation == null) {
            return new CompactChatResponse("", "", List.of(), "", compactUsage(usage));
        }

        var output = generation.getOutput();
        var toolCalls = output.getToolCalls().stream().map(CompactToolCall::from).toList();
        return new CompactChatResponse(
                Objects.requireNonNullElse(output.getText(), ""),
                reasoningContent(output),
                toolCalls,
                Objects.requireNonNullElse(generation.getMetadata().getFinishReason(), ""),
                compactUsage(usage));
    }

    private String reasoningContent(AssistantMessage output) {
        var value = output.getMetadata().get(REASONING_CONTENT);
        return value instanceof String text ? text : "";
    }

    private CompactUsage compactUsage(Usage usage) {
        return new CompactUsage(
                usage.getPromptTokens(),
                usage.getCompletionTokens(),
                usage.getTotalTokens(),
                usage.getCacheReadInputTokens());
    }

    private record CompactChatResponse(
            String text,
            String reasoningContent,
            List<CompactToolCall> toolCalls,
            String finishReason,
            CompactUsage usage) {}

    private record CompactToolCall(String id, String type, String name, String arguments) {
        private static CompactToolCall from(AssistantMessage.ToolCall toolCall) {
            return new CompactToolCall(toolCall.id(), toolCall.type(), toolCall.name(), toolCall.arguments());
        }
    }

    private record CompactUsage(
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens,
            Long cacheReadInputTokens) {}
}
