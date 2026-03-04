# Module 03 — Controllers and Routing

## @RestController

```java
@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    ...
}
```

`@RestController` is a combination of two annotations:
- `@Controller` — marks this as a Spring MVC controller (Spring manages it as a bean)
- `@ResponseBody` — tells Spring to serialize every return value to JSON and write it to the HTTP response body

Without `@ResponseBody`, Spring would try to resolve the return value as a **view name** (HTML template name).

---

## HTTP Method Mapping Annotations

Spring provides shorthand annotations for each HTTP method:

| Annotation | HTTP Method | Equivalent Long Form |
|-----------|-------------|---------------------|
| `@GetMapping` | GET | `@RequestMapping(method = RequestMethod.GET)` |
| `@PostMapping` | POST | `@RequestMapping(method = RequestMethod.POST)` |
| `@PutMapping` | PUT | `@RequestMapping(method = RequestMethod.PUT)` |
| `@PatchMapping` | PATCH | `@RequestMapping(method = RequestMethod.PATCH)` |
| `@DeleteMapping` | DELETE | `@RequestMapping(method = RequestMethod.DELETE)` |

```java
@RestController
@RequestMapping("/api/products")   // ← class-level prefix
public class ProductController {

    @GetMapping              // → handles GET /api/products
    public List<ProductResponse> getAll() { ... }

    @GetMapping("/{id}")     // → handles GET /api/products/{id}
    public ProductResponse getById(@PathVariable Long id) { ... }

    @PostMapping             // → handles POST /api/products
    public ResponseEntity<ProductResponse> create(...) { ... }

    @PutMapping("/{id}")     // → handles PUT /api/products/{id}
    public ResponseEntity<ProductResponse> update(...) { ... }

    @PatchMapping("/{id}/stock")  // → handles PATCH /api/products/{id}/stock
    public ResponseEntity<ProductResponse> updateStock(...) { ... }

    @DeleteMapping("/{id}")  // → handles DELETE /api/products/{id}
    public ResponseEntity<Void> delete(...) { ... }
}
```

---

## @PathVariable — Extracting URL Segments

`@PathVariable` extracts a value from the URL path.

```java
// URL: GET /api/products/42
@GetMapping("/{id}")
public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
    // id = 42 (Spring auto-converts String "42" to Long)
    return ResponseEntity.ok(productService.findById(id));
}
```

### Name mismatch

If the variable name in the URL doesn't match the parameter name, specify it:
```java
@GetMapping("/{productId}")
public ProductResponse getById(@PathVariable("productId") Long id) { ... }
```

### Multiple path variables

```java
// URL: GET /api/categories/2/products/5
@GetMapping("/categories/{categoryId}/products/{productId}")
public ProductResponse getProduct(
        @PathVariable Long categoryId,
        @PathVariable Long productId) { ... }
```

### Type safety

Spring automatically converts path variables to the declared type:
- `/products/42` → `Long id = 42L` ✅
- `/products/abc` → Spring returns `400 Bad Request` (can't convert "abc" to Long) ✅

---

## @RequestParam — Extracting Query Parameters

Query parameters appear after `?` in the URL: `/products?name=laptop&minPrice=500`

```java
// URL: GET /api/products?name=laptop
@GetMapping
public List<ProductResponse> getProducts(
        @RequestParam(required = false) String name) {
    if (name != null) {
        return productService.search(name);
    }
    return productService.findAll();
}
```

### Options

```java
// Required parameter (throws 400 if missing)
@RequestParam String name

// Optional parameter (null if missing)
@RequestParam(required = false) String name

// Optional with default value
@RequestParam(defaultValue = "10") int pageSize

// Map a different URL param name to a Java variable
@RequestParam("category_id") Long categoryId
```

### Differences: @PathVariable vs @RequestParam

| | `@PathVariable` | `@RequestParam` |
|--|----------------|----------------|
| Location in URL | `/products/{id}` | `/products?id=5` |
| Use for | Identifying a specific resource | Filtering, sorting, pagination |
| Required? | Always (part of the path) | Often optional |
| Example | `/products/42` | `/products?name=laptop` |
| REST convention | `/api/products/42` ← preferred for ID | `/api/products?search=laptop` |

---

## Filtering with @RequestParam — Full Example

From `ProductController.java`:

```java
/**
 * GET /api/products
 * GET /api/products?name=laptop
 * GET /api/products?categoryId=1
 * GET /api/products?minPrice=100&maxPrice=500
 *
 * All params are optional. Logic picks the right service method.
 */
@GetMapping
public ResponseEntity<List<ProductResponse>> getProducts(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice) {

    if (name != null) {
        return ResponseEntity.ok(productService.search(name));
    }
    if (categoryId != null) {
        return ResponseEntity.ok(productService.findByCategory(categoryId));
    }
    if (minPrice != null && maxPrice != null) {
        return ResponseEntity.ok(productService.findByPriceRange(minPrice, maxPrice));
    }
    return ResponseEntity.ok(productService.findAll());
}
```

In production, you'd use a dedicated filter object or Spring's `Specification` API to avoid this if-else chain. But for teaching purposes, this is clear and easy to understand.

---

## @RequestMapping at Class Level vs Method Level

```java
@RestController
@RequestMapping("/api")           // ← class-level prefix: applies to ALL methods
public class ProductController {

    @GetMapping("/products")      // → GET /api/products
    public List<ProductResponse> getAll() { ... }

    @GetMapping("/products/{id}") // → GET /api/products/{id}
    public ProductResponse getById(@PathVariable Long id) { ... }
}
```

Same as:
```java
@RestController
@RequestMapping("/api/products")  // ← more specific class-level prefix
public class ProductController {

    @GetMapping              // → GET /api/products
    public List<ProductResponse> getAll() { ... }

    @GetMapping("/{id}")     // → GET /api/products/{id}
    public ProductResponse getById(@PathVariable Long id) { ... }
}
```

Both approaches are correct. Grouping by resource (`/api/products` at class level) is cleaner.

---

## Multiple Controller Classes

Spring allows multiple controllers. Each controller typically handles one resource:

```
CategoryController → handles /api/categories/**
ProductController  → handles /api/products/**
OrderController    → handles /api/orders/**
```

This follows the **Single Responsibility Principle**. Each class has one job.

---

## Returning Different HTTP Status Codes

By default, a successful method returns `200 OK`. But for REST APIs, you often need different codes:

```java
// 200 OK — default (can use ResponseEntity.ok() or just return the object)
@GetMapping("/{id}")
public ProductResponse getById(@PathVariable Long id) {
    return productService.findById(id);
}

// 201 Created — use ResponseEntity.status(HttpStatus.CREATED)
@PostMapping
public ResponseEntity<ProductResponse> create(@RequestBody ProductRequest req) {
    ProductResponse created = productService.create(req);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
}

// 204 No Content — use ResponseEntity.noContent()
@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable Long id) {
    productService.delete(id);
    return ResponseEntity.noContent().build();
}
```

See Module 04 for a deep dive on `ResponseEntity`.

---

## Useful Testing Commands (curl)

```bash
# GET all categories
curl http://localhost:8080/api/categories

# GET one product by ID
curl http://localhost:8080/api/products/1

# GET products filtered by name
curl "http://localhost:8080/api/products?name=mac"

# GET products in category 1
curl "http://localhost:8080/api/products?categoryId=1"

# GET products in price range
curl "http://localhost:8080/api/products?minPrice=100&maxPrice=500"

# DELETE product 3
curl -X DELETE http://localhost:8080/api/products/3

# PATCH stock
curl -X PATCH "http://localhost:8080/api/products/1/stock?quantity=100"
```
