package com.training.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.training.entities.Employee;

@Repository
public interface EmployeeRepository extends CrudRepository<Employee, Long> {

//	   Employee save(Employee entity)
//     Optional<Employee> findById(Long id);
//     boolean existsById(Long id);
//     Iterable<Employee> findAll();
//     long count();
}

//Spring data jpa automatically provides a "proxy instance" that implements this "EmployeeRepositoty" interfa