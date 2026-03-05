package com.training.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.training.dto.EmployeeResponse;
import com.training.entities.Employee;
import com.training.repositories.EmployeeRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.NoArgsConstructor;

@Service
@NoArgsConstructor
public class EmployeeService {

	@Autowired
	private EmployeeRepository employeeRepository;
	
	public Employee findById(Long id) {
		return employeeRepository.findById(id)
				.orElseThrow(()-> new EntityNotFoundException("Employee with the given ID not found!"));
	}
	
	public List<EmployeeResponse> findAllEmployees(){
		
		List<Employee> list = employeeRepository.findAll();
		
		return list.stream()
				.map(emp -> new EmployeeResponse(
						emp.getEmpId(),
						emp.getName(), 
						emp.getCity(), 
						"****"
						))
				.toList();
		
	}
	
	public EmployeeResponse save(Employee emp) {
		
	  Employee savedEmp = employeeRepository.save(emp);
	  
	  EmployeeResponse employeeResponse = new EmployeeResponse();
	  employeeResponse.setEmpId(savedEmp.getEmpId());
	  employeeResponse.setName(savedEmp.getName());
	  employeeResponse.setCity(savedEmp.getCity());
	  employeeResponse.setSalary(employeeResponse.convertDoubleToString(savedEmp.getSalary()));
	  
	  return employeeResponse;
	}
	
}
