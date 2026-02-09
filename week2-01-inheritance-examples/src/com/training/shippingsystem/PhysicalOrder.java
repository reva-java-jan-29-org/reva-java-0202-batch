package com.training.shippingsystem;

public class PhysicalOrder extends Order {

	protected String shippingAddress;
	protected String courierPartner;
	
	public PhysicalOrder() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public PhysicalOrder(String orderId, String customerEmail, String[] items, double baseAmount, double discount,
			OrderStatus status, String shippingAddress, String courierPartner) {
		super(orderId, customerEmail, items, baseAmount, discount, status);
		this.shippingAddress = shippingAddress;
		this.courierPartner = courierPartner;
	}
	
	@Override
	protected double calculateTax() {
		// TODO Auto-generated method stub
		return baseAmount * 0.10;
	} 
	
	protected  double calculateExtraFees() {
		return 100.0;
	}
	

}
