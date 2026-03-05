# Module 06 — Spring Data JPA Integration

## Overview

Spring Data JPA sits between your service layer and the database:

```
Service → Repository (interface) → Spring Data JPA → Hibernate → MySQL
```

You write an **interface** with method signatures. Spring Data JPA generates the actual SQL and implementation at runtime. You never write JDBC boilerplate again.

---

## Setting Up MySQL in application.properties

```properties
# The JDBC URL tells Hibernate where MySQL is and which database to use.
# createDatabaseIfNotExist=true → MySQL creates "product_catalog_db" if it doesn't exist.
spring.datasource.url=jdbc:mysql://localhost:3306/product_catalog_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=Root123
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Hibernate Schema Generation:
# update   → Hibernate checks entities and alters/creates tables (safe for dev)
# create   → Drops all tables and recreates (data loss! dev only)
# validate → Validates schema matches entities, fails if mismatch (good for prod)
# none     → Don't touch the schema (production, when you manage migrations manually)
spring.jpa.hibernate.ddl-auto=update

# Shows SQL in console (development only)
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

---

## @Entity — Mapping Classes to DB Tables

```java
@Entity                         // Marks this class as a JPA entity (maps to a table)
@Table(name = "products")       // Specifies the table name (default: class name lowercase)
public class Product {

    @Id                         // Primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // AUTO_INCREMENT in MySQL
    private Long id;

    @Column(nullable = false, length = 200)  // NOT NULL, VARCHAR(200)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)  // DECIMAL(10,2)
    private BigDecimal price;

    @Column(nullable = false)   // NOT NULL
    private Integer stockQuantity;
}
```

### @Column Options

| Attribute | DB Equivalent | Example |
|-----------|--------------|---------|
| `name` | Column name | `name = "product_name"` |
| `nullable = false` | NOT NULL | Prevents null values |
| `unique = true` | UNIQUE constraint | Ensures uniqueness |
| `length` | VARCHAR length | `length = 200` |
| `precision, scale` | DECIMAL(p,s) | `precision=10, scale=2` → 12345678.99 |
| `insertable = false` | Can't INSERT this column | For DB-managed columns |
| `updatable = false` | Can't UPDATE this column | For created_at fields |

### @GeneratedValue Strategies

```java
// IDENTITY — uses AUTO_INCREMENT (MySQL, PostgreSQL)
@GeneratedValue(strategy = GenerationType.IDENTITY)

// SEQUENCE — uses a DB sequence (Oracle, PostgreSQL)
@GeneratedValue(strategy = GenerationType.SEQUENCE)

// AUTO — Hibernate picks the best strategy for the DB
@GeneratedValue(strategy = GenerationType.AUTO)
```

---

## Relationships in This Project

### Category (One) → Products (Many)

```java
// Category.java — "one" side (inverse)
@OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Product> products = new ArrayList<>();

// Product.java — "many" side (owning — has the FK column)
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "category_id", nullable = false)
private Category category;
```

Generated MySQL schema:
```sql
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500)
);

CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    stock_quantity INT NOT NULL,
    category_id BIGINT NOT NULL,          ← FK column (from @JoinColumn)
    FOREIGN KEY (category_id) REFERENCES categories(id)
);
```

---

## Repository Pattern

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // JpaRepository<EntityType, PrimaryKeyType>
}
```

`JpaRepository` gives you for free:

```java
// CRUD
productRepository.save(product)               // INSERT or UPDATE
productRepository.findById(1L)                // SELECT WHERE id = 1
productRepository.findAll()                   // SELECT *
productRepository.deleteById(1L)              // DELETE WHERE id = 1
productRepository.existsById(1L)              // SELECT COUNT(*) WHERE id = 1
productRepository.count()                     // SELECT COUNT(*)

// Batch operations
productRepository.saveAll(list)
productRepository.deleteAllById(list)

// Pagination (advanced)
productRepository.findAll(PageRequest.of(0, 10))  // first 10 records
```

---

## Query Methods — Three Approaches

### 1. Derived Query Methods (Method Name Parsing)

Spring reads the method name and generates the SQL automatically:

```java
// findBy<Field><Condition>

List<Product> findByCategoryId(Long categoryId);
// → SELECT * FROM products WHERE category_id = ?

List<Product> findByNameContainingIgnoreCase(String name);
// → SELECT * FROM products WHERE LOWER(name) LIKE LOWER('%?%')

List<Product> findByPriceBetween(BigDecimal min, BigDecimal max);
// → SELECT * FROM products WHERE price BETWEEN ? AND ?

List<Product> findByStockQuantityGreaterThan(int qty);
// → SELECT * FROM products WHERE stock_quantity > ?

Optional<Category> findByName(String name);
// → SELECT * FROM categories WHERE name = ?

boolean existsByName(String name);
// → SELECT COUNT(*) > 0 FROM categories WHERE name = ?
```

**Keyword reference:**

| Keyword | Example | SQL |
|---------|---------|-----|
| `And` | `findByNameAndCategoryId` | `WHERE name=? AND category_id=?` |
| `Or` | `findByNameOrDescription` | `WHERE name=? OR description=?` |
| `Between` | `findByPriceBetween` | `WHERE price BETWEEN ? AND ?` |
| `LessThan` | `findByPriceLessThan` | `WHERE price < ?` |
| `GreaterThan` | `findByStockQuantityGreaterThan` | `WHERE stock > ?` |
| `Like` | `findByNameLike` | `WHERE name LIKE ?` |
| `Containing` | `findByNameContaining` | `WHERE name LIKE '%?%'` |
| `IgnoreCase` | `findByNameIgnoreCase` | `WHERE LOWER(name) = LOWER(?)` |
| `OrderBy` | `findAllByOrderByNameAsc` | `ORDER BY name ASC` |

### 2. JPQL with @Query

JPQL (Java Persistence Query Language) uses **entity class names** and **field names**, not table/column names:

```java
// JPQL — refers to "Product" entity and "category" field (not table/column names)
@Query("SELECT p FROM Product p WHERE p.price < :maxPrice ORDER BY p.price ASC")
List<Product> findCheaperThan(@Param("maxPrice") BigDecimal maxPrice);

// JOIN FETCH — solves N+1 problem by loading category in the same query
@Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.category.id = :categoryId")
List<Product> findByCategoryIdWithCategory(@Param("categoryId") Long categoryId);
```

**JPQL vs SQL:**
```
SQL:  SELECT * FROM products WHERE category_id = 1
JPQL: SELECT p FROM Product p WHERE p.category.id = 1
                   ↑ entity name    ↑ field path (not column name)
```

### 3. Native SQL with nativeQuery = true

```java
// Raw SQL — use when JPQL can't express the query (DB-specific functions, complex joins)
@Query(value = "SELECT * FROM products WHERE price < :maxPrice ORDER BY price ASC",
       nativeQuery = true)
List<Product> findCheaperThan(@Param("maxPrice") BigDecimal maxPrice);
```

Downside: loses database portability (this SQL might not work on a different DB).

---

## The N+1 Query Problem

This is a classic performance pitfall with ORM tools. It's critical to understand.

### The problem

```java
// Imagine you have 100 products, each with a LAZY-loaded category.
List<Product> products = productRepository.findAll();   // Query 1: SELECT * FROM products

for (Product product : products) {
    // Each access triggers a separate query!
    System.out.println(product.getCategory().getName()); // Query 2, 3, 4...101
}

// Total: 1 + 100 = 101 queries!  ← N+1 problem (N = 100 products)
```

### The solution: JOIN FETCH

```java
// In ProductRepository:
@Query("SELECT p FROM Product p JOIN FETCH p.category")
List<Product> findAllWithCategory();

// This generates a SINGLE query:
// SELECT p.*, c.* FROM products p JOIN categories c ON p.category_id = c.id
// Total: 1 query! ← problem solved
```

### When does N+1 happen?

1. LAZY loading (which is correct by default)
2. Accessing the lazy relationship inside a loop
3. No JOIN FETCH or entity graph configured

In our project, `ProductResponse.from(product)` accesses `product.getCategory()`. If the category isn't loaded, Hibernate fires a query. To avoid N+1, we use `findByCategoryIdWithCategory()` which uses JOIN FETCH.

---

## Saving and Updating

```java
// CREATE
Product product = Product.builder()
    .name("Laptop")
    .price(new BigDecimal("999.99"))
    .category(category)
    .build();
Product saved = productRepository.save(product);
// Hibernate: INSERT INTO products (name, price, category_id) VALUES (?, ?, ?)
// saved.getId() is now populated

// UPDATE — same save() method!
// JPA detects the entity has an id → runs UPDATE instead of INSERT
saved.setPrice(new BigDecimal("899.99"));
productRepository.save(saved);
// Hibernate: UPDATE products SET price = ? WHERE id = ?

// Alternative: update in managed context (no explicit save needed)
@Transactional
public void updatePrice(Long id, BigDecimal newPrice) {
    Product product = productRepository.findById(id).orElseThrow();
    product.setPrice(newPrice);
    // No save() needed! Within @Transactional, Hibernate dirty-checks
    // the managed entity and automatically flushes changes at commit time.
}
```
