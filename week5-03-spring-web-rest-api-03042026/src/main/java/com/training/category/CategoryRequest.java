package com.training.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO (Data Transfer Object) for incoming Category creation/update requests.
 *
 * Why DTOs instead of using the Entity directly?
 * ─────────────────────────────────────────────────
 * 1. Security   — you control exactly what the client can send. The client can't
 *                 accidentally set internal fields like 'id' or 'products'.
 * 2. Validation — validation rules belong on the request model, not the entity.
 * 3. Flexibility — request shape can differ from DB shape.
 * 4. Decoupling  — API contract is separate from DB schema. You can change one
 *                  without breaking the other.
 *
 * @Valid on the controller method parameter triggers these validations.
 * If validation fails, Spring throws MethodArgumentNotValidException (400 Bad Request).
 */
@Data
public class CategoryRequest {

    @NotBlank(message = "Category name must not be blank")
    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
