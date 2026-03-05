package com.training.exceptions;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CustomFieldError {

	private String fieldName;
	private String message;
}
