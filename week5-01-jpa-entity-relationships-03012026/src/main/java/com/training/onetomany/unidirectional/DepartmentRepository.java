package com.training.onetomany.unidirectional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Department (OneToMany Unidirectional).
 *
 * Since the relationship is UNIDIRECTIONAL (Department -> Employees),
 * you can only navigate from Department to Employees, not the other way.
 *
 * To find all employees of a department, you call:
 *   department.getEmployees()
 *
 * You CANNOT query: "Find the department for a given employee"
 * using the JPA relationship (Employee has no department reference).
 * For that, you'd need a native/JPQL query on the FK column.
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByDeptName(String deptName);
}
