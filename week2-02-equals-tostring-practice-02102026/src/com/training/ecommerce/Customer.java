package com.training.ecommerce;

import java.util.Objects;

public class Customer {
	
	private final String customerId;		//internal Id
	private final String email;
	private final String fullName;
	
	public Customer(String customerId, String email, String fullName) {
		super();
		this.customerId = customerId;
		this.email = email;
		this.fullName = fullName;
	}

	public String getCustomerId() {
		return customerId;
	}

	public String getEmail() {
		return email;
	}

	public String getFullName() {
		return fullName;
	}
	
	private static String normalizeString(String email) {
		Objects.requireNonNull(email);
		return email.trim().toLowerCase();
	}
	
	//test@gmail.com			t***@gmail.com
	private static String maskEmail(String email) {
		
			int at = email.indexOf("@");
			
			if(at < 1) return "***";
			
			return email.charAt(0) + "***" + email.substring(at);
	}
	
	//override the toString() method to print the customer info 

	@Override
	public String toString() {
		return "Customer [customerId=" + customerId +  " Email: " + maskEmail(email) + " FullName :" + fullName + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(email);
	}
	
	//override the equals() method to compare two customers 

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Customer other = (Customer) obj;
		return Objects.equals(email, other.email);
	}
}
