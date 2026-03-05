package com.training.exceptions;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDTO {

	private List<CustomFieldError> errors;
	private String statusCode;
	
	
}
