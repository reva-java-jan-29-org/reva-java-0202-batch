package com.training.onetoone.unidirectional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Employee entity (OneToOne Unidirectional).
 *
 * Spring Data JPA automatically provides implementations for:
 * - save(), findById(), findAll(), delete(), count(), existsById(), etc.
 *
 * Custom query methods can be added using:
 * - Method name conventions (findBy..., existsBy...)
 * - @Query annotation (JPQL or native SQL)
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByCity(String city);

    List<Employee> findByName(String name);
}
