package com.training.product;

import com.training.category.Category;
import com.training.category.CategoryRepository;
import com.training.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service class for Product business logic.
 *
 * This service demonstrates:
 * - Cross-entity interactions (Product needs Category from CategoryRepository)
 * - How services coordinate multiple repositories
 * - Different query scenarios (filter by category, search, price range)
 * - PATCH semantics (partial update — only update fields that were provided)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // ─── READ operations ─────────────────────────────────────────────────────

    public List<ProductResponse> findAll() {
        return productRepository.findAll()
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    public ProductResponse findById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return ProductResponse.from(product);
    }

    public List<ProductResponse> findByCategory(Long categoryId) {
        // Validate the category exists first
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category", categoryId);
        }
        return productRepository.findByCategoryIdWithCategory(categoryId)
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    public List<ProductResponse> search(String name) {
        return productRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    public List<ProductResponse> findByPriceRange(BigDecimal min, BigDecimal max) {
        return productRepository.findByPriceBetween(min, max)
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    // ─── WRITE operations ────────────────────────────────────────────────────

    @Transactional
    public ProductResponse create(ProductRequest request) {
        // Resolve the Category entity — throws 404 if not found
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .category(category)
                .build();

        Product saved = productRepository.save(product);
        return ProductResponse.from(saved);
    }

    /**
     * Full update (PUT semantics) — replaces all fields with the request values.
     * All fields in ProductRequest are required for a PUT.
     */
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setPrice(request.getPrice());
        existing.setStockQuantity(request.getStockQuantity());
        existing.setCategory(category);

        Product updated = productRepository.save(existing);
        return ProductResponse.from(updated);
    }

    /**
     * Partial update (PATCH semantics) — only updates fields that are non-null in the request.
     * Useful for updating just the price or just the stock quantity without resending everything.
     *
     * This method uses a simple map-style request (ProductRequest) but only applies non-null fields.
     * In production you'd use a separate PatchRequest DTO or a JSON Merge Patch library.
     */
    @Transactional
    public ProductResponse updateStock(Long id, int newStockQuantity) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        existing.setStockQuantity(newStockQuantity);
        Product updated = productRepository.save(existing);
        return ProductResponse.from(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", id);
        }
        productRepository.deleteById(id);
    }
}
