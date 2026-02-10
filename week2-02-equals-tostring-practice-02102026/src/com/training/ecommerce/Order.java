package com.training.ecommerce;

import java.time.Instant;

public class Order {

	protected final String orderId;
	protected final Customer customer;
	
	//order may have some lineItems
	protected final Instant createdAt;
	protected double totalCost;

	public Order(String orderId, Customer customer) {
		super();
		this.orderId = orderId;
		this.customer = customer;
		this.createdAt = Instant.now();
	}

	public String getOrderId() {
		return orderId;
	}

	public double getTotalCost() {
		return totalCost;
	}

	public void setTotalCost(double totalCost) {
		this.totalCost = totalCost;
	}
	
	//override the toString() method to print the orderSummary
	//Order [orderId: XXXX, customerName: XXXXXX, totalCost: XXXXX, createdAt: XXXXX]
	
	
	
	//override the equals() method to compare orders
}
