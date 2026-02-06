import java.util.Scanner;

public class BankSystemTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Scanner scanner = new Scanner(System.in);
		
		BankAccount account1 = new BankAccount();
		
		System.out.print("Enter deposit amount : ");
		double amount = scanner.nextDouble();
		
		account1.deposit(amount);
		
		
		System.out.println("Current Balance : " + account1.getBalance());

	}

}
