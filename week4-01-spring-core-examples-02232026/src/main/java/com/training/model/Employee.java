package com.training.model;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

//@Getter
//@Setter
//@ToString
//@AllArgsConstructor
//@RequiredArgsConstructor
//@Builder
@Data
@Component
@Primary
public class Employee {
	
	private  int id;
	private  String name;
	private String city;
	private double salary;
	private String projectName;
	
	
	
	public Employee() {
		super();
		System.out.println("Employee object is created ");
	}



	public Employee(int id, String name, String city, double salary, String projectName) {
		super();
		this.id = id;
		this.name = name;
		this.city = city;
		this.salary = salary;
		this.projectName = projectName;
		System.out.println("Employee object is created " + name);
	}
	
	
	
	
}
