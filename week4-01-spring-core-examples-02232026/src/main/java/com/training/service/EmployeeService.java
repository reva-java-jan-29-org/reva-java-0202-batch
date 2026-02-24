package com.training.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.training.dao.EmployeeDao;

@Component
public class EmployeeService {
	
	
	private EmployeeDao employeeDao;

	public EmployeeService() {
		System.out.println("EmployeeService no-arg constructor is called");
	}

	
	public EmployeeService(EmployeeDao employeeDao) {
		super();
		this.employeeDao = employeeDao;
		System.out.println("EmployeeService object is created and initialized using param-constructor ");
	}

	public EmployeeDao getEmployeeDao() {
		return employeeDao;
	}

	@Autowired
	public void setEmployeeDao(EmployeeDao employeeDao) {
		this.employeeDao = employeeDao;
		System.out.println("setEmployeeDao() is setting value for the employeeDao");
	}
	
	public void processSalary() {
		System.out.println("Processing Salary");
	}
	
	
}
