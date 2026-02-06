
public class Employee {
	
	private int empId;
	private String name;
	private double salary;
	
	public Employee() {
		System.out.println("Employee object is created with default values");
	}
	
	public Employee(int empId) {
		this.empId = empId;
		System.out.println("Employee object is created, empId is initialized, but name and salary is not initialized");
	}
	
	public Employee(int id, String name, double salary) {
		this.empId = id;
		this.name = name;
		this.salary = salary;
		System.out.println("Employee object is created with the provided 3 parameters");
	}
	
	public int getEmpId() {
		return empId;
	}
	public void setEmpId(int empId) {
		this.empId = empId;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public double getSalary() {
		return salary;
	}
	public void setSalary(double salary) {
		this.salary = salary;
	} 
}
