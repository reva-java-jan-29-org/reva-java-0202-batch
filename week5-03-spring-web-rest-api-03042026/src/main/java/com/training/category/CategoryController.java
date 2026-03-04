package com.training.category;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Category CRUD operations.
 *
 * @RestController = @Controller + @ResponseBody
 *   Every method's return value is automatically serialized to JSON and written
 *   to the HTTP response body (no need for @ResponseBody on each method).
 *
 * @RequestMapping("/api/categories") — all endpoints in this class are prefixed with this path.
 *   Full URLs:
 *     GET  /api/categories
 *     POST /api/categories
 *     GET  /api/categories/{id}
 *     PUT  /api/categories/{id}
 *     DELETE /api/categories/{id}
 *
 * @RequiredArgsConstructor — constructor injection of CategoryService.
 *   Preferred over @Autowired field injection because:
 *   1. Easier to unit test (can pass a mock in the constructor)
 *   2. Makes dependencies explicit
 *   3. Allows fields to be 'final' (immutable)
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * GET /api/categories
     * Returns all categories.
     * HTTP 200 OK — successful response with body.
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        List<CategoryResponse> categories = categoryService.findAll();
        return ResponseEntity.ok(categories);
    }

    /**
     * GET /api/categories/{id}
     * Returns a single category by ID.
     *
     * @PathVariable — binds the {id} segment of the URL to the 'id' parameter.
     * If category not found, GlobalExceptionHandler returns 404.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        CategoryResponse category = categoryService.findById(id);
        return ResponseEntity.ok(category);
    }

    /**
     * POST /api/categories
     * Creates a new category.
     *
     * @RequestBody — Spring deserializes the incoming JSON request body into CategoryRequest.
     * @Valid — triggers Bean Validation on the CategoryRequest fields.
     *         If validation fails → MethodArgumentNotValidException → GlobalExceptionHandler returns 400.
     *
     * Returns HTTP 201 Created (not 200 OK) — the standard status for successful resource creation.
     * ResponseEntity.status(HttpStatus.CREATED) lets us explicitly set the status code.
     */
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse created = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/categories/{id}
     * Full update — replaces the entire category with the request body.
     * Returns 200 OK with the updated category.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse updated = categoryService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/categories/{id}
     * Deletes a category. Returns HTTP 204 No Content (success, but no body).
     *
     * Why 204 instead of 200?
     * 204 = "I did what you asked, there's nothing to return."
     * 200 = "I did what you asked, here's the result."
     * DELETE has nothing to return → 204 is the standard.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
