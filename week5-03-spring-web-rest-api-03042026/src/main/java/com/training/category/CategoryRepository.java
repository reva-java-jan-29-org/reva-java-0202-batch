package com.training.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for Category.
 *
 * JpaRepository<Category, Long> provides out-of-the-box CRUD methods:
 *   - save(category)
 *   - findById(id)
 *   - findAll()
 *   - deleteById(id)
 *   - existsById(id)
 *   - count()
 *   ... and more
 *
 * Custom derived query — Spring generates the SQL from the method name:
 *   findByName("Electronics") → SELECT * FROM categories WHERE name = 'Electronics'
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    boolean existsByName(String name);
}
