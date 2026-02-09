package com.training.shippingsystem;

public class DigitalOrder extends Order {

	public DigitalOrder() {
		super();
		// TODO Auto-generated constructor stub
	}

	public DigitalOrder(String orderId, String customerEmail, String[] items, double baseAmount, double discount,
			OrderStatus status) {
		super(orderId, customerEmail, items, baseAmount, discount, status);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected double calculateTax() {
		// TODO Auto-generated method stub
		return baseAmount * 0.05;
	}
	
	protected  double calculateExtraFees() {
		return 0.0;
	}
	
	
}
