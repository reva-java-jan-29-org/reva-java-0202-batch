package com.training;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import com.training.entities.Employee;
import com.training.repositories.EmployeeRepository;

@SpringBootApplication
public class Week404SpringDataJpaDemo02262026Application {

	public static void main(String[] args) {
	
	  ApplicationContext context =	SpringApplication.run(Week404SpringDataJpaDemo02262026Application.class, args);
	
	  EmployeeRepository empRep = context.getBean(EmployeeRepository.class);
	
	  Employee emp = new Employee(103L, "Siddhant", "Pune", 12000.00);
	  empRep.save(emp);
	  
	}

}
