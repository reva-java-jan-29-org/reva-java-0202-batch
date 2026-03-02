package com.training.entities;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.Data;

@Entity
@Data
public class Department {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long deptId;
	private String departmentName;
	
	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(
			name = "fk_deptId", unique = true, nullable = false
	)
	List<Employee> employees = new ArrayList();
}
