package com.threading.demo01;

public class MyThread extends Thread {
	
	public MyThread(String name) {
		super(name);
	}

	@Override
	public void run() {
		//code that executes inside a thread
		
		System.out.println(Thread.currentThread().getName() + " has started..");
		for(int i=1; i<=10; i++) {
			System.out.println(Thread.currentThread().getName() + " - i :" + i);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println(Thread.currentThread().getName() + " has ended!");
	}
}
