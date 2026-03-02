package com.training.onetoone.bidirectional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Employee entity (OneToOne Bidirectional).
 *
 * Employee is the OWNING side (has @JoinColumn).
 * Saving an Employee with a Profile set will persist both
 * because of CascadeType.ALL on the relationship.
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByCity(String city);
}
