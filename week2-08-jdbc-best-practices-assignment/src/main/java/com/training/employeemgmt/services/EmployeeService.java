package com.training.employeemgmt.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import com.training.employeemgmt.dao.EmployeeDao;
import com.training.employeemgmt.model.Employee;

public final class EmployeeService {
    private final EmployeeDao dao;

    public EmployeeService(EmployeeDao dao) {
        this.dao = Objects.requireNonNull(dao);
    }

    public Employee createEmployee(Employee e) {
        validateForCreate(e);

        // Example robustness: prevent duplicate empCode early (DB unique will also protect you)
        dao.findByEmpCode(e.getEmpCode()).ifPresent(existing -> {
            throw new ValidationException("Employee code already exists: " + e.getEmpCode());
        });

        return dao.create(e);
    }

    public Employee getEmployee(long id) {
        return dao.findById(id).orElseThrow(() -> new NotFoundException("Employee not found: id=" + id));
    }

    public List<Employee> listEmployees(int limit, int offset) {
        if (limit <= 0 || limit > 1000) throw new ValidationException("limit must be 1..1000");
        if (offset < 0) throw new ValidationException("offset must be >= 0");
        return dao.findAll(limit, offset);
    }

    public Employee updateEmployee(Employee e) {
        validateForUpdate(e);

        // Ensure exists
        getEmployee(e.getId());

        return dao.update(e);
    }

    public void deleteEmployee(long id) {
        boolean deleted = dao.deleteById(id);
        if (!deleted) throw new NotFoundException("Employee not found to delete: id=" + id);
    }

    private void validateForCreate(Employee e) {
        Objects.requireNonNull(e, "employee required");
        if (e.getId() != null) throw new ValidationException("New employee must not have id");
        validateCommon(e);
    }

    private void validateForUpdate(Employee e) {
        Objects.requireNonNull(e, "employee required");
        if (e.getId() == null) throw new ValidationException("Update requires id");
        validateCommon(e);
    }

    private void validateCommon(Employee e) {
        if (isBlank(e.getEmpCode())) throw new ValidationException("empCode required");
        if (e.getEmpCode().length() > 20) throw new ValidationException("empCode max length 20");
        if (isBlank(e.getFullName())) throw new ValidationException("fullName required");
        if (isBlank(e.getEmail()) || !e.getEmail().contains("@")) throw new ValidationException("valid email required");
        if (isBlank(e.getDepartment())) throw new ValidationException("department required");
        if (e.getSalary() == null || e.getSalary().compareTo(BigDecimal.ZERO) <= 0) throw new ValidationException("salary must be > 0");
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
