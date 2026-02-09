package com.training.shippingsystem;

public class FragileOrder extends PhysicalOrder {

	public FragileOrder() {
		super();
		// TODO Auto-generated constructor stub
	}

	public FragileOrder(String orderId, String customerEmail, String[] items, double baseAmount, double discount,
			OrderStatus status, String shippingAddress, String courierPartner) {
		super(orderId, customerEmail, items, baseAmount, discount, status, shippingAddress, courierPartner);
		// TODO Auto-generated constructor stub
	}

	protected  double calculateExtraFees() {
		return 200.00;
	}
}
