package com.training.category;

import com.training.exception.DuplicateResourceException;
import com.training.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service class for Category business logic.
 *
 * @Service — marks this as a Spring-managed service bean.
 *   Spring registers it in the application context.
 *   Can be injected into controllers via constructor injection.
 *
 * @RequiredArgsConstructor (Lombok) — generates a constructor for all 'final' fields.
 *   This enables constructor injection without writing boilerplate.
 *
 * Why a Service layer?
 * ───────────────────
 * 1. Separation of concerns: Controller handles HTTP, Service handles business logic.
 * 2. Reusability: Multiple controllers can use the same service.
 * 3. Testability: Services can be unit-tested without HTTP context (MockMvc not needed).
 * 4. Transaction management: @Transactional is cleanly placed at the service layer.
 *
 * @Transactional:
 *   - read-only = true on query methods → performance optimization (no dirty-checking).
 *   - read-only = false (default) on write methods → Spring manages commit/rollback.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // ─── READ operations ─────────────────────────────────────────────────────

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll()
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public CategoryResponse findById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        return CategoryResponse.from(category);
    }

    // ─── WRITE operations ────────────────────────────────────────────────────

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        // Business rule: category names must be unique
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException(
                    "Category with name '" + request.getName() + "' already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Category saved = categoryRepository.save(category);
        return CategoryResponse.from(saved);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        // Check name uniqueness only if the name has actually changed
        if (!existing.getName().equals(request.getName())
                && categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException(
                    "Category with name '" + request.getName() + "' already exists");
        }

        existing.setName(request.getName());
        existing.setDescription(request.getDescription());

        // No explicit save needed — the entity is managed and changes are flushed
        // automatically at the end of the transaction (@Transactional).
        // But calling save() is fine too and makes the intent explicit.
        Category updated = categoryRepository.save(existing);
        return CategoryResponse.from(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category", id);
        }
        categoryRepository.deleteById(id);
    }
}
