package com.example.enterprise.agent.observation;

import java.util.function.Supplier;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

public class RecordingToolCallback implements ToolCallback {
    private final ToolCallback delegate;
    private final AgentInteractionRecorder recorder;

    public RecordingToolCallback(ToolCallback delegate, AgentInteractionRecorder recorder) {
        this.delegate = delegate;
        this.recorder = recorder;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        return record(null, toolInput, () -> delegate.call(toolInput));
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return record(
                recorder.sessionFrom(toolContext),
                toolInput,
                () -> delegate.call(toolInput, toolContext));
    }

    private String record(
            AgentInteractionRecorder.InteractionSession session,
            String toolInput,
            Supplier<String> invocation) {
        var startedAt = System.nanoTime();
        try {
            var result = invocation.get();
            recordInvocation(session, toolInput, result, true, elapsedMillis(startedAt));
            return result;
        } catch (RuntimeException exception) {
            recordInvocation(
                    session,
                    toolInput,
                    "ERROR: " + exception.getClass().getSimpleName(),
                    false,
                    elapsedMillis(startedAt));
            throw exception;
        }
    }

    private void recordInvocation(
            AgentInteractionRecorder.InteractionSession session,
            String toolInput,
            String result,
            boolean success,
            long durationMs) {
        if (session == null) {
            recorder.recordToolInvocation(
                    getToolDefinition().name(), toolInput, result, success, durationMs);
            return;
        }
        recorder.recordToolInvocation(
                session, getToolDefinition().name(), toolInput, result, success, durationMs);
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
