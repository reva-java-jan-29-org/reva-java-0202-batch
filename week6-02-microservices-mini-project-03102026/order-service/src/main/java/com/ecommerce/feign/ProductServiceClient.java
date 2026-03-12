package com.ecommerce.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ecommerce.dto.ProductDto;

@FeignClient(name="product-service")
public interface ProductServiceClient {

	@GetMapping("/api/products/{id}")
    public ProductDto getProductById(@PathVariable Long id);
	
	@PutMapping("/api/products/{id}/reduce-stock")
    public ProductDto reduceStock(@PathVariable Long id, @RequestParam int quantity); 
}
