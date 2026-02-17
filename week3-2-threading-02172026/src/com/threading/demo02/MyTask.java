package com.threading.demo02;

public class MyTask implements Runnable{

	@Override
	public void run() {
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
