
public class BankAccount {
	
	private int customerId;
	private String customerName;
	
	public BankAccount() {
		
		System.out.println("BankAccount no-arg constructor");
	}

	public BankAccount(int customerId, String customerName) {
		super();
		this.customerId = customerId;
		this.customerName = customerName;
		
		System.out.println("BankAccount parameterized constructor");
	}

	public int getCustomerId() {
		return customerId;
	}

	public void setCustomerId(int customerId) {
		this.customerId = customerId;
	}

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}
}
