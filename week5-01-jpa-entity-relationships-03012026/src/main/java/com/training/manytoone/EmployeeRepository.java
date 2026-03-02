package com.training.manytoone;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Employee (ManyToOne).
 *
 * Employee is the OWNING side with the FK.
 * You can query employees by department directly.
 *
 * findByDepartment() - Spring Data JPA generates:
 *   SELECT * FROM mto_employees WHERE fk_dept_id = ?
 *
 * findByDepartmentDeptId() - Traverse the relationship to the department's ID
 *   Spring Data JPA generates the same join query automatically
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // Find all employees in a given Department object
    List<Employee> findByDepartment(Department department);

    // Find all employees by department ID (no need to load Department first)
    List<Employee> findByDepartmentDeptId(Long deptId);

    // Find employees by department name (traverses the relationship)
    List<Employee> findByDepartmentDeptName(String deptName);

    // JPQL query with JOIN FETCH to load Employee + Department in one query
    // Prevents N+1 when iterating over a list of employees and accessing dept
    @Query("SELECT e FROM MtoEmployee e JOIN FETCH e.department")
    List<Employee> findAllWithDepartment();

    List<Employee> findBySalaryBetween(Double min, Double max);
}
