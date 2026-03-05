package com.training.category;

import com.training.product.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Entity representing a product category.
 *
 * A Category is the "one" side of a One-to-Many relationship with Product.
 * One Category → Many Products.
 *
 * @Table(name = "categories") — maps to the "categories" table in MySQL.
 */
@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // NOT NULL + unique constraint at DB level
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /**
     * Inverse side of the One-to-Many relationship.
     * mappedBy = "category" → the "category" field in Product is the owning side.
     *
     * cascade = CascadeType.ALL → saving/deleting a category cascades to its products.
     * orphanRemoval = true     → if a product is removed from this list it gets deleted.
     *
     * NOTE: We exclude this from @Data's toString/equals to avoid StackOverflow.
     */
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Product> products = new ArrayList<>();
}
