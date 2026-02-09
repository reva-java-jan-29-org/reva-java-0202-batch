package com.training.shippingsystem;

public abstract class Order {

	protected String orderId;
	protected String customerEmail;
	protected String[] items;
	protected double baseAmount;
	protected double discount;
	protected OrderStatus status;
	
	public Order() {
		super();
	}

	public Order(String orderId, String customerEmail, String[] items, double baseAmount, double discount,
			OrderStatus status) {
		super();
		this.orderId = orderId;
		this.customerEmail = customerEmail;
		this.items = items;
		this.baseAmount = baseAmount;
		this.discount = discount;
		this.status = status;
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public String getCustomerEmail() {
		return customerEmail;
	}

	public void setCustomerEmail(String customerEmail) {
		this.customerEmail = customerEmail;
	}

	public String[] getItems() {
		return items;
	}

	public void setItems(String[] items) {
		this.items = items;
	}

	public double getBaseAmount() {
		return baseAmount;
	}

	public void setBaseAmount(double baseAmount) {
		this.baseAmount = baseAmount;
	}

	public double getDiscount() {
		return discount;
	}

	public void setDiscount(double discount) {
		this.discount = discount;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public void setStatus(OrderStatus status) {
		this.status = status;
	}
	
	protected abstract double calculateTax();
	
	protected abstract double calculateExtraFees();
	
	//processOrder()
	
}
