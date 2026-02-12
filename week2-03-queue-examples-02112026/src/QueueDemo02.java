import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

public class QueueDemo02 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Queue<Integer> q = new PriorityQueue<>(Comparator.reverseOrder());
		
		q.offer(50);
		q.offer(10);
		q.offer(30);
		
		System.out.println("item processed and removed :" + q.poll());

	}

}
