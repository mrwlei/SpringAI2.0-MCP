package com.example.enterprise.query.port;

import com.example.enterprise.query.model.DepartmentSummary;
import com.example.enterprise.query.model.EmployeeSummary;
import java.util.List;
import java.util.Optional;

public interface EnterpriseQueryPort {
    List<EmployeeSummary> searchEmployees(String keyword, String department, int limit);
    Optional<DepartmentSummary> findDepartment(String departmentId);
}
