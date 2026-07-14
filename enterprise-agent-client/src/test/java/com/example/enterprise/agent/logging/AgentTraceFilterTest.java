package com.example.enterprise.agent.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AgentTraceFilterTest {

    private final AgentTraceFilter filter = new AgentTraceFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldReuseValidTraceIdAndClearMdcAfterRequest() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(AgentTraceFilter.TRACE_HEADER, "caller-trace-001");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                assertThat(MDC.get("traceId")).isEqualTo("caller-trace-001"));

        assertThat(response.getHeader(AgentTraceFilter.TRACE_HEADER)).isEqualTo("caller-trace-001");
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    void shouldReplaceUnsafeTraceId() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(AgentTraceFilter.TRACE_HEADER, "unsafe\ntrace");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                assertThat(MDC.get("traceId")).matches("[a-f0-9-]{36}"));

        assertThat(response.getHeader(AgentTraceFilter.TRACE_HEADER)).matches("[a-f0-9-]{36}");
        assertThat(MDC.get("traceId")).isNull();
    }
}
