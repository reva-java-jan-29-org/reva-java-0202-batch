package com.training.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.training.dto.EmployeeRequest;
import com.training.dto.EmployeeResponse;
import com.training.entities.Employee;
import com.training.services.EmployeeService;

@RestController
public class EmployeeController {
	
	@Autowired
	private EmployeeService employeeService;
	
	
	@GetMapping("/employees")
	public List<EmployeeResponse> getAllEmployees(){
		
		return employeeService.findAllEmployees();
	}
	
	
	@PostMapping("/employees")
	public ResponseEntity<?> createEmployee(@RequestBody EmployeeRequest empRequset) {
		
		
		
		Employee employee = new Employee();
		employee.setCity(empRequset.getCity());
		employee.setName(empRequset.getName());
		employee.setSalary(empRequset.getSalary());
		
		EmployeeResponse empResponse = employeeService.save(employee);
		
		if(empResponse!=null)
			return new ResponseEntity<EmployeeResponse>(empResponse, HttpStatus.OK);
		else 
			return new ResponseEntity("Employee Couldn't be created", HttpStatus.NOT_FOUND);
	}
	
	
	@GetMapping("/employees/{id}")
	public ResponseEntity<?> getEmployee(@PathVariable() Long id) {
		
		Employee emp = employeeService.findById(id);
		
		if(emp!=null)
			return new ResponseEntity<Employee>(emp, HttpStatus.OK);
		else 
			return new ResponseEntity("Employee not found", HttpStatus.NOT_FOUND);
	}
	
	
	
	
	
	
	
	
	

	
//	@GetMapping("/employees")
//	public String listEmployees(Model model) {
//		
//		List<Employee> list = new ArrayList(
//				Arrays.asList(
//						new Employee(1L, "Vaishnavi", "Pune", 12000.00),
//						new Employee(2L, "Mayuri", "Mumbai", 11000.00),
//						new Employee(3L, "Sandhya", "Pune", 15000.00)
//						));
//		
//		model.addAttribute("employees", list);
//		
//		return "employees";	//return "viewname / name of your html file"
//	}
	
//	@GetMapping("/employeesdata")
//	@ResponseBody
//	public List<Employee> listEmployeesData() {
//		
//		List<Employee> list = new ArrayList(
//				Arrays.asList(
//						new Employee(1L, "Vaishnavi", "Pune", 12000.00),
//						new Employee(2L, "Mayuri", "Mumbai", 11000.00),
//						new Employee(3L, "Sandhya", "Pune", 15000.00)
//						));
//		
//		
//		return list; 
//	}
}
