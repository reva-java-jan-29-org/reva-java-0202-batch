# Module 09 — REST API Interview Questions

## Core REST Concepts

---

### Q1: What is REST? What are its core principles?

**REST** (Representational State Transfer) is an architectural style for distributed hypermedia systems, defined by Roy Fielding.

Core constraints:
1. **Client-Server** — separates UI from data storage
2. **Stateless** — no session on the server; every request is self-contained
3. **Cacheable** — responses declare if they can be cached
4. **Uniform Interface** — standard methods, URIs, representations
5. **Layered System** — client can't tell if it's talking to the origin server or a proxy
6. **Code on Demand** (optional) — server can send executable code

Most important for API design: **Stateless** and **Uniform Interface**.

---

### Q2: What is the difference between GET and POST?

| | GET | POST |
|--|-----|------|
| Purpose | Read data | Create/submit data |
| Body | No body | Has request body |
| Safe? | Yes (no side effects) | No |
| Idempotent? | Yes | No |
| Cached? | Yes | No |
| Bookmarkable? | Yes | No |
| URL visible? | Yes (query params) | No (body is private) |

---

### Q3: What is the difference between PUT and PATCH?

| | PUT | PATCH |
|--|-----|-------|
| Purpose | Full replacement of resource | Partial update |
| Required fields | ALL fields | Only fields being updated |
| Idempotent? | Yes | Yes (if implemented correctly) |
| Example use | Replace entire product | Update just the stock quantity |

```
PUT /api/products/5  Body: { all 5 fields }  → replaces everything
PATCH /api/products/5/stock?quantity=100      → only updates stock
```

---

### Q4: What is the difference between @Controller and @RestController?

```java
@Controller                        // Returns view names (HTML)
public class WebController {
    @GetMapping("/home")
    public String home() {
        return "home";             // → resolves to templates/home.html
    }
}

@RestController                    // Returns data (auto-converted to JSON)
public class ApiController {
    @GetMapping("/api/products")
    public List<Product> getAll() {
        return products;           // → Jackson converts to JSON
    }
}
```

`@RestController` = `@Controller` + `@ResponseBody`

`@ResponseBody` tells Spring to write the return value to the HTTP response body (via Jackson) instead of resolving a view.

---

### Q5: What is @RequestBody and @RequestParam? When do you use each?

**@RequestParam** — extracts data from the URL:
```
GET /api/products?name=laptop&minPrice=100
@RequestParam String name         → "laptop"
@RequestParam BigDecimal minPrice → 100
```

**@RequestBody** — reads the HTTP request body (JSON → Java object):
```
POST /api/products
Body: {"name": "Laptop", "price": 999.99}
@RequestBody ProductRequest request → populated from JSON body
```

| | `@RequestParam` | `@RequestBody` |
|--|----------------|----------------|
| Source | URL query string or form data | HTTP request body |
| HTTP methods | GET (usually) | POST, PUT, PATCH |
| Multiple values | Multiple params | One structured object |
| Use for | Filtering, pagination, search | Creating/updating resources |

---

### Q6: What HTTP status codes do you know? When do you use each?

**2xx Success:**
- `200 OK` — successful GET, PUT, PATCH
- `201 Created` — successful POST (resource created)
- `204 No Content` — successful DELETE (nothing to return)

**4xx Client Error:**
- `400 Bad Request` — invalid input, validation failure, malformed JSON
- `401 Unauthorized` — not authenticated (no token or invalid token)
- `403 Forbidden` — authenticated but not authorized (wrong role)
- `404 Not Found` — resource doesn't exist
- `409 Conflict` — duplicate resource (e.g., duplicate email)
- `422 Unprocessable Entity` — business rule violation

**5xx Server Error:**
- `500 Internal Server Error` — unexpected server crash
- `503 Service Unavailable` — server overloaded or in maintenance

---

### Q7: What is ResponseEntity? Why do you use it?

`ResponseEntity<T>` is a Spring class that represents the entire HTTP response:
- Status code
- Headers
- Body

```java
// Without ResponseEntity — always returns 200 OK
public ProductResponse getById(Long id) {
    return productService.findById(id);
}

// With ResponseEntity — full control
public ResponseEntity<ProductResponse> create(ProductRequest req) {
    ProductResponse created = productService.create(req);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);  // 201 Created
}

// No body
public ResponseEntity<Void> delete(Long id) {
    productService.delete(id);
    return ResponseEntity.noContent().build();  // 204 No Content
}
```

Use `ResponseEntity` when you need to control the status code or add response headers.

---

### Q8: What is a DTO? Why do you use DTOs instead of Entities directly in REST APIs?

**DTO** (Data Transfer Object) — an object designed to carry data between layers.

Reasons not to expose JPA entities directly:

1. **Security** — Entity fields like `password`, `internalCode` would be serialized
2. **LazyInitializationException** — LAZY-loaded collections fail when Jackson tries to serialize them after the Hibernate session closes
3. **Infinite loops** — Bidirectional relationships cause StackOverflow during serialization
4. **Tight coupling** — DB schema changes break the API contract
5. **Different shapes** — Request shape ≠ Response shape ≠ DB shape

Best practice:
```
ProductRequest  → what client SENDS (with validation annotations)
ProductResponse → what client RECEIVES (curated fields only)
Product (entity) → DB representation (never leaves the service layer)
```

---

### Q9: What is @RestControllerAdvice? How does it differ from @ExceptionHandler in a controller?

`@ExceptionHandler` in a controller handles exceptions only for **that controller**.

`@RestControllerAdvice` is a global handler applied to **all controllers**.

```java
// ❌ Only works for exceptions from ProductController
@RestController
public class ProductController {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handle(ResourceNotFoundException ex) { ... }
}

// ✅ Works for ALL controllers
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handle(ResourceNotFoundException ex, ...) { ... }
}
```

`@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody`

---

### Q10: What is the difference between @Transactional(readOnly = true) and @Transactional?

| | `@Transactional` | `@Transactional(readOnly = true)` |
|--|-----------------|----------------------------------|
| Use for | Write operations (INSERT, UPDATE, DELETE) | Read operations (SELECT only) |
| Dirty checking | Enabled (Hibernate tracks changes) | Disabled (performance optimization) |
| DB optimization | No | Some DBs route to read replicas |
| Flushes changes | Yes | No |

In service classes: set `readOnly = true` at class level (default for all methods), then override with `@Transactional` on write methods.

---

### Q11: What is the N+1 query problem?

When loading N entities triggers N additional queries to fetch their lazy-loaded relationships.

```java
// 100 products → 1 query
// For each product, accessing category → 100 more queries
// Total: 101 queries → performance disaster
List<Product> products = productRepository.findAll();
products.forEach(p -> System.out.println(p.getCategory().getName()));  // N+1!
```

**Solution — JOIN FETCH:**
```java
@Query("SELECT p FROM Product p JOIN FETCH p.category")
List<Product> findAllWithCategory();
// → Single query with JOIN: total = 1 query
```

---

### Q12: What is the difference between @PathVariable and a query parameter?

```
Path variable:      /api/products/42         → identifies a specific resource
Query parameter:    /api/products?name=mac   → filters/searches resources
```

REST convention:
- Path variables for resource identification: `/api/products/{id}`
- Query params for filtering, sorting, pagination: `?page=2&sort=price`

---

### Q13: What Bean Validation annotations do you know?

**String:**
- `@NotNull`, `@NotEmpty`, `@NotBlank`
- `@Size(min, max)`, `@Length(min, max)`
- `@Email`, `@Pattern(regexp)`

**Numeric:**
- `@Min(value)`, `@Max(value)`
- `@DecimalMin`, `@DecimalMax`
- `@Positive`, `@PositiveOrZero`, `@Negative`, `@NegativeOrZero`
- `@Digits(integer, fraction)`

**Date:**
- `@Past`, `@PastOrPresent`, `@Future`, `@FutureOrPresent`

**Trigger:**
- `@Valid` on controller parameter triggers validation on `@RequestBody`
- `@Validated` on controller class triggers validation on `@PathVariable` / `@RequestParam`

---

### Q14: What is Idempotency? Which HTTP methods are idempotent?

**Idempotent** = calling the operation multiple times with the same input always produces the same result.

```
GET    /api/products/5   → Always returns the same product (idempotent ✅)
DELETE /api/products/5   → First call deletes it; subsequent calls → 404 (same final state ✅)
PUT    /api/products/5   → Always sets the product to the given values (idempotent ✅)
POST   /api/products     → Each call creates a NEW product → different IDs (NOT idempotent ❌)
```

| Method | Idempotent | Safe |
|--------|-----------|------|
| GET | ✅ | ✅ |
| DELETE | ✅ | ❌ |
| PUT | ✅ | ❌ |
| PATCH | ✅* | ❌ |
| POST | ❌ | ❌ |

---

### Q15: How would you design pagination for a REST API?

```
GET /api/products?page=0&size=10&sort=price,asc

Response:
{
  "content": [...],        // array of products
  "totalElements": 150,   // total records in DB
  "totalPages": 15,       // 150 / 10
  "number": 0,            // current page (0-indexed)
  "size": 10,             // page size
  "first": true,          // is this the first page?
  "last": false           // is this the last page?
}
```

In Spring Data JPA:
```java
@GetMapping
public Page<ProductResponse> getAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id") String sortBy) {

    Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
    return productRepository.findAll(pageable)
        .map(ProductResponse::from);
}
```

---

### Q16: How do you secure a REST API?

Common approaches:
1. **JWT (JSON Web Token)** — stateless, most common for modern REST APIs
2. **OAuth 2.0** — for third-party authorization (Google login, GitHub login)
3. **API Keys** — simpler, for server-to-server communication
4. **Basic Auth** — username/password in header (only over HTTPS)
5. **Session + Cookie** — stateful, traditional web apps

In Spring Boot:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

Spring Security provides authentication (who you are) and authorization (what you can do).

---

### Q17: What is CORS? How do you handle it in Spring Boot?

**CORS** (Cross-Origin Resource Sharing) — browsers block requests from a different origin (protocol + domain + port).

```
Frontend: http://localhost:3000
Backend:  http://localhost:8080  ← different port = different origin → browser blocks!
```

Solution in Spring Boot:
```java
// Option 1: @CrossOrigin on a specific controller
@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class ProductController { ... }

// Option 2: Global CORS configuration
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH");
    }
}
```
