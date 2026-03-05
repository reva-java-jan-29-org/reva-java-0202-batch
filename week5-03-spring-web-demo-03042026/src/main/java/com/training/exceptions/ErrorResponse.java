package com.training.exceptions;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponse {

	private int status;
	private String message;
	private String path;
	private LocalDate timeStamp;
	
}
