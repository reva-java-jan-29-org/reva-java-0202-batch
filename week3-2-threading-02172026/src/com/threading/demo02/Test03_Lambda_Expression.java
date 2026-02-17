package com.threading.demo02;

public class Test03_Lambda_Expression {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Runnable task = () -> System.out.println("this code runs inside a thread!...");

		Thread t = new Thread(task);
		t.start();
	}

}
