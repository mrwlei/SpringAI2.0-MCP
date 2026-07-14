package com.example.enterprise.agent.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.enterprise.agent.service.EnterpriseAgentService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;

class AgentControllerTest {

    @Test
    void shouldExposeConfigAndDetailedQueryResponse() {
        ChatModel chatModel = prompt -> new ChatResponse(
                List.of(new Generation(new AssistantMessage("订单状态为已完成"))));
        var agentService = new EnterpriseAgentService(
                chatModel,
                new SyncMcpToolCallbackProvider(List.of()),
                new ObjectMapper(),
                "test-model");
        var controller = new AgentController(agentService);

        var config = controller.config();
        var response = controller.query(new AgentController.QueryRequest("查询订单状态"));

        assertThat(config.model()).isEqualTo("test-model");
        assertThat(config.systemPrompt()).contains("企业智能查询助手");
        assertThat(response.answer()).isEqualTo("订单状态为已完成");
        assertThat(response.systemPrompt()).isEqualTo(config.systemPrompt());
        assertThat(response.model()).isEqualTo(config.model());
        assertThat(response.llmRounds()).hasSize(1);
        assertThat(response.toolInvocations()).isEmpty();
    }

    @Test
    void shouldExposeServerSentEventsWithoutChangingBlockingEndpoint() throws Exception {
        ChatModel chatModel = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(
                        List.of(new Generation(new AssistantMessage("阻塞回答"))));
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                return Flux.just("流式", "回答")
                        .map(content -> new ChatResponse(
                                List.of(new Generation(new AssistantMessage(content)))));
            }
        };
        var agentService = new EnterpriseAgentService(
                chatModel,
                new SyncMcpToolCallbackProvider(List.of()),
                new ObjectMapper(),
                "test-model");
        var controller = new AgentController(agentService);

        var blockingResponse = controller.query(new AgentController.QueryRequest("阻塞查询"));
        var streamEvents = controller
                .queryStream(new AgentController.QueryRequest("流式查询"))
                .collectList()
                .block();

        assertThat(blockingResponse.answer()).isEqualTo("阻塞回答");
        assertThat(streamEvents)
                .extracting(event -> event.event())
                .containsExactly("metadata", "delta", "delta", "done");
        assertThat(streamEvents.getLast().data().result().answer()).isEqualTo("流式回答");

        var mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        var asyncResult = mockMvc
                .perform(post("/api/v1/agent/query/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"question\":\"流式查询\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event:metadata")))
                .andExpect(content().string(containsString("event:delta")))
                .andExpect(content().string(containsString("event:done")));
    }
}
