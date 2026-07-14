package com.example.enterprise.mcp.adapter;

import com.example.enterprise.query.model.DepartmentSummary;
import com.example.enterprise.query.model.EmployeeSummary;
import com.example.enterprise.query.port.EnterpriseQueryPort;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryEnterpriseQueryAdapter implements EnterpriseQueryPort {
    private static final List<EmployeeSummary> EMPLOYEES = List.of(
            new EmployeeSummary("E1001", "张伟", "研发中心", "高级工程师", "zhangwei@example.com"),
            new EmployeeSummary("E1002", "李娜", "财务部", "财务经理", "lina@example.com"),
            new EmployeeSummary("E1003", "王芳", "研发中心", "产品经理", "wangfang@example.com"));

    @Override
    public List<EmployeeSummary> searchEmployees(String keyword, String department, int limit) {
        String normalizedKeyword = normalize(keyword);
        String normalizedDepartment = normalize(department);
        int safeLimit = Math.clamp(limit, 1, 50);
        return EMPLOYEES.stream()
                .filter(employee -> normalizedKeyword.isBlank()
                        || normalize(employee.name()).contains(normalizedKeyword)
                        || normalize(employee.employeeId()).contains(normalizedKeyword)
                        || normalize(employee.title()).contains(normalizedKeyword))
                .filter(employee -> normalizedDepartment.isBlank()
                        || normalize(employee.department()).contains(normalizedDepartment))
                .limit(safeLimit)
                .toList();
    }

    @Override
    public Optional<DepartmentSummary> findDepartment(String departmentId) {
        return switch (normalize(departmentId)) {
            case "rd", "研发中心" -> Optional.of(new DepartmentSummary("RD", "研发中心", "赵敏", 128));
            case "fin", "财务部" -> Optional.of(new DepartmentSummary("FIN", "财务部", "周强", 32));
            default -> Optional.empty();
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
