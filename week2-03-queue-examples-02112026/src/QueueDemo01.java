import java.util.ArrayDeque;
import java.util.Queue;

public class QueueDemo01 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		
		Queue<String> queue = new ArrayDeque<>();
		
		queue.add("Ticket-1");
		queue.add("Ticket-2");
		queue.offer("Ticket-3");
		
		System.out.println(queue);
		
		String item  = queue.poll();
		System.out.println("Item processed :" + item);
		
		System.out.println(queue);
		
	}

}
