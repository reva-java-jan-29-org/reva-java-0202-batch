# Spring Web & REST APIs — Complete Overview

**Project:** `week5-03-spring-web-rest-api-03042026`
**Date:** Wednesday, 4 March 2026
**Tech Stack:** Spring Boot 3.4 · Spring Web · Spring Data JPA · MySQL · Lombok · Bean Validation

---

## What We're Building

A **Product Catalog REST API** — a real-world, production-style REST API that manages products and categories for an online store.

This single project covers every aspect of building REST APIs with Spring Boot:

```
Client (Postman / Browser / React App)
         │
         │  HTTP Request (JSON)
         ▼
┌─────────────────────────────────────────────┐
│           Embedded Tomcat (port 8080)       │
│                                             │
│  DispatcherServlet (Front Controller)       │
│         │                                   │
│         ▼                                   │
│  @RestController  ←── routes requests       │
│         │                                   │
│         ▼                                   │
│  @Service         ←── business logic        │
│         │                                   │
│         ▼                                   │
│  @Repository      ←── data access (JPA)     │
│         │                                   │
│         ▼                                   │
│       MySQL Database                        │
└─────────────────────────────────────────────┘
         │
         │  HTTP Response (JSON)
         ▼
       Client
```

---

## Module Navigation

| # | File | Topic |
|---|------|-------|
| 01 | [01-spring-web-mvc-architecture.md](01-spring-web-mvc-architecture.md) | How Spring Web works internally |
| 02 | [02-rest-fundamentals.md](02-rest-fundamentals.md) | REST principles, HTTP methods, status codes |
| 03 | [03-controllers-and-routing.md](03-controllers-and-routing.md) | @RestController, @GetMapping, @PathVariable, @RequestParam |
| 04 | [04-request-response-handling.md](04-request-response-handling.md) | @RequestBody, ResponseEntity, DTOs, Jackson |
| 05 | [05-service-layer-and-di.md](05-service-layer-and-di.md) | @Service, Constructor Injection, @Transactional |
| 06 | [06-jpa-integration.md](06-jpa-integration.md) | Entity, Repository, MySQL setup |
| 07 | [07-exception-handling.md](07-exception-handling.md) | @RestControllerAdvice, custom errors |
| 08 | [08-validation.md](08-validation.md) | @Valid, Bean Validation, error messages |
| 09 | [09-interview-questions.md](09-interview-questions.md) | Common REST API interview questions |

---

## API Endpoints Reference

### Categories API

| Method | URL | Description | Request Body | Response |
|--------|-----|-------------|--------------|----------|
| `GET` | `/api/categories` | List all categories | — | `200 OK` + `[CategoryResponse]` |
| `GET` | `/api/categories/{id}` | Get one category | — | `200 OK` + `CategoryResponse` |
| `POST` | `/api/categories` | Create category | `CategoryRequest` | `201 Created` + `CategoryResponse` |
| `PUT` | `/api/categories/{id}` | Update category | `CategoryRequest` | `200 OK` + `CategoryResponse` |
| `DELETE` | `/api/categories/{id}` | Delete category | — | `204 No Content` |

### Products API

| Method | URL | Description | Request Body | Response |
|--------|-----|-------------|--------------|----------|
| `GET` | `/api/products` | List all products | — | `200 OK` + `[ProductResponse]` |
| `GET` | `/api/products?name=laptop` | Search by name | — | `200 OK` + `[ProductResponse]` |
| `GET` | `/api/products?categoryId=1` | Filter by category | — | `200 OK` + `[ProductResponse]` |
| `GET` | `/api/products?minPrice=100&maxPrice=500` | Filter by price range | — | `200 OK` + `[ProductResponse]` |
| `GET` | `/api/products/{id}` | Get one product | — | `200 OK` + `ProductResponse` |
| `POST` | `/api/products` | Create product | `ProductRequest` | `201 Created` + `ProductResponse` |
| `PUT` | `/api/products/{id}` | Full update | `ProductRequest` | `200 OK` + `ProductResponse` |
| `PATCH` | `/api/products/{id}/stock?quantity=50` | Update stock only | — | `200 OK` + `ProductResponse` |
| `DELETE` | `/api/products/{id}` | Delete product | — | `204 No Content` |

---

## Project Package Structure

```
com.training
├── SpringWebRestApiApplication.java        ← main class, entry point
│
├── product
│   ├── Product.java                        ← @Entity (DB table)
│   ├── ProductRepository.java              ← Spring Data JPA queries
│   ├── ProductRequest.java                 ← DTO: what client SENDS
│   ├── ProductResponse.java                ← DTO: what client RECEIVES
│   ├── ProductService.java                 ← business logic
│   └── ProductController.java             ← HTTP endpoints
│
├── category
│   ├── Category.java                       ← @Entity (DB table)
│   ├── CategoryRepository.java             ← Spring Data JPA queries
│   ├── CategoryRequest.java                ← DTO: what client SENDS
│   ├── CategoryResponse.java               ← DTO: what client RECEIVES
│   ├── CategoryService.java                ← business logic
│   └── CategoryController.java            ← HTTP endpoints
│
└── exception
    ├── ResourceNotFoundException.java      ← 404 custom exception
    ├── DuplicateResourceException.java     ← 409 custom exception
    ├── ErrorResponse.java                  ← standard error JSON structure
    └── GlobalExceptionHandler.java         ← @RestControllerAdvice
```

---

## Quick Start — Test It Yourself

### 1. Start MySQL and ensure `product_catalog_db` is accessible (auto-created by `createDatabaseIfNotExist=true`)

### 2. Run the application
```bash
mvn spring-boot:run
```
or run `SpringWebRestApiApplication.java` from your IDE.

### 3. Test with curl or Postman

**Create a category:**
```bash
curl -X POST http://localhost:8080/api/categories \
  -H "Content-Type: application/json" \
  -d '{"name": "Electronics", "description": "Electronic devices and gadgets"}'
```

**Create a product:**
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "MacBook Pro",
    "description": "Apple M3 chip, 16GB RAM",
    "price": 1999.99,
    "stockQuantity": 50,
    "categoryId": 1
  }'
```

**Get all products:**
```bash
curl http://localhost:8080/api/products
```

**Search products:**
```bash
curl "http://localhost:8080/api/products?name=mac"
```

**Update stock:**
```bash
curl -X PATCH "http://localhost:8080/api/products/1/stock?quantity=100"
```

---

## Key Concepts Taught in This Project

| Concept | Where to Find It |
|---------|-----------------|
| `@RestController` | `CategoryController.java`, `ProductController.java` |
| `@RequestMapping`, `@GetMapping`, etc. | Both controllers |
| `@PathVariable` | `/{id}` endpoints in both controllers |
| `@RequestParam` | `getProducts()` in `ProductController.java` |
| `@RequestBody` | POST/PUT endpoints in both controllers |
| `ResponseEntity` | All controller methods |
| DTO Pattern | `*Request.java`, `*Response.java` |
| `@Service` + Constructor Injection | Both service classes |
| `@Transactional` | Service classes |
| `@Repository` + JPA queries | Both repository interfaces |
| `@Entity`, `@ManyToOne` | `Product.java`, `Category.java` |
| `@Valid` + Bean Validation | Request DTOs + controller parameters |
| `@RestControllerAdvice` | `GlobalExceptionHandler.java` |
| Custom Exceptions | `ResourceNotFoundException.java`, `DuplicateResourceException.java` |
| Standardized Error Response | `ErrorResponse.java` |
