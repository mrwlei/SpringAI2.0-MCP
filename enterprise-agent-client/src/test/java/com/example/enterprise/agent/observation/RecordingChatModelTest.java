package com.example.enterprise.agent.observation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;

class RecordingChatModelTest {

    @Test
    void shouldRecordCompactModelResponse() {
        var output = AssistantMessage.builder()
                .content("")
                .properties(Map.of("reasoningContent", "需要根据主键查询订单"))
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "call-1",
                        "function",
                        "get_trx_order_by_id",
                        "{\"id\":\"order-1\"}")))
                .build();
        var generationMetadata = ChatGenerationMetadata.builder().finishReason("TOOL_CALLS").build();
        var responseMetadata = ChatResponseMetadata.builder()
                .usage(new DefaultUsage(800, 86, 886, null, 768L, null))
                .build();
        ChatModel delegate = prompt -> new ChatResponse(
                List.of(new Generation(output, generationMetadata)), responseMetadata);
        var recorder = new AgentInteractionRecorder();
        recorder.start();

        new RecordingChatModel(delegate, recorder, new ObjectMapper()).call(new Prompt("查询订单"));

        var rawResult = recorder.finish().llmRounds().getFirst().rawResult();
        assertThat(rawResult)
                .contains("\"text\" : \"\"")
                .contains("\"reasoningContent\" : \"需要根据主键查询订单\"")
                .contains("\"name\" : \"get_trx_order_by_id\"")
                .contains("\"finishReason\" : \"TOOL_CALLS\"")
                .contains("\"promptTokens\" : 800")
                .contains("\"cacheReadInputTokens\" : 768")
                .doesNotContain("\"metadata\"")
                .doesNotContain("\"result\"")
                .doesNotContain("\"results\"")
                .doesNotContain("\"nativeUsage\"");
    }

    @Test
    void shouldAggregateStreamingChunksIntoOneModelRound() {
        ChatModel delegate = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                throw new UnsupportedOperationException("call is not used");
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                return Flux.just(response("订单状态"), response("为已完成"));
            }
        };
        var recorder = new AgentInteractionRecorder();
        var session = recorder.openSession();
        var options = ToolCallingChatOptions.builder()
                .toolContext(AgentInteractionRecorder.TOOL_CONTEXT_SESSION_KEY, session)
                .build();

        var chunks = new RecordingChatModel(delegate, recorder, new ObjectMapper())
                .stream(new Prompt("查询订单", options))
                .collectList()
                .block();

        assertThat(chunks).hasSize(2);
        assertThat(session.snapshot().llmRounds()).singleElement().satisfies(round -> {
            assertThat(round.round()).isEqualTo(1);
            assertThat(round.toolCallRequested()).isFalse();
            assertThat(round.rawResult()).contains("\"text\" : \"订单状态为已完成\"");
        });
    }

    private ChatResponse response(String content) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }
}
