# Module 07 — Exception Handling

## The Problem with Default Spring Boot Error Handling

Without a custom exception handler, Spring Boot returns its own error response:

```json
{
  "timestamp": "2026-03-04T10:30:00.000+00:00",
  "status": 404,
  "error": "Not Found",
  "path": "/api/products/99"
}
```

Problems:
1. The `message` field is empty — the client doesn't know WHY it's 404
2. Format is inconsistent (timestamp format differs from your app's format)
3. For validation errors, Spring Boot returns a verbose `errors` array that's hard to consume
4. No single standard — every error looks different

**Goal:** Every error response from our API looks exactly the same, predictable structure:

```json
{
  "timestamp": "2026-03-04 10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found with id: 99",
  "path": "/api/products/99"
}
```

---

## Custom Exception Classes

### Why custom exceptions?

Instead of throwing generic `RuntimeException("message")`, custom exceptions:
1. Are self-documenting — the type name tells you what went wrong
2. Can carry additional context (e.g., resource name + ID)
3. Are caught by specific `@ExceptionHandler` methods → correct HTTP status

### ResourceNotFoundException (404)

```java
@ResponseStatus(HttpStatus.NOT_FOUND)  // ← backup if no ExceptionHandler catches it
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    // Convenience constructor
    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " not found with id: " + id);
    }
}
```

Usage:
```java
// In service:
Product product = productRepository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Product", id));
// → throws: "Product not found with id: 42"
```

### DuplicateResourceException (409)

```java
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
```

Usage:
```java
if (categoryRepository.existsByName(request.getName())) {
    throw new DuplicateResourceException(
        "Category with name '" + request.getName() + "' already exists");
}
```

---

## @RestControllerAdvice — Global Exception Handler

### @ExceptionHandler

`@ExceptionHandler` on a method means: "When this exception type is thrown anywhere in the app, call this method."

```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, ...) {
    // Return a clean 404 response
}
```

### @RestControllerAdvice

`@RestControllerAdvice` means: "Apply the @ExceptionHandler methods in this class to ALL @RestController classes."

Without `@RestControllerAdvice`, you'd need to put `@ExceptionHandler` methods in every single controller — duplication.

```java
@RestControllerAdvice    // = @ControllerAdvice + @ResponseBody
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(404)
            .error("Not Found")
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
```

### The Full Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handles throw new ResourceNotFoundException(...)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    // Handles throw new DuplicateResourceException(...)
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(
            DuplicateResourceException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.CONFLICT, request);
    }

    // Handles @Valid failures (Bean Validation)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<String> errors = ex.getBindingResult().getFieldErrors()
            .stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .toList();

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(400)
            .error("Bad Request")
            .message("Validation failed. Please check the request body.")
            .path(request.getRequestURI())
            .validationErrors(errors)
            .build();

        return ResponseEntity.badRequest().body(error);
    }

    // Catches everything else — fallback handler
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(
            Exception ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred.", request);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            Exception ex, HttpStatus status, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .build();
        return ResponseEntity.status(status).body(error);
    }
}
```

---

## Error Response Structure

```java
@Data
@Builder
public class ErrorResponse {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    private int status;              // 404, 400, 500
    private String error;            // "Not Found", "Bad Request"
    private String message;          // human-readable description
    private String path;             // which URL caused this

    // Only present for 400 validation errors
    private List<String> validationErrors;
}
```

---

## Exception Flow — Step by Step

```
Request: GET /api/products/999

ProductController.getProductById(999)
  └─ productService.findById(999)
       └─ productRepository.findById(999) → returns Optional.empty()
            └─ .orElseThrow(() -> new ResourceNotFoundException("Product", 999))
                 ← throws ResourceNotFoundException("Product not found with id: 999")

Spring propagates the exception UP the call stack
    ↓
GlobalExceptionHandler.handleNotFound() catches it
    ↓
Returns:
HTTP/1.1 404 Not Found
Content-Type: application/json

{
  "timestamp": "2026-03-04 10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found with id: 999",
  "path": "/api/products/999"
}
```

---

## Testing Exception Scenarios with curl

```bash
# 404 — Product not found
curl http://localhost:8080/api/products/999
# Response: 404 + {"message": "Product not found with id: 999"}

# 409 — Duplicate category name
curl -X POST http://localhost:8080/api/categories \
  -H "Content-Type: application/json" \
  -d '{"name": "Electronics"}'
# (first time: 201) (second time: 409 + {"message": "Category with name 'Electronics' already exists"})

# 400 — Validation failure
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name": "", "price": -5}'
# Response: 400 + {"validationErrors": ["name: Product name must not be blank", "price: Price must be greater than 0"]}

# 500 — (rare, would be caused by a DB being down or a coding bug)
```

---

## @ResponseStatus vs @RestControllerAdvice — Choosing the Right Approach

| Approach | How it works | When to use |
|----------|-------------|-------------|
| `@ResponseStatus` on exception class | Spring auto-sets the status if exception propagates to the servlet | Simple apps, quick prototypes |
| `@ExceptionHandler` in the controller | Handler method in the same controller class | When only one controller needs special handling |
| `@RestControllerAdvice` | Global handler for all controllers | **Recommended for production APIs** |

> In our project, we have **both**: `@ResponseStatus` on the exception class (as a fallback) AND `@RestControllerAdvice` that catches it first. The `@RestControllerAdvice` always wins.

---

## Exception Hierarchy Best Practices

```
RuntimeException (unchecked — Spring rolls back transaction by default)
    ├── ResourceNotFoundException   → 404
    ├── DuplicateResourceException  → 409
    ├── BadRequestException         → 400 (business rule violation)
    └── UnauthorizedException       → 401

Exception (checked — Spring does NOT roll back by default)
    ├── IOException
    └── SQLException
```

In REST APIs, almost all custom exceptions should extend `RuntimeException` so transactions roll back automatically.
