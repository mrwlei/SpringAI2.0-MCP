package com.example.enterprise.mcp.stdio;

import static org.assertj.core.api.Assertions.assertThat;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpSchema;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EnterpriseMcpStdioServerIT {

    @Test
    void shouldInitializeListAndCallToolsOverStdio() {
        var serverJar = Path.of(System.getProperty("stdio.server.jar")).toAbsolutePath();
        var javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        var serverParameters = ServerParameters.builder(javaExecutable)
                .args("-jar", serverJar.toString())
                .env(Map.of("ENTERPRISE_DB_PASSWORD", "integration-test-only"))
                .build();
        var transport = new StdioClientTransport(serverParameters, McpJsonDefaults.getMapper());

        try (var client = McpClient.sync(transport)
                .initializationTimeout(Duration.ofSeconds(10))
                .requestTimeout(Duration.ofSeconds(10))
                .build()) {
            var initialization = client.initialize();
            assertThat(initialization.serverInfo().name()).isEqualTo("enterprise-query-mcp");

            var tools = client.listTools().tools();
            assertThat(tools).extracting(McpSchema.Tool::name)
                    .containsExactlyInAnyOrder(
                            "search_employees",
                            "get_department",
                            "get_trx_order_by_id",
                            "get_latest_trx_order_by_payee_id_card_no");
            assertThat(tools).allSatisfy(tool -> {
                assertThat(tool.annotations().readOnlyHint()).isTrue();
                assertThat(tool.annotations().destructiveHint()).isFalse();
            });

            var request = McpSchema.CallToolRequest.builder("search_employees")
                    .arguments(Map.of("keyword", "产品", "department", "研发", "limit", 10))
                    .build();
            var result = client.callTool(request);

            assertThat(result.isError()).isFalse();
            assertThat(result.content()).singleElement().satisfies(content -> {
                assertThat(content).isInstanceOf(McpSchema.TextContent.class);
                assertThat(((McpSchema.TextContent) content).text()).contains("王芳", "E1003");
            });
        }
    }
}
