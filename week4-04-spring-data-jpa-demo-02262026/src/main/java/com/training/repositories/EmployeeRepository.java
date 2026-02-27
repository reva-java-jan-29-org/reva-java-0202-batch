package com.training.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.training.entities.Employee;

import jakarta.transaction.Transactional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
	
	@Query("SELECT e FROM Employee e WHERE e.city = :city")		//JPQL Query
	List<Employee> getAllEmployeesByCity(@Param("city") String city);
	
	
//	@Query(value = "select id, name, city, salary from Employee where city = :city", nativeQuery = true)
//	List<Employee> getAllEmployeesByCity(@Param("city") String city);

//	@Query("SELECT e FROM Employee e WHERE e.salary > :salary and e.city = :city")
//	List<Employee> getAllEmployeesBySalary(@Param("salary") double salary, @Param("city") String city);
	
	List<Employee> findAllByCity(String city);
	
	List<Employee> findAllByCityAndSalaryGreaterThan(String city, double salary);
	
	@Modifying
	@Transactional
	@Query("UPDATE Employee e SET e.salary= :salary WHERE e.city = :city")
	int incrementSalaryByCity(double salary, String city);
	
}

//class EmployeeRepositoryImpl implements EmployeeRepository{ 

//}

//Spring data jpa automatically provides a "proxy instance" that implements this "EmployeeRepositoty" interface 