package com.threading.demo01;

public class Test {

	public static void main(String[] args) throws InterruptedException {
		// TODO Auto-generated method stub
		
		System.out.println("Current Thread :" + Thread.currentThread().getName());
		
		MyThread thread1 = new MyThread("thread1");
		MyThread thread2 = new MyThread("thread2");

		System.out.println("thread1 state : " + thread1.getState());
		System.out.println("thread2 state : " + thread2.getState());
		
		thread1.start();
		thread2.start();
		
		
		System.out.println(Thread.currentThread().getName() + " has started..");
		for(int i=1; i<=10; i++) {
			System.out.println("\t" + Thread.currentThread().getName() + " - i :" + i);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println(Thread.currentThread().getName() + " has ended!");
		
		System.out.println("thread1 state : " + thread1.getState());
		System.out.println("thread2 state : " + thread2.getState());
		
		Thread.sleep(500);
		thread1.interrupt();
		
		System.out.println("thread1 state : " + thread1.getState());
		System.out.println("thread2 state : " + thread2.getState());
		
		
		thread1.join();
		thread2.join();
		
		System.out.println("Exiting from main() method");
	}

}
