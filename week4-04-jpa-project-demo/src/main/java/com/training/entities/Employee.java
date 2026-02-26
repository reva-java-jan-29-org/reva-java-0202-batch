package com.training.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Employee {
	
	@Id
	private Long id;
	
	private String fullName;
	
	private String city;
	
	private Double salary;

	public Employee() {
		super();
	}

	public Employee(String fullName, String city, Double salary) {
		super();
		this.fullName = fullName;
		this.city = city;
		this.salary = salary;
	}

	public Employee(Long id, String fullName, String city, Double salary) {
		super();
		this.id = id;
		this.fullName = fullName;
		this.city = city;
		this.salary = salary;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public Double getSalary() {
		return salary;
	}

	public void setSalary(Double salary) {
		this.salary = salary;
	}

	@Override
	public String toString() {
		return "\n Employee [id=" + id + ", fullName=" + fullName + ", city=" + city + ", salary=" + salary + "]";
	}
	
	
	
}
