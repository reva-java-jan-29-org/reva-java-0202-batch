package com.training.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.training.entities.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>{
	
	//find a product by given sku
	Optional<Product> findBySku(String sku);
	
}
