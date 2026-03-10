package com.training.entities;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

	@Id
    private Long productId;
    
	private String sku;
    private String name;
    private String category;
    private BigDecimal price;
    private boolean active;
    
    
    
}
