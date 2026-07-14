package com.example.enterprise.agent.stdio.web;

import com.example.enterprise.agent.stdio.service.EnterpriseStdioAgentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agent")
public class StdioAgentController {
    private final EnterpriseStdioAgentService agentService;

    public StdioAgentController(EnterpriseStdioAgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping(value = "/query", consumes = MediaType.APPLICATION_JSON_VALUE)
    public QueryResponse query(@Valid @RequestBody QueryRequest request) {
        return new QueryResponse(agentService.ask(request.question()));
    }

    public record QueryRequest(@NotBlank String question) {}
    public record QueryResponse(String answer) {}
}
