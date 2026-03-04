package com.training.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for outgoing Product responses.
 *
 * Notice:
 * - We include categoryId and categoryName (flattened from the nested Category object).
 *   This is intentional — we don't want to serialize the entire Category entity
 *   (that would include a list of all products → infinite loop risk).
 * - The client gets exactly the fields they need, nothing more.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;

    // Flattened category info — avoids nested objects in JSON
    private Long categoryId;
    private String categoryName;

    // ─── Static factory method — converts Entity → DTO ───────────────────────
    public static ProductResponse from(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .build();
    }
}
