package com.training.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.training.dao.DBConfig;
import com.training.dao.EmployeeDao;
import com.training.model.Employee;
import com.training.service.EmployeeService;

@Configuration
public class AppConfig {
	
	//define BeanDefinitions

//	@Bean(name = "empDao")
//	@Scope("singleton")
//	public EmployeeDao employeeDao() {
//		return new EmployeeDao();
//	}
//	
//	@Bean("empService")
//	@Scope("singleton")
//	public EmployeeService employeeService(EmployeeDao employeeDao) {
//		return new EmployeeService(employeeDao);
//	}
//	
	@Bean(name = "emp1", initMethod = "customInitMethod")
	public Employee employee1() {
		return new Employee();
	}
	
	@Bean
	public DBConfig dbConfig() {
		return new DBConfig();
	}
	
//	@Bean(name = "emp2")
//	public Employee employee2() {
//		Employee emp = new Employee();
//		
//		emp.setId(102);
//		emp.setName("Monali");
//		emp.setCity("Pune");
//		return emp;
//	}
	
}
