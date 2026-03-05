package com.training.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmployeeRequest {
	
	@NotBlank
	@Size(min = 5, max = 255, message = "Name must be min 5 and max 255 characters")
	private String name;
	
	@NotBlank(message = "City cannot be blank")
	private String city;
	
	@Positive(message = "Salary cannot be negative")
	private Double salary;

}
