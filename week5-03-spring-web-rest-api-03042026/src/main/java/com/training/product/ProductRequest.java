package com.training.product;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for incoming Product creation/update requests.
 *
 * Bean Validation Annotations used here:
 * ─────────────────────────────────────────
 * @NotBlank    — string is not null, not empty, not whitespace-only
 * @NotNull     — value is not null (use for non-string types)
 * @Size        — string length between min/max
 * @Min / @Max  — numeric value constraints
 * @DecimalMin  — BigDecimal minimum value
 * @Positive    — number must be > 0
 * @PositiveOrZero — number must be >= 0
 */
@Data
public class ProductRequest {

    @NotBlank(message = "Product name must not be blank")
    @Size(min = 2, max = 200, message = "Product name must be between 2 and 200 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

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
