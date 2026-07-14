package com.example.enterprise.agent.service;

import com.example.enterprise.agent.model.AgentQueryResult;
import com.example.enterprise.agent.model.AgentStreamEvent;
import com.example.enterprise.agent.observation.AgentInteractionRecorder;
import com.example.enterprise.agent.observation.RecordingChatModel;
import com.example.enterprise.agent.observation.RecordingToolCallback;
import java.util.Arrays;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;

@Service
public class EnterpriseAgentService {
    private static final Logger log = LoggerFactory.getLogger(EnterpriseAgentService.class);
    private static final Logger modelContentLog =
            LoggerFactory.getLogger("com.example.enterprise.agent.llm.content");
    private static final int MAX_LOG_CONTENT_LENGTH = 2_000;

    private static final String SYSTEM_PROMPT = """
            你是企业智能查询助手。你必须优先使用 MCP 工具获取实时企业数据，不得编造员工、组织或交易订单信息。
            只回答用户明确询问的内容；不要泄露无关个人信息。工具无结果时如实说明。
            使用简洁中文回答，并在可能时说明数据来自企业查询工具。工具返回结构化查询数据时，使用 Markdown 表格展示。
            """.strip();

    private final ChatClient chatClient;
    private final AgentInteractionRecorder interactionRecorder;
    private final String modelName;

    public EnterpriseAgentService(
            ChatModel chatModel,
            SyncMcpToolCallbackProvider tools,
            ObjectMapper objectMapper,
            @Value("${spring.ai.openai.chat.model:unknown}") String modelName) {
        this.interactionRecorder = new AgentInteractionRecorder();
        this.modelName = modelName;
        var recordingModel = new RecordingChatModel(chatModel, interactionRecorder, objectMapper);
        var recordingTools = Arrays.stream(tools.getToolCallbacks())
                .map(tool -> new RecordingToolCallback(tool, interactionRecorder))
                .toArray(RecordingToolCallback[]::new);
        this.chatClient = ChatClient.builder(recordingModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultToolCallbacks(recordingTools)
                .build();
    }

    public String ask(String question) {
        return askDetailed(question).answer();
    }

    public AgentQueryResult askDetailed(String question) {
        var startedAt = System.nanoTime();
        log.info(
                "layer=agent_service event=model_request_start provider=openaiCompatible questionLength={}",
                contentLength(question));
        logModelContent("request", question);
        interactionRecorder.start();
        try {
            var answer = chatClient.prompt().user(question).call().content();
            var interaction = interactionRecorder.finish();
            log.info(
                    "layer=agent_service event=model_request_success provider=openaiCompatible answerLength={} durationMs={}",
                    contentLength(answer),
                    elapsedMillis(startedAt));
            logModelContent("response", answer);
            return new AgentQueryResult(
                    answer,
                    SYSTEM_PROMPT,
                    modelName,
                    interaction.llmRounds(),
                    interaction.toolInvocations());
        } catch (RuntimeException exception) {
            interactionRecorder.discard();
            log.error(
                    "layer=agent_service event=model_request_failure provider=openaiCompatible errorType={} durationMs={}",
                    exception.getClass().getSimpleName(),
                    elapsedMillis(startedAt));
            throw exception;
        }
    }

    public Flux<AgentStreamEvent> askDetailedStream(String question) {
        return Flux.defer(() -> {
            var startedAt = System.nanoTime();
            var session = interactionRecorder.openSession();
            var answer = new StringBuilder();
            log.info(
                    "layer=agent_service event=model_stream_start provider=openaiCompatible questionLength={}",
                    contentLength(question));
            logModelContent("request", question);

            var contentEvents = chatClient
                    .prompt()
                    .user(question)
                    .toolContext(Map.of(
                            AgentInteractionRecorder.TOOL_CONTEXT_SESSION_KEY, session))
                    .stream()
                    .content()
                    .map(content -> {
                        answer.append(content);
                        return AgentStreamEvent.delta(content);
                    });

            var doneEvent = Flux.defer(() -> {
                var snapshot = session.snapshot();
                var result = new AgentQueryResult(
                        answer.toString(),
                        SYSTEM_PROMPT,
                        modelName,
                        snapshot.llmRounds(),
                        snapshot.toolInvocations());
                log.info(
                        "layer=agent_service event=model_stream_success provider=openaiCompatible answerLength={} durationMs={}",
                        contentLength(result.answer()),
                        elapsedMillis(startedAt));
                logModelContent("response", result.answer());
                return Flux.just(AgentStreamEvent.done(result));
            });

            return Flux.just(AgentStreamEvent.metadata(SYSTEM_PROMPT, modelName))
                    .concatWith(contentEvents)
                    .concatWith(doneEvent)
                    .doOnError(exception -> log.error(
                            "layer=agent_service event=model_stream_failure provider=openaiCompatible errorType={} durationMs={}",
                            exception.getClass().getSimpleName(),
                            elapsedMillis(startedAt)))
                    .doOnCancel(() -> log.info(
                            "layer=agent_service event=model_stream_cancelled provider=openaiCompatible answerLength={} durationMs={}",
                            answer.length(),
                            elapsedMillis(startedAt)))
                    .contextWrite(context -> context.put(
                            AgentInteractionRecorder.TOOL_CONTEXT_SESSION_KEY, session));
        });
    }

    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String modelName() {
        return modelName;
    }

    private void logModelContent(String direction, String content) {
        if (modelContentLog.isDebugEnabled()) {
            modelContentLog.debug(
                    "layer=model_content direction={} content={}",
                    direction,
                    normalizeForLog(content));
        }
    }

    private String normalizeForLog(String content) {
        if (content == null) {
            return "";
        }
        var normalized = content.replace('\r', ' ').replace('\n', ' ');
        if (normalized.length() <= MAX_LOG_CONTENT_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_LOG_CONTENT_LENGTH) + "...[truncated]";
    }

    private int contentLength(String content) {
        return content == null ? 0 : content.length();
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
