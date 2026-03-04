package com.training.product;

import com.training.category.Category;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * JPA Entity representing a product in the catalog.
 *
 * Product is the "many" side of the Many-to-One / One-to-Many relationship.
 * Many Products → One Category.
 *
 * @Table(name = "products") — maps to the "products" table in MySQL.
 *
 * NOTE: We use @Getter @Setter @NoArgsConstructor instead of @Data to avoid
 * StackOverflow in toString/hashCode caused by the bidirectional relationship
 * with Category.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    // BigDecimal is the correct type for monetary values (no floating-point errors)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stockQuantity;

    /**
     * Owning side of the Many-to-One relationship.
     * @JoinColumn(name = "category_id") creates a FK column "category_id" in the products table.
     * fetch = LAZY — don't load Category automatically (avoids N+1 problem).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Custom toString to avoid StackOverflow (don't include category's product list)
    @Override
    public String toString() {
        return "Product{id=" + id + ", name='" + name + "', price=" + price + "}";
    }
}
