package com.example.enterprise.agent.stdio;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.client.common.autoconfigure.properties.McpStdioClientProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

class StdioClientConfigurationTest {

    @Test
    void shouldBindEnterpriseQueryStdioConnection() throws IOException {
        var environment = new StandardEnvironment();
        var propertySources = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"));
        propertySources.forEach(environment.getPropertySources()::addLast);

        var properties = Binder.get(environment)
                .bind("spring.ai.mcp.client.stdio", Bindable.of(McpStdioClientProperties.class))
                .orElseThrow(() -> new IllegalStateException("STDIO MCP client configuration is missing"));
        var connection = properties.getConnections().get("enterprise-query");

        assertThat(connection).isNotNull();
        assertThat(connection.command()).isEqualTo("java");
        assertThat(connection.args())
                .containsExactly(
                        "-jar",
                        "enterprise-mcp-server-stdio/target/enterprise-mcp-server-stdio-0.1.0-SNAPSHOT.jar");
    }
}
