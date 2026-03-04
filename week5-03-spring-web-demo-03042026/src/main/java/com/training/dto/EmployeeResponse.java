package com.training.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {

	private Long empId;
	private String name;
	private String city;
	private String salary;
	
	public String convertDoubleToString(Double salary) {
		return "****";
	}
}
