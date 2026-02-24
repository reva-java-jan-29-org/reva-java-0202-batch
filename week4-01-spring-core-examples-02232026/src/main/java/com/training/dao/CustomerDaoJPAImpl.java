package com.training.dao;

import org.springframework.stereotype.Component;

import com.training.model.Customer;

@Component
public class CustomerDaoJPAImpl implements CustomerDao {
	
	public CustomerDaoJPAImpl() {
		System.out.println("CustomerDaoJPAImpl object is created");
	}

	@Override
	public Customer saveCustomer(Customer customer) {
		// TODO Auto-generated method stub
		System.out.println("Saving the given customer using CustomerDaoJPAImpl ");
		return null;
	}

}
