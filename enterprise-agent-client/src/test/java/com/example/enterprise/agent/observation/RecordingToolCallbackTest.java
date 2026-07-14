package com.example.enterprise.agent.observation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

class RecordingToolCallbackTest {

    @Test
    void shouldRecordToolArgumentsAndResult() {
        var recorder = new AgentInteractionRecorder();
        recorder.start();
        recorder.recordLlmRound(true, "{\"toolCalls\":[{\"name\":\"get_trx_order_by_id\"}]}");
        var callback = new RecordingToolCallback(new StubToolCallback(), recorder);

        assertThat(callback.call("{\"id\":\"order-1\"}")).isEqualTo("{\"status\":\"PAID\"}");

        var snapshot = recorder.finish();
        assertThat(snapshot.toolInvocations()).singleElement().satisfies(invocation -> {
            assertThat(invocation.sequence()).isEqualTo(1);
            assertThat(invocation.round()).isEqualTo(1);
            assertThat(invocation.toolName()).isEqualTo("get_trx_order_by_id");
            assertThat(invocation.arguments()).isEqualTo("{\"id\":\"order-1\"}");
            assertThat(invocation.result()).isEqualTo("{\"status\":\"PAID\"}");
            assertThat(invocation.success()).isTrue();
        });
    }

    @Test
    void shouldRecordToolInvocationInExplicitStreamingSession() {
        var recorder = new AgentInteractionRecorder();
        var session = recorder.openSession();
        recorder.recordLlmRound(session, true, "{\"toolCalls\":[{\"name\":\"get_trx_order_by_id\"}]}");
        var callback = new RecordingToolCallback(new StubToolCallback(), recorder);
        var toolContext = new ToolContext(Map.of(
                AgentInteractionRecorder.TOOL_CONTEXT_SESSION_KEY, session));

        assertThat(callback.call("{\"id\":\"order-2\"}", toolContext))
                .isEqualTo("{\"status\":\"PAID\"}");

        assertThat(session.snapshot().toolInvocations()).singleElement().satisfies(invocation -> {
            assertThat(invocation.round()).isEqualTo(1);
            assertThat(invocation.arguments()).isEqualTo("{\"id\":\"order-2\"}");
        });
    }

    private static class StubToolCallback implements ToolCallback {

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder()
                    .name("get_trx_order_by_id")
                    .description("查询交易订单")
                    .inputSchema("{\"type\":\"object\"}")
                    .build();
        }

        @Override
        public String call(String toolInput) {
            return "{\"status\":\"PAID\"}";
        }
    }
}
