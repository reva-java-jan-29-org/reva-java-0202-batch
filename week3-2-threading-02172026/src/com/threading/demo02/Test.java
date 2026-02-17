package com.threading.demo02;

public class Test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Runnable task1 = new MyTask();
		
		Thread t1 = new Thread(task1);
		Thread t2 = new Thread(task1);
		
		t1.start();
		t2.start();

	}

}
