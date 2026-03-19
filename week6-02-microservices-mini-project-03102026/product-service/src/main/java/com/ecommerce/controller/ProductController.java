package com.ecommerce.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.dto.ProductDto;
import com.ecommerce.service.ProductService;

import jakarta.validation.Valid;

/**
 * Public (GET) endpoints are accessible by everyone.
 * Write endpoints (POST / PUT / DELETE) require ROLE_ADMIN via X-User-Role header.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private Environment environment;

    // ── Public read endpoints ─────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        System.out.println("Product Service : Port No - " + environment.getProperty("server.port"));
        try {
            return ResponseEntity.ok(productService.getProductById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductDto>> searchProducts(@RequestParam String q) {
        return ResponseEntity.ok(productService.searchProducts(q));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductDto>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(productService.getProductsByCategory(category));
    }

    // ── Admin-only write endpoints ────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> createProduct(
            @RequestHeader(value = "X-User-Role", defaultValue = "") String role,
            @Valid @RequestBody ProductDto dto) {
        if (!isAdmin(role)) return forbidden();
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(
            @RequestHeader(value = "X-User-Role", defaultValue = "") String role,
            @PathVariable Long id,
            @Valid @RequestBody ProductDto dto) {
        if (!isAdmin(role)) return forbidden();
        try {
            return ResponseEntity.ok(productService.updateProduct(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(
            @RequestHeader(value = "X-User-Role", defaultValue = "") String role,
            @PathVariable Long id) {
        if (!isAdmin(role)) return forbidden();
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // ── Internal endpoint (called by order-service via Feign — no role check needed) ──

    @PutMapping("/{id}/reduce-stock")
    public ResponseEntity<?> reduceStock(@PathVariable Long id, @RequestParam int quantity) {
        try {
            return ResponseEntity.ok(productService.reduceStock(id, quantity));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private boolean isAdmin(String role) {
        return "ROLE_ADMIN".equals(role);
    }

    private ResponseEntity<Map<String, String>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Admin access required"));
    }
}
