package com.training.ecommerce;

public class CustomerTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Customer customer1 = new Customer("101", "test@test.com", "Test");
		Customer customer2 = new Customer("102", "test2@test.com", "Test");
		
		System.out.println("Customer 1 :" + customer1);
		System.out.println("Customer 2 :" + customer2);
		
		if(customer1.equals(customer2)) {
			System.out.println("Both customers are same");
		}else {
			System.out.println("Not same");
		}

	}

}
