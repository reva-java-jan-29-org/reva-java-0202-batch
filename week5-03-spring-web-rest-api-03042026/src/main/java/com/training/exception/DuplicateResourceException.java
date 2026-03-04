package com.training.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception thrown when attempting to create a resource that already exists.
 * Example: creating a Category with a name that is already taken.
 *
 * Maps to HTTP 409 Conflict — the request conflicts with the current state of the server.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
