package com.training.onetomany.unidirectional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Employee (OneToMany Unidirectional).
 *
 * NOTE: In the unidirectional mapping, Employee has no reference to Department.
 * If you need to find all employees of a department, use:
 *   departmentRepository.findById(deptId).get().getEmployees()
 *
 * Employee can be persisted independently or as part of Department
 * via cascade (when added to department.getEmployees() and dept is saved).
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByCity(String city);

    List<Employee> findBySalaryGreaterThan(Double salary);
}
