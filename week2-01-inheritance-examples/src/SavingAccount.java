
public class SavingAccount extends BankAccount {

	private double interestRate;

	public SavingAccount() {
		//by default, it will invoke the super class no-arg constructor
		
		System.out.println("SAvingAccount no-arg constructor");
	}

	public SavingAccount(int id, String name, double interestRate) {
		
		super(id, name);
		this.interestRate = interestRate;
		System.out.println("SavingAccount parameterized constructor");
	}

	public double getInterestRate() {
		return interestRate;
	}

	public void setInterestRate(double interestRate) {
		this.interestRate = interestRate;
	}
	
	
}
