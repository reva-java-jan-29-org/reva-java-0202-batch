package com.training.onetomany.bidirectional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Department (OneToMany Bidirectional).
 *
 * Department is the INVERSE side (mappedBy). It does NOT own the FK.
 * However, with CascadeType.ALL and the helper addEmployee() method,
 * saving a Department will persist all its employees.
 *
 * Custom JPQL query to fetch department with employees eagerly loaded
 * (useful to avoid LazyInitializationException outside a transaction).
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByDeptName(String deptName);

    // JPQL with JOIN FETCH - eagerly loads employees in a single query
    // Prevents N+1 query problem when accessing employees in a loop
    @Query("SELECT d FROM OtmBiDepartment d JOIN FETCH d.employees WHERE d.deptId = :deptId")
    Optional<Department> findByIdWithEmployees(Long deptId);

    List<Department> findByLocation(String location);
}
