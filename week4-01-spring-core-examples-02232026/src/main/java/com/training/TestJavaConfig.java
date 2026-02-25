package com.training;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.training.config.AppConfig;
import com.training.model.Employee;

public class TestJavaConfig {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
	
		Employee emp1 = context.getBean("emp1", Employee.class);
		System.out.println(emp1);
		
		
	}

}
