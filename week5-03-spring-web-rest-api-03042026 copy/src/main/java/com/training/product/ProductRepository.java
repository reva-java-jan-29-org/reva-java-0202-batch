package com.training.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Spring Data JPA repository for Product.
 *
 * Demonstrates different ways to write queries in Spring Data JPA:
 *
 * 1. Derived Query Methods — Spring parses method names and generates SQL automatically.
 *    findByCategoryId() → SELECT * FROM products WHERE category_id = ?
 *
 * 2. JPQL (Java Persistence Query Language) — object-based query, not SQL.
 *    Refers to entity class names and field names, not table/column names.
 *
 * 3. Native SQL — raw SQL with @Query(nativeQuery = true).
 *    Use only when JPQL can't express the query.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // ─── Derived Query Methods ───────────────────────────────────────────────

    // Find all products in a specific category
    List<Product> findByCategoryId(Long categoryId);

    // Find products whose name contains the search term (case-insensitive)
    List<Product> findByNameContainingIgnoreCase(String name);

    // Find products within a price range
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    // Find products that are in stock (stockQuantity > 0)
    List<Product> findByStockQuantityGreaterThan(int quantity);

    // ─── JPQL Query ──────────────────────────────────────────────────────────

    /**
     * JOIN FETCH solves the N+1 problem.
     * Without it: loading 10 products makes 1 + 10 queries (1 for products, 10 for category each).
     * With it: single JOIN query loads everything at once.
     */
    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.category.id = :categoryId")
    List<Product> findByCategoryIdWithCategory(@Param("categoryId") Long categoryId);

    // ─── Native SQL Query ─────────────────────────────────────────────────────

    @Query(value = "SELECT * FROM products WHERE price < :maxPrice ORDER BY price ASC", nativeQuery = true)
    List<Product> findCheaperThan(@Param("maxPrice") BigDecimal maxPrice);
}
