package com.training.model;

import java.math.BigDecimal;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
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
public class Employee implements InitializingBean {
	
	@Value("10")
	private  int id;
	
	@Value("Samiksha")
	private  String name;
	
	@Value("Pune")
	private String city;
	
	@Value("-100000.00")
	private double salary;
	
	@Value("Persistent")
	private String projectName;
	
	
	
	public Employee() {
		super();
		System.out.println("1. INSTATIATION: Employee object is created ");
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
	
	@PostConstruct
	public void empValidation() {
		System.out.println("5.a: PostConstruct - I am validating the values initialized to this object ");
		if(this.salary<=0)
			this.salary=100.00;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// TODO Auto-generated method stub
		System.out.println("5.b: InitializingBean.afterPropertiesSet() - perform some validation ");

	}
	
	public void customInitMethod() {
		System.out.println("5.c: customInitMethod - this can also be used to perform validation");
	}
	
	
}
