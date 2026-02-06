
public class BankAccount {

	private double balance;

	public void deposit(double amount) {

		if (amount <= 0) {
			throw new IllegalArgumentException("Invalid deposit amount");
		}

		this.balance = this.balance + amount;
	}

	public void withdraw(double amount) {

		if (amount <= 0)
			throw new IllegalArgumentException("Invalid withdraw");

		if (amount > balance)
			throw new IllegalStateException("Insufficient funds");

		balance -= amount;
	}

	public double getBalance() {
		return balance;
	}

}
