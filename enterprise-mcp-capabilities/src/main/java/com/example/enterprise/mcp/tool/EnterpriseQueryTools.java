package com.example.enterprise.mcp.tool;

import com.example.enterprise.query.model.DepartmentSummary;
import com.example.enterprise.query.model.EmployeeSummary;
import com.example.enterprise.query.port.EnterpriseQueryPort;
import java.util.List;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class EnterpriseQueryTools {
    private final EnterpriseQueryPort queryPort;

    public EnterpriseQueryTools(EnterpriseQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    @McpTool(
            name = "search_employees",
            title = "查询企业员工",
            description = "按姓名、工号、职位和部门查询企业员工，只返回用户有权查看的基础资料",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public List<EmployeeSummary> searchEmployees(
            @McpToolParam(description = "姓名、工号或职位关键词；空字符串表示不限", required = true) String keyword,
            @McpToolParam(description = "部门名称；空字符串表示不限", required = true) String department,
            @McpToolParam(description = "返回数量，范围 1 到 50", required = true) int limit) {
        return queryPort.searchEmployees(keyword, department, limit);
    }

    @McpTool(
            name = "get_department",
            title = "查询部门概况",
            description = "根据部门编号或名称查询部门概况",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public DepartmentSummary getDepartment(
            @McpToolParam(description = "部门编号或名称", required = true) String departmentId) {
        return queryPort.findDepartment(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("部门不存在: " + departmentId));
    }
}
