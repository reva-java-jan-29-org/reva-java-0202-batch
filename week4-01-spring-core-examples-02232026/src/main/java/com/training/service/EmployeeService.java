package com.training.service;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.training.dao.EmployeeDao;

@Component
public class EmployeeService implements BeanNameAware, ApplicationContextAware {
	
	
	private EmployeeDao employeeDao;

	public EmployeeService() {
		System.out.println("1. instantiation is done! EmployeeService no-arg constructor is called");
	}

	
	public EmployeeService(EmployeeDao employeeDao) {
		super();
		this.employeeDao = employeeDao;
		System.out.println("1. instantiation is done! EmployeeService object is created and initialized using param-constructor ");
	}

	public EmployeeDao getEmployeeDao() {
		return employeeDao;
	}

	@Autowired
	public void setEmployeeDao(EmployeeDao employeeDao) {
		this.employeeDao = employeeDao;
		System.out.println("2. population: setEmployeeDao() is setting value for the employeeDao");
	}
	
	public void processSalary() {
		System.out.println("Processing Salary");
	}


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		// TODO Auto-generated method stub
		
		System.out.println("3. Aware interface: getting access to the applicationcontext");
	}


	@Override
	public void setBeanName(String name) {
		// TODO Auto-generated method stub
		System.out.println("3. Aware interface: Setting name = " + name + " for the bean");
	}
	
	
}
