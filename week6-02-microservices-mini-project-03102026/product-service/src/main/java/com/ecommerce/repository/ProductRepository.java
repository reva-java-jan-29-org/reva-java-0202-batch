package com.ecommerce.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ecommerce.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
	
		@Query("SELECT p FROM Product p WHERE " +
	           "LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
	           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
	           "LOWER(p.category) LIKE LOWER(CONCAT('%', :query, '%'))")
	    List<Product> searchProducts(@Param("query") String query);

	    List<Product> findByCategory(String category);

	    List<Product> findByStockGreaterThan(int stock);

}
