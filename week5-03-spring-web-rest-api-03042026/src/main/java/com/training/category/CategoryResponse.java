package com.training.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for outgoing Category responses.
 *
 * Why a separate response DTO?
 * ────────────────────────────
 * 1. We choose exactly what fields to expose — we expose 'id' in the response
 *    but don't accept it from the client in CategoryRequest.
 * 2. We can add computed fields (e.g., productCount) without modifying the entity.
 * 3. We avoid accidentally serializing lazy-loaded collections and causing
 *    LazyInitializationException or infinite loops.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponse {

    private Long id;
    private String name;
    private String description;

    // How many products are in this category (computed field — not in the DB)
    private int productCount;

    // ─── Static factory method — converts Entity → DTO ───────────────────────
    public static CategoryResponse from(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .productCount(category.getProducts().size())
                .build();
    }
}
