import java.util.function.Predicate;

public class PredicateExample01 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		
		int[] array = {12,21,4,45,65,34,6,34,7,23,12,54,34,6,54,87};
		
		
		
		Predicate<Integer> p1 = value -> value % 2 == 1;
		showArray(array, p1);
		
	}
	
	public static void showArray(int[] array, Predicate<Integer> predicate ) {
		for(int value: array) {
			if(predicate.test(value))
				System.out.print(value + " ");
		}
	}

}
