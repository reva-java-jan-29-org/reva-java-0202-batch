package com.threading.demo02;

public class Test02_Annoymous_Object {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Runnable task = new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				System.out.println("this code runs inside a thread!...");
			}
		};
		
		Thread t = new Thread(task);
		t.start();

	}

}
