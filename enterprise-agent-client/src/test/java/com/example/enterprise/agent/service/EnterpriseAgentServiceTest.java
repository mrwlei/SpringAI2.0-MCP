package com.example.enterprise.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.enterprise.agent.model.AgentStreamEvent;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;

class EnterpriseAgentServiceTest {

    @Test
    void shouldReturnModelResponse() {
        var agentService = createAgentService(() -> "来自模型的回答");

        var result = agentService.askDetailed("查询交易订单");

        assertThat(result.answer()).isEqualTo("来自模型的回答");
        assertThat(result.model()).isEqualTo("test-model");
        assertThat(result.systemPrompt())
                .contains("企业智能查询助手")
                .contains("使用 Markdown 表格展示");
        assertThat(result.llmRounds()).singleElement().satisfies(round -> {
            assertThat(round.round()).isEqualTo(1);
            assertThat(round.toolCallRequested()).isFalse();
            assertThat(round.rawResult())
                    .contains("\"text\" : \"来自模型的回答\"")
                    .contains("\"toolCalls\" : [ ]")
                    .doesNotContain("\"results\"");
        });
        assertThat(result.toolInvocations()).isEmpty();
    }

    @Test
    void shouldPropagateModelFailure() {
        var agentService = createAgentService(() -> {
            throw new IllegalStateException("model unavailable");
        });

        assertThatThrownBy(() -> agentService.ask("查询交易订单"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("model unavailable");
    }

    @Test
    void shouldStreamMetadataContentAndDetailedResult() {
        var agentService = createStreamingAgentService("订单状态", "为已完成");

        var events = agentService.askDetailedStream("查询交易订单").collectList().block();

        assertThat(events)
                .extracting(AgentStreamEvent::type)
                .containsExactly(
                        AgentStreamEvent.Type.METADATA,
                        AgentStreamEvent.Type.DELTA,
                        AgentStreamEvent.Type.DELTA,
                        AgentStreamEvent.Type.DONE);
        assertThat(events.get(1).content()).isEqualTo("订单状态");
        assertThat(events.get(2).content()).isEqualTo("为已完成");
        assertThat(events.getLast().result()).satisfies(result -> {
            assertThat(result.answer()).isEqualTo("订单状态为已完成");
            assertThat(result.llmRounds()).hasSize(1);
            assertThat(result.toolInvocations()).isEmpty();
        });
    }

    @Test
    void shouldStreamFinalAnswerAndPreserveToolCallingTrace() {
        ChatModel chatModel = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                throw new UnsupportedOperationException("call is not used");
            }

            @Override
            public ChatOptions getOptions() {
                return ToolCallingChatOptions.builder().build();
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                var hasToolResult = prompt.getInstructions().stream()
                        .anyMatch(message -> message instanceof ToolResponseMessage);
                if (hasToolResult) {
                    return Flux.just(new ChatResponse(
                            List.of(new Generation(new AssistantMessage("订单状态为已完成")))));
                }

                var toolCallMessage = AssistantMessage.builder()
                        .content("")
                        .toolCalls(List.of(new AssistantMessage.ToolCall(
                                "call-1",
                                "function",
                                "get_trx_order_by_id",
                                "{\"id\":\"order-1\"}")))
                        .build();
                var metadata = ChatGenerationMetadata.builder()
                        .finishReason("TOOL_CALLS")
                        .build();
                return Flux.just(new ChatResponse(
                        List.of(new Generation(toolCallMessage, metadata))));
            }
        };
        var tools = new SyncMcpToolCallbackProvider(List.of()) {
            @Override
            public ToolCallback[] getToolCallbacks() {
                return new ToolCallback[] {new StubOrderTool()};
            }
        };
        var agentService = new EnterpriseAgentService(
                chatModel, tools, new ObjectMapper(), "test-model");

        var events = agentService.askDetailedStream("查询订单").collectList().block();
        var result = events.getLast().result();

        assertThat(result.llmRounds()).hasSize(2);
        assertThat(result.toolInvocations()).singleElement().satisfies(invocation -> {
            assertThat(invocation.round()).isEqualTo(1);
            assertThat(invocation.toolName()).isEqualTo("get_trx_order_by_id");
            assertThat(invocation.result()).isEqualTo("{\"status\":\"PAID\"}");
        });
        assertThat(events)
                .filteredOn(event -> event.type() == AgentStreamEvent.Type.DELTA)
                .extracting(AgentStreamEvent::content)
                .containsExactly("订单状态为已完成");
    }

    @Test
    void shouldKeepConcurrentStreamingTracesIsolated() {
        ChatModel chatModel = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                throw new UnsupportedOperationException("call is not used");
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                return Flux.just(prompt.getUserMessage().getText())
                        .delayElements(Duration.ofMillis(5))
                        .map(content -> new ChatResponse(
                                List.of(new Generation(new AssistantMessage(content)))));
            }
        };
        var agentService = new EnterpriseAgentService(
                chatModel,
                new SyncMcpToolCallbackProvider(List.of()),
                new ObjectMapper(),
                "test-model");

        var results = Flux.merge(
                        agentService
                                .askDetailedStream("订单-A")
                                .filter(event -> event.type() == AgentStreamEvent.Type.DONE)
                                .map(AgentStreamEvent::result),
                        agentService
                                .askDetailedStream("订单-B")
                                .filter(event -> event.type() == AgentStreamEvent.Type.DONE)
                                .map(AgentStreamEvent::result))
                .collectList()
                .block();

        assertThat(results)
                .extracting(result -> result.answer())
                .containsExactlyInAnyOrder("订单-A", "订单-B");
        results.forEach(result -> {
            assertThat(result.llmRounds()).hasSize(1);
            assertThat(result.llmRounds().getFirst().rawResult())
                    .contains(result.answer())
                    .doesNotContain(result.answer().equals("订单-A") ? "订单-B" : "订单-A");
        });
    }

    private EnterpriseAgentService createAgentService(Supplier<String> modelResponse) {
        ChatModel chatModel = prompt -> {
            var answer = modelResponse.get();
            return new ChatResponse(List.of(new Generation(new AssistantMessage(answer))));
        };
        return new EnterpriseAgentService(
                chatModel,
                new SyncMcpToolCallbackProvider(List.of()),
                new ObjectMapper(),
                "test-model");
    }

    private EnterpriseAgentService createStreamingAgentService(String... chunks) {
        ChatModel chatModel = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                throw new UnsupportedOperationException("call is not used");
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                return Flux.fromArray(chunks)
                        .map(content -> new ChatResponse(
                                List.of(new Generation(new AssistantMessage(content)))));
            }
        };
        return new EnterpriseAgentService(
                chatModel,
                new SyncMcpToolCallbackProvider(List.of()),
                new ObjectMapper(),
                "test-model");
    }

    private static final class StubOrderTool implements ToolCallback {
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
