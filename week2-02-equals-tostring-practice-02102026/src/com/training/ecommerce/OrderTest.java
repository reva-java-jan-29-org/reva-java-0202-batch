package com.training.ecommerce;

public class OrderTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		Customer customer1 = new Customer("101", "test@test.com", "Test");
		
		Order order1 = new Order("321", customer1);
		order1.setTotalCost(3210.00);
		
		Order order2 = new Order("321", customer1);
		order2.setTotalCost(3210.00);
		
		System.out.println("Order Summary :" + order1);

	}

}
