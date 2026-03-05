# Module 08 — Bean Validation

## What is Bean Validation?

Bean Validation (JSR-380) is a Java standard for declaring validation rules **as annotations** directly on your Java objects.

`spring-boot-starter-validation` bundles **Hibernate Validator** — the reference implementation.

Without Bean Validation, you'd write manual checks:
```java
// ❌ Manual validation — verbose and error-prone
if (request.getName() == null || request.getName().isBlank()) {
    return ResponseEntity.badRequest().body("Name is required");
}
if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
    return ResponseEntity.badRequest().body("Price must be positive");
}
```

With Bean Validation:
```java
// ✅ Annotation-based — clean and declarative
@NotBlank(message = "Name is required")
private String name;

@DecimalMin(value = "0.01", message = "Price must be positive")
private BigDecimal price;
```

---

## Enabling Validation in Controllers

Add `@Valid` before `@RequestBody` to trigger validation:

```java
@PostMapping
public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
    // If validation fails, MethodArgumentNotValidException is thrown BEFORE this line.
    // GlobalExceptionHandler handles it → 400 Bad Request + validation errors list.
    return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
}
```

If `@Valid` is absent, validation annotations on `ProductRequest` are completely ignored.

---

## Common Validation Annotations

### String Constraints

```java
@NotNull(message = "Name cannot be null")      // value != null
@NotEmpty(message = "Name cannot be empty")    // value != null AND length > 0
@NotBlank(message = "Name cannot be blank")    // value != null AND trimmed length > 0
// ↑ For Strings, use @NotBlank (most strict — handles whitespace-only strings)

@Size(min = 2, max = 100, message = "Name must be 2–100 characters")
@Length(min = 2, max = 100)  // Hibernate-specific, same effect

@Pattern(regexp = "^[A-Za-z0-9 ]+$", message = "Only alphanumeric characters allowed")

@Email(message = "Invalid email format")

@URL(message = "Invalid URL format")  // Hibernate-specific
```

### Numeric Constraints

```java
@Min(value = 1, message = "Must be at least 1")
@Max(value = 1000, message = "Must be at most 1000")

@DecimalMin(value = "0.01", message = "Must be greater than 0")
@DecimalMax(value = "9999.99", message = "Must be at most 9999.99")

@Positive(message = "Must be positive (> 0)")
@PositiveOrZero(message = "Must be zero or positive (>= 0)")
@Negative(message = "Must be negative (< 0)")
@NegativeOrZero(message = "Must be zero or negative (<= 0)")

@Digits(integer = 8, fraction = 2, message = "At most 8 integer digits and 2 decimal places")
```

### Date/Time Constraints

```java
@Past(message = "Date must be in the past")
@PastOrPresent(message = "Date must be in the past or today")
@Future(message = "Date must be in the future")
@FutureOrPresent(message = "Date must be in the future or today")
```

### Boolean

```java
@AssertTrue(message = "Must be true")   // value == true
@AssertFalse(message = "Must be false") // value == false
```

---

## ProductRequest — Full Validation Example

```java
@Data
public class ProductRequest {

    @NotBlank(message = "Product name must not be blank")
    @Size(min = 2, max = 200, message = "Product name must be between 2 and 200 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;    // ← optional field, no @NotBlank

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Stock quantity is required")
    @PositiveOrZero(message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @NotNull(message = "Category ID is required")
    @Positive(message = "Category ID must be a positive number")
    private Long categoryId;
}
```

When the client sends:
```json
{ "name": "", "price": -5, "stockQuantity": -1, "categoryId": -3 }
```

The validation fails and `GlobalExceptionHandler` returns:
```json
{
  "timestamp": "2026-03-04 10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed. Please check the request body.",
  "path": "/api/products",
  "validationErrors": [
    "name: Product name must not be blank",
    "price: Price must be greater than 0",
    "stockQuantity: Stock quantity cannot be negative",
    "categoryId: Category ID must be a positive number"
  ]
}
```

---

## @NotNull vs @NotBlank vs @NotEmpty

These three are often confused. Here's the definitive comparison:

| Annotation | Null? | Empty `""`? | Blank `"   "`? |
|-----------|-------|------------|--------------|
| `@NotNull` | ❌ fails | ✅ passes | ✅ passes |
| `@NotEmpty` | ❌ fails | ❌ fails | ✅ passes |
| `@NotBlank` | ❌ fails | ❌ fails | ❌ fails |

```
@NotNull:  null → ❌   ""  → ✅   "  " → ✅  "hello" → ✅
@NotEmpty: null → ❌   ""  → ❌   "  " → ✅  "hello" → ✅
@NotBlank: null → ❌   ""  → ❌   "  " → ❌  "hello" → ✅
```

**Rule of thumb:**
- For Strings → use `@NotBlank` (most strict, handles all bad inputs)
- For Numbers, Objects → use `@NotNull`
- `@NotEmpty` is useful for Lists: `@NotEmpty List<String> tags`

---

## Validating Nested Objects with @Valid

If your DTO has nested objects, you need `@Valid` on the nested field too:

```java
@Data
public class OrderRequest {

    @NotNull
    @Valid                              // ← triggers validation on AddressRequest too
    private AddressRequest shippingAddress;

    @NotEmpty
    @Valid                              // ← triggers validation on each item
    private List<OrderItemRequest> items;
}

@Data
public class AddressRequest {
    @NotBlank
    private String street;

    @NotBlank
    private String city;
}
```

Without `@Valid` on the nested field, the nested object's constraints are **not checked**.

---

## Validating @PathVariable and @RequestParam

For path variables and query params, add `@Validated` at the class level:

```java
@RestController
@RequestMapping("/api/products")
@Validated                              // ← enable parameter-level validation
public class ProductController {

    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductResponse> updateStock(
            @PathVariable @Positive Long id,                    // id must be > 0
            @RequestParam @PositiveOrZero int quantity) {       // quantity >= 0
        ...
    }
}
```

If these constraints fail, Spring throws `ConstraintViolationException` (not `MethodArgumentNotValidException`). Add a handler for it in GlobalExceptionHandler if needed:

```java
@ExceptionHandler(ConstraintViolationException.class)
public ResponseEntity<ErrorResponse> handleConstraintViolation(
        ConstraintViolationException ex, HttpServletRequest request) {

    List<String> errors = ex.getConstraintViolations()
        .stream()
        .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
        .toList();

    ErrorResponse error = ErrorResponse.builder()
        .status(400)
        .error("Bad Request")
        .message("Constraint violation")
        .validationErrors(errors)
        .build();

    return ResponseEntity.badRequest().body(error);
}
```

---

## Custom Validation Annotations

For complex business rules not covered by built-in annotations:

```java
// Step 1: Create the annotation
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidSkuValidator.class)
public @interface ValidSku {
    String message() default "SKU must start with 'SKU-' followed by digits";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// Step 2: Create the validator
public class ValidSkuValidator implements ConstraintValidator<ValidSku, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;  // Let @NotNull handle null checks
        return value.matches("SKU-\\d+");
    }
}

// Step 3: Use it
@ValidSku
private String sku;
```

---

## Validation Groups (Advanced)

Sometimes you need different validation rules for create vs update:

```java
// Define marker interfaces for groups
public interface OnCreate {}
public interface OnUpdate {}

@Data
public class ProductRequest {

    @Null(groups = OnCreate.class, message = "ID must not be set on create")
    @NotNull(groups = OnUpdate.class, message = "ID is required for update")
    private Long id;

    @NotBlank(groups = {OnCreate.class, OnUpdate.class})
    private String name;
}

// In controller, specify which group to use:
@PostMapping
public ResponseEntity<?> create(@Validated(OnCreate.class) @RequestBody ProductRequest req) { ... }

@PutMapping("/{id}")
public ResponseEntity<?> update(@Validated(OnUpdate.class) @RequestBody ProductRequest req) { ... }
```
