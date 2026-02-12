package com.training.employeemgmt.dao;

import java.util.List;
import java.util.Optional;

import com.training.employeemgmt.model.Employee;

public interface EmployeeDao {
	
    Employee create(Employee employee);
    
    Optional<Employee> findById(long id);
    
    Optional<Employee> findByEmpCode(String empCode);
    
    List<Employee> findAll(int limit, int offset);
    
    List<Employee> findByDepartment(String department, int limit, int offset);
    
    Employee update(Employee employee);          // by id
    
    boolean deleteById(long id);
}

