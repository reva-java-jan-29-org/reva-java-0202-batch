# Module 6: Product Service

## Overview

In this module we build the **Product Service** — the catalogue backbone of our e-commerce platform. Products can be browsed by anyone (no authentication required), but operations like reducing stock are called internally by the Order Service via OpenFeign. By the end of this module you will have a fully working product catalogue with real data, accessible through the API Gateway.

**What you will build:**
- `Product` entity mapped to a MySQL table
- `ProductRepository` with custom JPQL search and derived query methods
- `ProductDto` for the API contract
- `ProductService` with full CRUD + stock management
- `ProductController` exposing REST endpoints under `/api/products`
- `DataInitializer` to seed 8 sample products on first startup
- `SecurityConfig` that permits all requests (auth is handled by the gateway)
- `application.yml` wired to the Config Server and Eureka

**Validation checkpoint:** After completing this module, `GET http://localhost:8080/api/products` should return a JSON array of 8 products.

---

## 6.1 Where Product Service Fits

```
Browser / cURL
      │
      ▼
  API Gateway :8080
      │  routes /api/products/** → lb://product-service
      ▼
 Product Service :8082
      │  reads config from
      ▼
 Config Server :8888
      │  registers/discovers via
      ▼
 Eureka Server :8761
      │  persists to
      ▼
   product_db (MySQL)
```

The gateway marks all `GET /api/products/**` requests as **public** — no JWT needed. Only the `PUT /api/products/{id}/reduce-stock` endpoint is called by Order Service (service-to-service, also bypasses the gateway's auth because it arrives from inside the cluster).

---

## 6.2 Maven Dependencies — `product-service/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.ecommerce</groupId>
        <artifactId>microservices-mini-project</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>product-service</artifactId>
    <name>Product Service</name>

    <dependencies>
        <!-- Web layer -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- JPA + MySQL -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Security (needed to configure permitAll) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- Bean Validation (@NotBlank, @DecimalMin) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Health endpoints -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Eureka client -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>

        <!-- Config Server client -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-config</artifactId>
        </dependency>
    </dependencies>
</project>
```

**Key observations:**
- `spring-boot-starter-security` is included so we can define a `SecurityFilterChain` — without it, Spring Boot would auto-block all requests
- `spring-cloud-starter-config` enables the Config Server import in `application.yml`
- `spring-cloud-starter-netflix-eureka-client` enables service registration and discovery

---

## 6.3 Configuration — `application.yml`

```yaml
# product-service/src/main/resources/application.yml

server:
  port: 8082

spring:
  application:
    name: product-service           # Eureka registration name; must match config-repo file name
  config:
    import: "optional:configserver:http://localhost:8888"

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true

management:
  endpoints:
    web:
      exposure:
        include: "*"
```

### Why `optional:configserver:`?

The `optional:` prefix means the application will still start even if the Config Server is unreachable. Without it, a missing Config Server would cause startup failure — useful in local dev but worth understanding.

### The Config Server provides the remaining properties

The Config Server serves `product-service.properties` from the config repo, which contains:

```properties
# config-repo/product-service.properties
spring.datasource.url=jdbc:mysql://localhost:3306/product_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=Root123
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

Notice there is no `jwt.secret` here — Product Service never validates tokens; that's the gateway's job.

---

## 6.4 Main Application Class

```java
// product-service/src/main/java/com/ecommerce/product/ProductServiceApplication.java
package com.ecommerce.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProductServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
```

**Notice:** No `@EnableDiscoveryClient` or `@EnableFeignClients` here. With Spring Cloud 2021+ auto-configuration, Eureka client registration happens automatically when `spring-cloud-starter-netflix-eureka-client` is on the classpath.

---

## 6.5 The Product Entity

```java
// product-service/src/main/java/com/ecommerce/product/entity/Product.java
package com.ecommerce.product.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")      // ① long text in the DB column
    private String description;

    @DecimalMin(value = "0.0", inclusive = false)
    @Column(nullable = false, precision = 10, scale = 2)  // ② precise money type
    private BigDecimal price;

    @Min(0)
    @Column(nullable = false)
    private Integer stock = 0;              // ③ default value at field level

    @Column(nullable = false)
    private String category;

    private String imageUrl;                // ④ nullable — no URL in sample data

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

### Key annotations explained

| Annotation | Location | Purpose |
|---|---|---|
| `@Entity` | Class | Marks this as a JPA-managed object; Hibernate creates/manages the table |
| `@Table(name = "products")` | Class | Maps to the `products` table (default would be `product`) |
| `@Id` | `id` field | Declares the primary key |
| `@GeneratedValue(IDENTITY)` | `id` field | Auto-increment in MySQL; database assigns the value |
| `@Column(columnDefinition = "TEXT")` | `description` | Overrides the default `VARCHAR(255)` — product descriptions can be long |
| `@Column(precision=10, scale=2)` | `price` | Stores up to 99,999,999.99 — never use `double`/`float` for money |
| `@DecimalMin` | `price` | Bean Validation: price must be > 0 |
| `@Min(0)` | `stock` | Bean Validation: stock cannot go negative |

### Why `BigDecimal` for price?

```
float    → 0.1 + 0.2 = 0.30000000000000004  (floating-point rounding error)
double   → same problem
BigDecimal → 0.1 + 0.2 = 0.3               (exact decimal arithmetic)
```

Financial calculations must use `BigDecimal`. Use `BigDecimal.add()`, `subtract()`, `multiply()` rather than `+`, `-`, `*`.

---

## 6.6 Data Transfer Object — `ProductDto`

```java
// product-service/src/main/java/com/ecommerce/product/dto/ProductDto.java
package com.ecommerce.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductDto {

    private Long id;

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @DecimalMin(value = "0.01", message = "Price must be positive")
    private BigDecimal price;

    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;

    @NotBlank(message = "Category is required")
    private String category;

    private String imageUrl;
    private LocalDateTime createdAt;

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

### Entity vs DTO — the Product case

The `Product` entity and `ProductDto` are nearly identical in this service because products have no sensitive fields (unlike `User` which has a password). The DTO pattern is still valuable here because:

1. **Decoupling:** The API contract (DTO) can evolve independently of the database schema (entity)
2. **Validation placement:** `@Valid` on DTOs validates incoming data; entities are validated by Hibernate when saving
3. **Computed fields:** In future, a DTO could expose `inStock: boolean` derived from `stock > 0` without a DB column
4. **Internal fields:** The `createdAt` field is read-only in the API (set by the entity default, not the client)

---

## 6.7 The Repository

```java
// product-service/src/main/java/com/ecommerce/product/repository/ProductRepository.java
package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Custom JPQL query — searches across name, description, and category
    @Query("SELECT p FROM Product p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.category) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Product> searchProducts(@Param("query") String query);

    // Derived query — Spring Data generates SQL from the method name
    List<Product> findByCategory(String category);

    // Derived query — finds products that are in stock
    List<Product> findByStockGreaterThan(int stock);
}
```

### Three types of queries in Spring Data JPA

**1. JpaRepository built-ins** (inherited automatically):
```java
findAll()           // SELECT * FROM products
findById(id)        // SELECT ... WHERE id = ?
save(product)       // INSERT or UPDATE
deleteById(id)      // DELETE WHERE id = ?
count()             // SELECT COUNT(*)
```

**2. Derived queries** (Spring Data parses the method name):
```java
findByCategory(String category)
// → SELECT p FROM Product p WHERE p.category = ?

findByStockGreaterThan(int stock)
// → SELECT p FROM Product p WHERE p.stock > ?
```
Spring Data reads the method name, splits it at keywords (`By`, `And`, `Or`, `GreaterThan`, `Like`, etc.), and generates the JPQL at startup. No SQL to write.

**3. Custom JPQL with `@Query`** (when derived queries are not expressive enough):
```java
@Query("SELECT p FROM Product p WHERE " +
       "LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR ...")
List<Product> searchProducts(@Param("query") String query);
```
- **JPQL, not SQL** — uses entity/field names, not table/column names
- `LOWER(...) LIKE LOWER(...)` — case-insensitive search
- `CONCAT('%', :query, '%')` — wraps the search term in wildcards
- `@Param("query")` — binds the method parameter to `:query` in the JPQL

> **JPQL vs SQL:**
> - SQL: `SELECT * FROM products WHERE LOWER(name) LIKE ?`
> - JPQL: `SELECT p FROM Product p WHERE LOWER(p.name) LIKE ?`
> JPQL uses class/field names; SQL uses table/column names. Hibernate translates JPQL → SQL at runtime.

---

## 6.8 The Service Layer

```java
// product-service/src/main/java/com/ecommerce/product/service/ProductService.java
package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductDto;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public List<ProductDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ProductDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return toDto(product);
    }

    public List<ProductDto> searchProducts(String query) {
        return productRepository.searchProducts(query).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ProductDto> getProductsByCategory(String category) {
        return productRepository.findByCategory(category).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ProductDto createProduct(ProductDto dto) {
        Product product = toEntity(dto);
        Product saved = productRepository.save(product);
        return toDto(saved);
    }

    public ProductDto updateProduct(Long id, ProductDto dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setCategory(dto.getCategory());
        product.setImageUrl(dto.getImageUrl());
        return toDto(productRepository.save(product));
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    /**
     * Called by Order Service (via OpenFeign) when placing an order.
     * Checks stock availability, then decrements it atomically.
     */
    public ProductDto reduceStock(Long id, int quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        if (product.getStock() < quantity) {
            throw new RuntimeException("Insufficient stock for product: " + id);
        }
        product.setStock(product.getStock() - quantity);
        return toDto(productRepository.save(product));
    }

    // --- Mapping helpers ---

    private ProductDto toDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStock(product.getStock());
        dto.setCategory(product.getCategory());
        dto.setImageUrl(product.getImageUrl());
        dto.setCreatedAt(product.getCreatedAt());
        return dto;
    }

    private Product toEntity(ProductDto dto) {
        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock() != null ? dto.getStock() : 0);  // null-safe default
        product.setCategory(dto.getCategory());
        product.setImageUrl(dto.getImageUrl());
        return product;
    }
}
```

### Focus: `reduceStock` — the service-to-service operation

This method is critical for the Order Service integration in Module 8 (Order Service). Trace what happens when an order is placed:

```
OrderService.placeOrder()
    │
    ├─ calls ProductServiceClient.getProductById(id) [Feign GET]
    │       └─ validates product exists and has the right price
    │
    └─ calls ProductServiceClient.reduceStock(id, quantity) [Feign PUT]
            └─ ProductController.reduceStock()
                    └─ ProductService.reduceStock()
                            ├─ findById(id) → throws if not found
                            ├─ stock < quantity → throws if insufficient
                            └─ stock -= quantity → save → return updated DTO
```

The `reduceStock` method implements a **check-then-act** pattern:
1. Load the entity (read lock via JPA)
2. Validate stock is sufficient
3. Decrement and save

> **Production consideration:** In a high-concurrency system, two simultaneous orders for the same product could both pass the stock check before either saves. The solution is `@Transactional` + database-level row locking (`SELECT ... FOR UPDATE`). For this training project, single-threaded ordering is assumed.

### Stream API pattern

```java
return productRepository.findAll().stream()  // ① Convert List<Product> to Stream<Product>
        .map(this::toDto)                    // ② Transform each Product → ProductDto
        .collect(Collectors.toList());       // ③ Collect back to List<ProductDto>
```

`this::toDto` is a **method reference** — equivalent to `p -> toDto(p)`. It applies the private `toDto()` helper to every element in the stream.

---

## 6.9 The Controller

```java
// product-service/src/main/java/com/ecommerce/product/controller/ProductController.java
package com.ecommerce.product.controller;

import com.ecommerce.product.dto.ProductDto;
import com.ecommerce.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    // PUBLIC — no auth needed (gateway permits all GET /api/products/**)
    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(productService.getProductById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();   // 404
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

    // ADMIN operations (no auth in this project — production would require role check)
    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductDto dto) {
        try {
            return ResponseEntity.ok(productService.updateProduct(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();   // 204
    }

    // INTERNAL — called by Order Service via Feign
    @PutMapping("/{id}/reduce-stock")
    public ResponseEntity<?> reduceStock(@PathVariable Long id, @RequestParam int quantity) {
        try {
            return ResponseEntity.ok(productService.reduceStock(id, quantity));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));  // 400
        }
    }
}
```

### REST design decisions

| Endpoint | Method | Path | Auth | Notes |
|---|---|---|---|---|
| List all | GET | `/api/products` | None | Public catalogue |
| Get one | GET | `/api/products/{id}` | None | Returns 404 if not found |
| Search | GET | `/api/products/search?q=laptop` | None | Searches name/description/category |
| By category | GET | `/api/products/category/Electronics` | None | Exact category match |
| Create | POST | `/api/products` | None* | *Would require ADMIN in production |
| Update | PUT | `/api/products/{id}` | None* | Full replacement |
| Delete | DELETE | `/api/products/{id}` | None* | Returns 204 No Content |
| Reduce stock | PUT | `/api/products/{id}/reduce-stock?quantity=2` | None | Called by Order Service internally |

**Why `PUT` for reduce-stock instead of `PATCH`?**
- `PATCH` is for partial updates where the client specifies which fields to change
- `PUT /reduce-stock` represents a specific **action** on the resource; it's an RPC-style operation that modifies one field
- Both are technically valid; the team chose `PUT` for simplicity here

---

## 6.10 Security Configuration

```java
// product-service/src/main/java/com/ecommerce/product/config/SecurityConfig.java
package com.ecommerce.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
        return http.build();
    }
}
```

### Why permit all in Product Service?

The security architecture of this project uses a **gateway-first** approach:

```
Internet → API Gateway (validates JWT) → Product Service (trusts all incoming)
```

The gateway already:
- Validates the JWT signature and expiry for protected endpoints
- Injects `X-User-Id` and `X-Username` headers for authenticated requests
- Allows `GET /api/products/**` through without any token

The Product Service doesn't need to re-validate because:
1. It cannot be reached from the internet directly (port 8082 would be firewalled in production)
2. All traffic arrives via the gateway which has already authenticated the caller
3. Adding duplicate JWT validation in every service would require every service to know the JWT secret

This is the **trust-the-network** model — services trust their internal network. In Kubernetes, this is enforced by `NetworkPolicy` rules.

---

## 6.11 DataInitializer — Seeding Sample Products

```java
// product-service/src/main/java/com/ecommerce/product/config/DataInitializer.java
package com.ecommerce.product.config;

import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private ProductRepository productRepository;

    @Override
    public void run(String... args) {
        if (productRepository.count() == 0) {   // ← only seed if table is empty
            productRepository.save(createProduct(
                "Laptop Pro 15",
                "High-performance laptop with 16GB RAM and 512GB SSD",
                new BigDecimal("1299.99"), 50, "Electronics"));

            productRepository.save(createProduct(
                "Wireless Headphones",
                "Noise-cancelling Bluetooth headphones",
                new BigDecimal("249.99"), 100, "Electronics"));

            productRepository.save(createProduct(
                "Running Shoes",
                "Lightweight running shoes for all terrains",
                new BigDecimal("89.99"), 200, "Footwear"));

            productRepository.save(createProduct(
                "Coffee Maker",
                "Automatic drip coffee maker with programmable timer",
                new BigDecimal("49.99"), 75, "Kitchen"));

            productRepository.save(createProduct(
                "Yoga Mat",
                "Non-slip eco-friendly yoga mat",
                new BigDecimal("29.99"), 150, "Sports"));

            productRepository.save(createProduct(
                "Smartphone X",
                "Latest smartphone with 5G and 128GB storage",
                new BigDecimal("799.99"), 30, "Electronics"));

            productRepository.save(createProduct(
                "Backpack Pro",
                "Waterproof laptop backpack with USB charging port",
                new BigDecimal("59.99"), 80, "Bags"));

            productRepository.save(createProduct(
                "Smart Watch",
                "Fitness tracker with heart rate monitor",
                new BigDecimal("199.99"), 60, "Electronics"));
        }
    }

    private Product createProduct(String name, String description,
                                   BigDecimal price, int stock, String category) {
        Product p = new Product();
        p.setName(name);
        p.setDescription(description);
        p.setPrice(price);
        p.setStock(stock);
        p.setCategory(category);
        return p;
    }
}
```

### How `CommandLineRunner` works

```
Spring Boot startup sequence:
  1. Create ApplicationContext
  2. Instantiate all @Component / @Service / @Repository beans
  3. Start the embedded Tomcat server
  4. Run all CommandLineRunner beans (in order of @Order annotation if needed)
  5. Application is ready
```

The `run()` method executes **once** immediately after the application is fully started, with full access to all Spring beans. The `if (count() == 0)` guard prevents re-seeding on every restart — once products are in the database, this is a no-op.

**Alternative approaches:**
- `data.sql` — Hibernate runs this file on startup (`spring.sql.init.mode=always`)
- Flyway / Liquibase — migration-based seeding with versioning
- `ApplicationReadyEvent` listener — fires after all initialization complete
- `@PostConstruct` on a `@Component` — runs after bean construction

`CommandLineRunner` is the simplest option for small training projects.

---

## 6.12 Package Structure

Your completed product-service should look like this:

```
product-service/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── com/ecommerce/product/
        │       ├── ProductServiceApplication.java    ← main class
        │       ├── config/
        │       │   ├── DataInitializer.java          ← seeds sample data
        │       │   └── SecurityConfig.java           ← permitAll
        │       ├── controller/
        │       │   └── ProductController.java        ← REST endpoints
        │       ├── dto/
        │       │   └── ProductDto.java               ← API contract
        │       ├── entity/
        │       │   └── Product.java                  ← JPA entity
        │       ├── repository/
        │       │   └── ProductRepository.java        ← data access + search
        │       └── service/
        │           └── ProductService.java           ← business logic
        └── resources/
            └── application.yml                       ← port + config server ref
```

---

## 6.13 Startup Order and Validation

### Step 1 — Ensure services are running

Before starting Product Service, make sure these are already running:

```
✓ Eureka Server   → http://localhost:8761
✓ Config Server   → http://localhost:8888
```

Verify Config Server is serving product configuration:
```
GET http://localhost:8888/product-service/default
```
You should see a JSON response containing the datasource URL, username, and password.

### Step 2 — Start Product Service

```bash
# From the project root
cd product-service
mvn spring-boot:run
```

Watch for these key log lines:
```
Fetching config from server at: http://localhost:8888
Located property source: CompositePropertySource [...]
Started ProductServiceApplication in X.XXX seconds
Registering application PRODUCT-SERVICE with eureka
```

### Step 3 — Verify Eureka registration

Open `http://localhost:8761` — you should now see `PRODUCT-SERVICE` listed.

### Step 4 — Validate via cURL (direct to service)

```bash
# ① List all products
curl http://localhost:8082/api/products

# ② Get product by ID
curl http://localhost:8082/api/products/1

# ③ Search products
curl "http://localhost:8082/api/products/search?q=laptop"

# ④ Filter by category
curl http://localhost:8082/api/products/category/Electronics

# ⑤ Test reduce-stock (simulating Order Service call)
curl -X PUT "http://localhost:8082/api/products/1/reduce-stock?quantity=2"

# Check stock reduced from 50 to 48
curl http://localhost:8082/api/products/1
```

### Step 5 — Validate via API Gateway

With the gateway running (port 8080), repeat the calls through the gateway:

```bash
# No token needed — GET /api/products/** is public
curl http://localhost:8080/api/products

# Search through gateway
curl "http://localhost:8080/api/products/search?q=headphones"

# Category filter through gateway
curl http://localhost:8080/api/products/category/Electronics
```

These should return identical results to direct service calls, proving the gateway is correctly routing to the Product Service via Eureka.

---

## 6.14 Common Mistakes

### Mistake 1: Using `double` or `float` for price

```java
// WRONG — precision loss
private double price;

// CORRECT — exact decimal arithmetic
private BigDecimal price;
```

And when creating `BigDecimal` values:
```java
// WRONG — double literal causes precision loss before BigDecimal wraps it
new BigDecimal(1299.99)  // → 1299.99000000000001...

// CORRECT — string literal preserves exact value
new BigDecimal("1299.99")
```

### Mistake 2: Not guarding DataInitializer with count() check

```java
// WRONG — inserts duplicates on every restart
@Override
public void run(String... args) {
    productRepository.save(createProduct(...));
}

// CORRECT — only seed if empty
@Override
public void run(String... args) {
    if (productRepository.count() == 0) {
        productRepository.save(createProduct(...));
    }
}
```

### Mistake 3: JPQL using table/column names instead of entity/field names

```java
// WRONG — SQL syntax in JPQL
@Query("SELECT * FROM products WHERE name LIKE ?")

// CORRECT — JPQL uses entity class and field names
@Query("SELECT p FROM Product p WHERE p.name LIKE :name")
```

### Mistake 4: Forgetting `@Param` with named parameters in JPQL

```java
// WRONG — :query not bound to a method parameter
@Query("SELECT p FROM Product p WHERE p.name LIKE :query")
List<Product> search(String searchTerm);

// CORRECT — @Param connects the named parameter to the argument
@Query("SELECT p FROM Product p WHERE p.name LIKE :query")
List<Product> search(@Param("query") String searchTerm);
```

### Mistake 5: Returning entities directly from the controller

```java
// WRONG — exposes internal JPA proxies, can cause LazyInitializationException
@GetMapping
public List<Product> getAllProducts() {
    return productRepository.findAll();
}

// CORRECT — map to DTO in service layer before returning
@GetMapping
public List<ProductDto> getAllProducts() {
    return productService.getAllProducts();
}
```

---

## 6.15 Concepts Recap

| Concept | What you learned |
|---|---|
| `@Entity` + `@Table` | Maps a Java class to a database table |
| `BigDecimal` | Exact decimal type for financial values |
| `@Column(columnDefinition)` | Override Hibernate's default column type |
| Derived query methods | Spring Data generates SQL from the method name |
| `@Query` with JPQL | Custom queries using entity/field names |
| `CommandLineRunner` | Hook to run code after Spring Boot startup |
| DTO pattern | Separate API contract from internal entity |
| Stream `map()` | Transform a collection element-by-element |
| `orElseThrow()` | Handle missing Optional with a meaningful exception |
| Gateway-first auth | Services trust internal network; auth at the edge |

---

## Module Checkpoint

Before moving to Module 7 (API Gateway), confirm:

- [ ] `GET http://localhost:8888/product-service/default` returns datasource config
- [ ] Product Service starts without errors (`Started ProductServiceApplication`)
- [ ] `PRODUCT-SERVICE` appears in the Eureka dashboard at `http://localhost:8761`
- [ ] `GET http://localhost:8082/api/products` returns 8 products
- [ ] `GET http://localhost:8082/api/products/search?q=laptop` returns "Laptop Pro 15"
- [ ] `GET http://localhost:8082/api/products/category/Electronics` returns 4 products
- [ ] `PUT http://localhost:8082/api/products/1/reduce-stock?quantity=2` reduces stock by 2

### Postman Validation — through the API Gateway

Once the API Gateway is also running (Module 7), validate product endpoints through port 8080:

- [ ] `GET http://localhost:8080/api/products` → 8 products (no token needed)
- [ ] `GET http://localhost:8080/api/products/search?q=headphones` → "Wireless Headphones"
- [ ] `GET http://localhost:8080/api/products/category/Electronics` → 4 products

---

## What's Next — Module 7

With User Service, Product Service, and the two infrastructure services working, **Module 7** implements the **API Gateway**:

- How `JwtAuthFilter` implements `GlobalFilter` to intercept every request
- How it validates the JWT signature and extracts `userId` + `username` claims
- How it injects `X-User-Id` and `X-Username` headers for downstream services
- How `getOrder() = -1` ensures the filter runs before all default filters
- How route predicates and `lb://` URIs resolve via Eureka
- Full end-to-end Postman validation through a single port (8080)
