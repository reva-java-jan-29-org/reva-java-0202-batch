package com.training.dao;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.training.model.Customer;

@Component()
public class CustomerDaoJDBCImpl implements CustomerDao {

	public CustomerDaoJDBCImpl() {
		System.out.println("CustomerDaoJDBCImpl no-arg constructor is called");
	}

	@Override
	public Customer saveCustomer(Customer customer) {
		// TODO Auto-generated method stub
		System.out.println("Saving the given customer to the db using CustomerDaoJDBCImpl");
		return null;
	}
}
