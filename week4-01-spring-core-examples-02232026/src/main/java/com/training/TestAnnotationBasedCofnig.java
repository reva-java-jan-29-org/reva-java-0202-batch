package com.training;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.training.model.Employee;


public class TestAnnotationBasedCofnig {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		ApplicationContext context = new AnnotationConfigApplicationContext(AnnotationBasedConfig.class);
	
//		Employee e1 = (Employee) context.getBean("emp1");
//		System.out.println(e1);
//		
//		Employee e2 = (Employee) context.getBean("emp2");
//		System.out.println(e2);
		
//		Employee e3 = (Employee) context.getBean(Employee.class);
//		System.out.println(e3);
		
	}

}
