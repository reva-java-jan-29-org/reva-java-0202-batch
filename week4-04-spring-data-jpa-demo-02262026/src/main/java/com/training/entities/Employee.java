package com.training.entities;

import java.time.LocalDate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;

@Entity
@Table(name="employees", schema = "empmgmtdb")
@Data
public class Employee {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)						//defines primary key generation strategy
	@Column(name="emp_id", nullable = false)
	private Long empId;
	
	private String name;
	private String city;
	private Double salary;
	
	@Enumerated(EnumType.STRING)
	private EmployeeType type;
	
	
	private LocalDate joiningDate;
	
	@Transient
	private int totalExperienceInYears;
	
	@OneToOne(cascade = CascadeType.ALL)
	private Profile profile;

	@Override
	public String toString() {
		return "\n Employee [empId=" + empId + ", name=" + name + ", city=" + city + ", salary=" + salary + ", type="
				+ type + ", joiningDate=" + joiningDate + ", totalExperienceInYears=" + totalExperienceInYears + "]";
	}
	
	
	
}
