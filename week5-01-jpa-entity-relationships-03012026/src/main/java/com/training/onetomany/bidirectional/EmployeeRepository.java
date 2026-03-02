package com.training.onetomany.bidirectional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Employee (OneToMany Bidirectional).
 *
 * Employee is the OWNING side (has @JoinColumn, has FK).
 * Setting employee.setDepartment(dept) and saving the employee
 * will write the FK to the employees table.
 *
 * You can query employees by department since Employee has the FK.
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // Spring Data JPA understands the 'department' relationship field
    List<Employee> findByDepartment(Department department);

    List<Employee> findByDepartmentDeptId(Long deptId);

    List<Employee> findBySalaryGreaterThan(Double salary);
}
