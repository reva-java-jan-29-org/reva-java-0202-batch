package com.training.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.training.dao.CustomerDao;

@Component
public class CustomerService {
	
	private CustomerDao customerDao;
	
	public CustomerService() {
		System.out.println("CustomerService no-arg constructor");
	}
	
	@Autowired
	public CustomerService(@Qualifier("custJDBCImpl") CustomerDao customerDao) {
		this.customerDao = customerDao;
		System.out.println("CustomerService param-constructor to initialize customerDao");
	}

}
