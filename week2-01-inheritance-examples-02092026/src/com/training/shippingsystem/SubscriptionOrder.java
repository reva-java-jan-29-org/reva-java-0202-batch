package com.training.shippingsystem;

public class SubscriptionOrder extends PhysicalOrder {
	
	private int weeks;

	public SubscriptionOrder() {
		super();
		// TODO Auto-generated constructor stub
	}

	public SubscriptionOrder(String orderId, String customerEmail, String[] items, double baseAmount, double discount,
			OrderStatus status, String shippingAddress, String courierPartner, int weeks) {
		super(orderId, customerEmail, items, baseAmount, discount, status, shippingAddress, courierPartner);
		this.weeks = weeks;
	}
	
	

}
