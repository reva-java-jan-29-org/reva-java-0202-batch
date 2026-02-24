package com.training.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.training.dao.EmployeeDao;
import com.training.model.Employee;
import com.training.service.EmployeeService;

@Configuration
public class AppConfig {
	
	//define BeanDefinitions

	@Bean(name = "empDao")
	@Scope("prototype")
	public EmployeeDao employeeDao() {
		return new EmployeeDao();
	}
	
	@Bean
	@Scope("prototype")
	public EmployeeService employeeService(EmployeeDao employeeDao) {
		return new EmployeeService(employeeDao);
	}
	
	@Bean(name = "emp1")
	public Employee employee1() {
		return new Employee(101, "Mayuri", "Pune", 120000.00, "Persistent");
	}
	
	@Bean(name = "emp2")
	public Employee employee2() {
		Employee emp = new Employee();
		
		emp.setId(102);
		emp.setName("Monali");
		emp.setCity("Pune");
		return emp;
	}
	
}
