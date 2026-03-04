# Module 04 — Request & Response Handling

## @RequestBody — Receiving JSON

`@RequestBody` tells Spring to read the **HTTP request body**, parse the JSON, and deserialize it into a Java object using Jackson.

```java
@PostMapping
public ResponseEntity<ProductResponse> createProduct(@RequestBody ProductRequest request) {
    // Spring + Jackson automatically populated:
    // request.getName()         → "MacBook Pro"
    // request.getPrice()        → 1999.99
    // request.getStockQuantity() → 50
    // request.getCategoryId()   → 1
}
```

### What the client sends:

```http
POST /api/products
Content-Type: application/json

{
  "name": "MacBook Pro",
  "description": "Apple M3, 16GB RAM",
  "price": 1999.99,
  "stockQuantity": 50,
  "categoryId": 1
}
```

### Important rules for @RequestBody:

1. The `Content-Type: application/json` header **must** be present in the request.
2. Jackson uses the **field names** in JSON to match the Java fields (or getter names).
3. Unknown JSON fields are ignored by default.
4. Missing JSON fields become `null` (or default value) in the Java object.

---

## @ResponseBody — Sending JSON

When a method is in a `@RestController`, every return value is automatically annotated with `@ResponseBody` — Spring serializes the return value to JSON.

```java
// In a @RestController, this:
@GetMapping("/{id}")
public ProductResponse getById(@PathVariable Long id) {
    return productService.findById(id);   // ProductResponse is serialized to JSON
}

// Is equivalent to:
@GetMapping("/{id}")
@ResponseBody
public ProductResponse getById(@PathVariable Long id) {
    return productService.findById(id);
}
```

Jackson determines the JSON field names from the class fields (or getter methods).

---

## ResponseEntity — Full Control Over the HTTP Response

`ResponseEntity<T>` lets you control:
- The **HTTP status code**
- The **response body**
- The **response headers**

```java
// Just status 200 + body
return ResponseEntity.ok(product);

// Status 201 + body
return ResponseEntity.status(HttpStatus.CREATED).body(created);

// Status 204 (no content, no body)
return ResponseEntity.noContent().build();

// Status 404 + error message body
return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found");

// Custom header + body
return ResponseEntity.ok()
    .header("X-Custom-Header", "value")
    .body(product);
```

### ResponseEntity Builder Pattern

```java
// Full builder chain
ResponseEntity<ProductResponse> response = ResponseEntity
    .status(HttpStatus.CREATED)         // set status
    .header("Location", "/api/products/5")  // add header
    .body(createdProduct);              // set body

return response;
```

### Generic type parameter

```java
ResponseEntity<ProductResponse>    // body is ProductResponse
ResponseEntity<List<ProductResponse>>  // body is a list
ResponseEntity<Void>               // no body (for DELETE)
ResponseEntity<Object>             // body can be anything
ResponseEntity<?>                  // wildcard (avoid — use specific types)
```

---

## DTO Pattern — Why Not Return Entities Directly?

### Problem with returning Entities directly

```java
// ❌ BAD — returns the JPA entity directly
@GetMapping("/{id}")
public Product getById(@PathVariable Long id) {
    return productRepository.findById(id).orElseThrow();
}
```

Problems:
1. **Lazy loading crash**: `product.getCategory()` is LAZY. Jackson tries to serialize it → Hibernate session is closed → `LazyInitializationException`
2. **Infinite loops**: `Product` → `Category` → `products` list → `Product` → infinite recursion
3. **Security leak**: Entity may have fields you don't want to expose (internal IDs, passwords)
4. **Coupling**: API shape is now tied to DB schema. If the DB changes, the API breaks.

### Solution: DTO Pattern

```
Client ←→ DTO ←→ Service ←→ Entity ←→ Database
         (API layer)     (DB layer)
```

- **Request DTO** (`ProductRequest`): models what the client SENDS. Has validation annotations.
- **Response DTO** (`ProductResponse`): models what the client RECEIVES. We choose exactly what to expose.
- **Entity** (`Product`): models the DB table. Not exposed to the client.

```java
// ✅ GOOD — return a DTO
@GetMapping("/{id}")
public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
    ProductResponse response = productService.findById(id);
    return ResponseEntity.ok(response);
}
```

### Mapping Entity → DTO

```java
// Method 1: Static factory in the DTO class (used in this project)
public class ProductResponse {
    public static ProductResponse from(Product product) {
        return ProductResponse.builder()
            .id(product.getId())
            .name(product.getName())
            .price(product.getPrice())
            .categoryId(product.getCategory().getId())
            .categoryName(product.getCategory().getName())
            .build();
    }
}

// Usage in service:
return ProductResponse.from(savedProduct);

// Method 2: MapStruct (library — generates mapper code at compile time)
@Mapper(componentModel = "spring")
public interface ProductMapper {
    ProductResponse toResponse(Product product);
    Product toEntity(ProductRequest request);
}

// Method 3: Manual mapping in service
ProductResponse response = new ProductResponse();
response.setId(product.getId());
response.setName(product.getName());
// ... (verbose but explicit)
```

---

## Jackson Field Naming and Customization

### Default behavior: Java field names become JSON keys

```java
// Java field:
private String firstName;

// JSON:
{ "firstName": "Alice" }
```

### Rename a JSON field with @JsonProperty

```java
@JsonProperty("full_name")
private String firstName;

// JSON:
{ "full_name": "Alice" }
```

### Exclude a field from JSON

```java
@JsonIgnore
private String internalCode;   // never serialized/deserialized
```

### Include only non-null fields (configured globally in application.properties)

```properties
spring.jackson.default-property-inclusion=non_null
```

```java
// If description is null, it won't appear in the JSON response
private String description;  // null → not included in response
```

### Custom date format

```java
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
private LocalDateTime createdAt;

// JSON: "createdAt": "2026-03-04 10:30:00"
```

---

## Content Negotiation

Spring Web supports content negotiation — responding in different formats based on the client's `Accept` header.

```http
Accept: application/json     → Spring returns JSON (default)
Accept: application/xml      → Spring returns XML (need jackson-dataformat-xml dependency)
```

For REST APIs, JSON is almost always the only format. XML is legacy.

---

## Complete Request-Response Flow Example

```
Client sends:
POST /api/products
Content-Type: application/json
Body: {"name": "Gaming Mouse", "price": 49.99, "stockQuantity": 200, "categoryId": 1}

1. Spring reads Content-Type: application/json
2. @RequestBody + Jackson: JSON body → ProductRequest object
3. @Valid: validates ProductRequest fields
4. Controller calls productService.create(request)
5. Service builds Product entity, saves to MySQL
6. Service calls ProductResponse.from(savedProduct)
7. Returns ProductResponse object
8. Controller wraps in ResponseEntity.status(201).body(response)
9. Jackson: ProductResponse → JSON string
10. Spring writes response:
    HTTP/1.1 201 Created
    Content-Type: application/json
    Body: {"id": 5, "name": "Gaming Mouse", "price": 49.99, ...}
```

---

## Common Mistakes

```java
// ❌ Forget Content-Type header in request
// → Spring can't deserialize body → 415 Unsupported Media Type

// ❌ Return the entity directly
public Product getById(Long id) { return repository.findById(id).get(); }
// → LazyInitializationException or infinite recursion in JSON

// ❌ Use @ResponseStatus and return null
@ResponseStatus(HttpStatus.CREATED)
public void create(...) { ... }
// → Returns 201 but with empty body — confusing for clients

// ✅ Always return a meaningful body with status
public ResponseEntity<ProductResponse> create(...) {
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
}
```
