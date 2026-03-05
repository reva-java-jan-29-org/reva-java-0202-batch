package com.training.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized error response body returned by GlobalExceptionHandler.
 *
 * Instead of Spring's default error JSON (which includes internal stack traces),
 * we return a clean, predictable structure that API clients can rely on.
 *
 * Example JSON response for a 404:
 * {
 *   "timestamp": "2026-03-04T10:30:00",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Product not found with id: 99",
 *   "path": "/api/products/99"
 * }
 *
 * Example JSON for a 400 validation error:
 * {
 *   "timestamp": "...",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Validation failed",
 *   "validationErrors": ["name: Product name must not be blank", "price: Price is required"]
 * }
 */
@Data
@Builder
public class ErrorResponse {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    private int status;
    private String error;
    private String message;
    private String path;

    // Only present for validation errors (400 Bad Request)
    private List<String> validationErrors;
}
