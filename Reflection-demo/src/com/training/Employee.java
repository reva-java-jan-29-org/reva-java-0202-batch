package com.training;

public class Employee {
	
	private int id;
	private String name;
	private String city;
	public double salary;
	
	public Employee() {
		super();
	}

	public Employee(int id, String name, String city, double salary) {
		super();
		this.id = id;
		this.name = name;
		this.city = city;
		this.salary = salary;
	} 
	
	public void increaseSalary(double percentage) {
	
		if(percentage < 0)
			throw new IllegalArgumentException("Percentage cannot be zero or less");
	
		this.salary = this.salary + (this.salary * percentage / 100.00);
		System.out.println("Salary is increased by " + percentage + "%, new Salary :" + this.salary);
	}
	

}
