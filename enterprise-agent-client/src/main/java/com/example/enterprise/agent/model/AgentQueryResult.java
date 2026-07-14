package com.example.enterprise.agent.model;

import java.util.List;

public record AgentQueryResult(
        String answer,
        String systemPrompt,
        String model,
        List<LlmRound> llmRounds,
        List<ToolInvocation> toolInvocations) {

    public record LlmRound(int round, boolean toolCallRequested, String rawResult) {}

    public record ToolInvocation(
            int sequence,
            int round,
            String toolName,
            String arguments,
            String result,
            boolean success,
            long durationMs) {}
}
