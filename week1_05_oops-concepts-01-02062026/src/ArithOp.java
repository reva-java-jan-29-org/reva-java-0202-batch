
public class ArithOp {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		int x = 10, y = 20;
		byte p = 2, q = 4;
		short s = 5, r = 10;
		
		arithAdd(s, r);

	}

	
	public static void arithAdd(int a, int b) {
		System.out.println("method 1");
		int sum = a + b;
		System.out.println("sum :" + sum);
	}
	
	public static void arithAdd(byte a, byte b) {
		System.out.println("method 2");
		int sum = a + b;
		System.out.println("sum :" + sum);
	}
	
	public static void arithAdd(int a, byte b) {
		System.out.println("method 3");
		int sum = a + b;
		System.out.println("sum :" + sum);
	}
	
	public static void arithAdd(byte a, int b) {
		System.out.println("method 4");
		int sum = a + b;
		System.out.println("sum :" + sum);
	}
	
	public static int arithAdd(short a, short b) {
		System.out.println("method 5");
		int sum = a + b;
		System.out.println("sum :" + sum);
		
		return sum;
	}
}
