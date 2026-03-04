package com.training.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception thrown when a requested resource (Product, Category) is not found.
 *
 * @ResponseStatus(HttpStatus.NOT_FOUND) — if this exception is NOT caught by
 * GlobalExceptionHandler and propagates to Spring, it automatically responds with 404.
 * However, since we have a GlobalExceptionHandler, that will catch it first.
 *
 * Extending RuntimeException means:
 * - It's unchecked — no need for try-catch or 'throws' declaration
 * - Spring can propagate it naturally up through the call stack
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    // Convenience constructor: "Product not found with id: 42"
    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " not found with id: " + id);
    }
}
