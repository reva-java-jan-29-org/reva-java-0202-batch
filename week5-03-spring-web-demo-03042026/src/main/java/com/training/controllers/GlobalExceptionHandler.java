package com.training.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.training.exceptions.CustomFieldError;
import com.training.exceptions.ErrorResponse;
import com.training.exceptions.ErrorResponseDTO;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<?> handleEntityNotFoundException(EntityNotFoundException ex, HttpServletRequest request) {
		System.out.println("handleEntityNotFoundException is called.....");
		ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage(),
				request.getRequestURI(), LocalDate.now());

		return new ResponseEntity(errorResponse, HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {

		List<CustomFieldError> customFieldErrors = ex.getFieldErrors().stream()
				.map(fieldError -> new CustomFieldError(fieldError.getField(), fieldError.getDefaultMessage()))
				.toList();

		ErrorResponseDTO errorResponse = new ErrorResponseDTO();
		errorResponse.setErrors(customFieldErrors);
		errorResponse.setStatusCode(String.valueOf(HttpStatus.BAD_REQUEST));

		return new ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST);
	}
}
