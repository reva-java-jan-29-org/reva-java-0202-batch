package com.training.manytoone;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Department (ManyToOne).
 *
 * In unidirectional @ManyToOne, Department is the target side.
 * It has no knowledge of employees. To find all employees of a
 * department, you use EmployeeRepository.findByDepartment().
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByDeptName(String deptName);
}
