package com.example.enterprise.mcp.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.enterprise.mcp.adapter.InMemoryEnterpriseQueryAdapter;
import org.junit.jupiter.api.Test;

class EnterpriseQueryToolsTest {

    private final EnterpriseQueryTools tools = new EnterpriseQueryTools(new InMemoryEnterpriseQueryAdapter());

    @Test
    void shouldSearchEmployeesUsingTheSharedCapability() {
        var employees = tools.searchEmployees("产品", "研发", 10);

        assertThat(employees).singleElement().satisfies(employee -> {
            assertThat(employee.employeeId()).isEqualTo("E1003");
            assertThat(employee.name()).isEqualTo("王芳");
        });
    }

    @Test
    void shouldReportMissingDepartment() {
        assertThatThrownBy(() -> tools.getDepartment("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("部门不存在: missing");
    }
}
