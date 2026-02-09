package com.training.tostringdemo;

public class EmployeeTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Employee employee1 = new Employee(101, "Rohit", 12000.00);
		
		Employee employee2 = new Employee(101, "Rohit", 12000.00);
		
		System.out.println("employee1: " + employee1); 		//toString()
		System.out.println("employee2: " + employee2);
		
//		if(employee1 == employee2) {
//			System.out.println("both are same");
//		}else {
//			System.out.println("not same");
//		}

		
		if(employee1.equals(employee2)) {
			System.out.println("Both are equal");
		}else {
			System.out.println("Not equal");
		}
		
		 
		
		
	}

}
