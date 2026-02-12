import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class QueueDemo03 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		
		BlockingQueue<Integer> bq = new ArrayBlockingQueue<Integer>(5);
		
		bq.offer(10);
		bq.offer(20);
		bq.offer(30);
		bq.offer(40);
		bq.offer(50);
		
		Runnable task = new Runnable() {
			@Override
			public void run() {
				System.out.println(Thread.currentThread().getName() + " trying to add 6th element in the queue: ");
				try {
					bq.put(60);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("added 6th item");
				System.out.println(bq);
				
			}
		};
		
		
		Thread thread = new Thread(task);
		thread.start();
		
		
		try {
			Thread.sleep(10000);
			System.out.println(Thread.currentThread().getName() + " removing an item from the queue....");
			System.out.println("removed :" + bq.take());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

}
