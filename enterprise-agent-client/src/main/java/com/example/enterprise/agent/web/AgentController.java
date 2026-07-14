package com.example.enterprise.agent.web;

import com.example.enterprise.agent.model.AgentQueryResult.LlmRound;
import com.example.enterprise.agent.model.AgentQueryResult.ToolInvocation;
import com.example.enterprise.agent.model.AgentStreamEvent;
import com.example.enterprise.agent.service.EnterpriseAgentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {
    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final EnterpriseAgentService agentService;

    public AgentController(EnterpriseAgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping("/config")
    public AgentConfigResponse config() {
        return new AgentConfigResponse(agentService.systemPrompt(), agentService.modelName());
    }

    @PostMapping(value = "/query", consumes = MediaType.APPLICATION_JSON_VALUE)
    public QueryResponse query(@Valid @RequestBody QueryRequest request) {
        var startedAt = System.nanoTime();
        log.info(
                "layer=agent_controller event=query_start questionLength={}",
                request.question().length());
        try {
            var result = agentService.askDetailed(request.question());
            log.info(
                    "layer=agent_controller event=query_success answerLength={} durationMs={}",
                    contentLength(result.answer()),
                    elapsedMillis(startedAt));
            return new QueryResponse(
                    result.answer(),
                    result.systemPrompt(),
                    result.model(),
                    result.llmRounds(),
                    result.toolInvocations());
        } catch (RuntimeException exception) {
            log.error(
                    "layer=agent_controller event=query_failure errorType={} durationMs={}",
                    exception.getClass().getSimpleName(),
                    elapsedMillis(startedAt));
            throw exception;
        }
    }

    @PostMapping(
            value = "/query/stream",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AgentStreamEvent>> queryStream(
            @Valid @RequestBody QueryRequest request) {
        var startedAt = System.nanoTime();
        log.info(
                "layer=agent_controller event=query_stream_start questionLength={}",
                request.question().length());

        return agentService
                .askDetailedStream(request.question())
                .map(this::toServerSentEvent)
                .doOnComplete(() -> log.info(
                        "layer=agent_controller event=query_stream_success durationMs={}",
                        elapsedMillis(startedAt)))
                .doOnError(exception -> log.error(
                        "layer=agent_controller event=query_stream_failure errorType={} durationMs={}",
                        exception.getClass().getSimpleName(),
                        elapsedMillis(startedAt)))
                .onErrorResume(exception -> Flux.just(toServerSentEvent(
                        AgentStreamEvent.error("查询失败，请稍后重试"))));
    }

    private ServerSentEvent<AgentStreamEvent> toServerSentEvent(AgentStreamEvent event) {
        return ServerSentEvent.<AgentStreamEvent>builder()
                .event(event.type().eventName())
                .data(event)
                .build();
    }

    private int contentLength(String content) {
        return content == null ? 0 : content.length();
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    public record QueryRequest(@NotBlank String question) {}

    public record QueryResponse(
            String answer,
            String systemPrompt,
            String model,
            List<LlmRound> llmRounds,
            List<ToolInvocation> toolInvocations) {}

    public record AgentConfigResponse(String systemPrompt, String model) {}
}
