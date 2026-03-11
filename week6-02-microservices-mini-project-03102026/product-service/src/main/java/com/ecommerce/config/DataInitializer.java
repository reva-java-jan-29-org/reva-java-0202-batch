package com.ecommerce.config;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.ecommerce.entity.Product;
import com.ecommerce.repository.ProductRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private ProductRepository productRepository;

    @Override
    public void run(String... args) {
        if (productRepository.count() == 0) {
            productRepository.save(createProduct("Laptop Pro 15", "High-performance laptop with 16GB RAM and 512GB SSD", new BigDecimal("1299.99"), 50, "Electronics", null));
            productRepository.save(createProduct("Wireless Headphones", "Noise-cancelling Bluetooth headphones", new BigDecimal("249.99"), 100, "Electronics", null));
            productRepository.save(createProduct("Running Shoes", "Lightweight running shoes for all terrains", new BigDecimal("89.99"), 200, "Footwear", null));
            productRepository.save(createProduct("Coffee Maker", "Automatic drip coffee maker with programmable timer", new BigDecimal("49.99"), 75, "Kitchen", null));
            productRepository.save(createProduct("Yoga Mat", "Non-slip eco-friendly yoga mat", new BigDecimal("29.99"), 150, "Sports", null));
            productRepository.save(createProduct("Smartphone X", "Latest smartphone with 5G and 128GB storage", new BigDecimal("799.99"), 30, "Electronics", null));
            productRepository.save(createProduct("Backpack Pro", "Waterproof laptop backpack with USB charging port", new BigDecimal("59.99"), 80, "Bags", null));
            productRepository.save(createProduct("Smart Watch", "Fitness tracker with heart rate monitor", new BigDecimal("199.99"), 60, "Electronics", null));
        }
    }

    private Product createProduct(String name, String description, BigDecimal price, int stock, String category, String imageUrl) {
        Product p = new Product();
        p.setName(name);
        p.setDescription(description);
        p.setPrice(price);
        p.setStock(stock);
        p.setCategory(category);
        p.setImageUrl(imageUrl);
        return p;
    }
}

