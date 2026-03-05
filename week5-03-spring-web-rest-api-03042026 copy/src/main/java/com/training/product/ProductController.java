package com.training.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST Controller for Product CRUD operations.
 *
 * This controller demonstrates:
 * - @PathVariable         — extract values from the URL path
 * - @RequestParam         — extract query parameters from the URL (?name=laptop)
 * - @RequestBody          — deserialize JSON request body into a DTO
 * - ResponseEntity        — full control over HTTP status + body + headers
 * - HTTP verbs            — GET, POST, PUT, PATCH, DELETE
 * - Filtering via query params (search, category, price range)
 *
 * API Design (follows REST conventions):
 *   GET    /api/products              → list all
 *   GET    /api/products/{id}         → get one
 *   GET    /api/products?name=laptop  → search by name
 *   GET    /api/products?categoryId=2 → filter by category
 *   GET    /api/products?minPrice=100&maxPrice=500 → filter by price
 *   POST   /api/products              → create new
 *   PUT    /api/products/{id}         → full update
 *   PATCH  /api/products/{id}/stock   → partial update (stock only)
 *   DELETE /api/products/{id}         → delete
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * GET /api/products
     * GET /api/products?name=laptop
     * GET /api/products?categoryId=2
     * GET /api/products?minPrice=100&maxPrice=500
     *
     * @RequestParam(required = false) — the parameter is optional.
     *   If not provided, the value is null.
     *
     * We check which params were provided and call the appropriate service method.
     * This is a common pattern for flexible list endpoints.
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {

        if (name != null) {
            return ResponseEntity.ok(productService.search(name));
        }
        if (categoryId != null) {
            return ResponseEntity.ok(productService.findByCategory(categoryId));
        }
        if (minPrice != null && maxPrice != null) {
            return ResponseEntity.ok(productService.findByPriceRange(minPrice, maxPrice));
        }

        return ResponseEntity.ok(productService.findAll());
    }

    /**
     * GET /api/products/{id}
     *
     * @PathVariable Long id — Spring auto-converts the String "{id}" from URL to Long.
     * If id is not a valid number → Spring returns 400 Bad Request automatically.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    /**
     * POST /api/products
     * Creates a new product. Returns 201 Created.
     *
     * Request body example:
     * {
     *   "name": "MacBook Pro",
     *   "description": "Apple M3 chip, 16GB RAM",
     *   "price": 1999.99,
     *   "stockQuantity": 50,
     *   "categoryId": 1
     * }
     */
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/products/{id}
     * Full update — all fields must be provided.
     *
     * PUT is idempotent: calling it multiple times with the same body
     * always produces the same result. The resource ends up in the same state.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    /**
     * PATCH /api/products/{id}/stock
     * Partial update — only updates the stock quantity.
     *
     * PATCH vs PUT:
     * - PUT replaces the entire resource.
     * - PATCH applies a partial modification.
     *
     * Here we use a simple @RequestParam for the new stock value.
     * Another approach is to send a JSON body: { "stockQuantity": 100 }
     *
     * @RequestParam(value = "quantity") — query parameter: PATCH /api/products/5/stock?quantity=100
     */
    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductResponse> updateStock(
            @PathVariable Long id,
            @RequestParam @PositiveOrZero int quantity) {
        return ResponseEntity.ok(productService.updateStock(id, quantity));
    }

    /**
     * DELETE /api/products/{id}
     * Returns 204 No Content on success.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
