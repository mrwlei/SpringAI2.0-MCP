package com.example.enterprise.agent.observation;

import com.example.enterprise.agent.model.AgentQueryResult.LlmRound;
import com.example.enterprise.agent.model.AgentQueryResult.ToolInvocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.model.ToolContext;

public class AgentInteractionRecorder {
    public static final String TOOL_CONTEXT_SESSION_KEY =
            AgentInteractionRecorder.class.getName() + ".session";

    private final ThreadLocal<InteractionSession> current = new ThreadLocal<>();

    public void start() {
        if (current.get() != null) {
            throw new IllegalStateException("Agent interaction is already being recorded");
        }
        current.set(openSession());
    }

    public InteractionSession openSession() {
        return new InteractionSession();
    }

    public void recordLlmRound(boolean toolCallRequested, String rawResult) {
        recordLlmRound(current.get(), toolCallRequested, rawResult);
    }

    public void recordLlmRound(
            InteractionSession session, boolean toolCallRequested, String rawResult) {
        if (session != null) {
            session.recordLlmRound(toolCallRequested, rawResult);
        }
    }

    public void recordToolInvocation(
            String toolName, String arguments, String result, boolean success, long durationMs) {
        recordToolInvocation(
                current.get(), toolName, arguments, result, success, durationMs);
    }

    public void recordToolInvocation(
            InteractionSession session,
            String toolName,
            String arguments,
            String result,
            boolean success,
            long durationMs) {
        if (session != null) {
            session.recordToolInvocation(
                    toolName, arguments, result, success, durationMs);
        }
    }

    public InteractionSnapshot finish() {
        var session = current.get();
        current.remove();
        return session == null ? InteractionSnapshot.empty() : session.snapshot();
    }

    public void discard() {
        current.remove();
    }

    public InteractionSession sessionFrom(Map<String, Object> context) {
        if (context == null) {
            return null;
        }
        var value = context.get(TOOL_CONTEXT_SESSION_KEY);
        return value instanceof InteractionSession session ? session : null;
    }

    public InteractionSession sessionFrom(ToolContext toolContext) {
        return toolContext == null ? null : sessionFrom(toolContext.getContext());
    }

    public record InteractionSnapshot(List<LlmRound> llmRounds, List<ToolInvocation> toolInvocations) {
        private static InteractionSnapshot empty() {
            return new InteractionSnapshot(List.of(), List.of());
        }
    }

    public static final class InteractionSession {
        private final List<LlmRound> llmRounds = new ArrayList<>();
        private final List<ToolInvocation> toolInvocations = new ArrayList<>();

        private synchronized void recordLlmRound(boolean toolCallRequested, String rawResult) {
            llmRounds.add(new LlmRound(llmRounds.size() + 1, toolCallRequested, rawResult));
        }

        private synchronized void recordToolInvocation(
                String toolName,
                String arguments,
                String result,
                boolean success,
                long durationMs) {
            toolInvocations.add(new ToolInvocation(
                    toolInvocations.size() + 1,
                    llmRounds.size(),
                    toolName,
                    arguments,
                    result,
                    success,
                    durationMs));
        }

        public synchronized InteractionSnapshot snapshot() {
            return new InteractionSnapshot(List.copyOf(llmRounds), List.copyOf(toolInvocations));
        }
    }
}
