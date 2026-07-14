package com.example.enterprise.agent.stdio.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.stereotype.Service;

@Service
public class EnterpriseStdioAgentService {
    private static final String SYSTEM_PROMPT = """
            你是企业智能查询助手。你必须优先使用 MCP 工具获取实时企业数据，不得编造员工或组织信息。
            只回答用户明确询问的内容；不要泄露无关个人信息。工具无结果时如实说明。
            使用简洁中文回答，并在可能时说明数据来自企业查询工具。
            """;

    private final ChatClient chatClient;

    public EnterpriseStdioAgentService(ChatClient.Builder builder, SyncMcpToolCallbackProvider tools) {
        this.chatClient = builder.defaultSystem(SYSTEM_PROMPT).defaultToolCallbacks(tools.getToolCallbacks()).build();
    }

    public String ask(String question) {
        return chatClient.prompt().user(question).call().content();
    }
}
