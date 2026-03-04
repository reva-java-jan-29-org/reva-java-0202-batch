# Module 01 — Spring Web & MVC Architecture

## What is Spring Web?

`spring-boot-starter-web` is a single dependency that bundles everything you need to build web applications and REST APIs:

| Included | Purpose |
|----------|---------|
| **Embedded Tomcat** | HTTP server, no separate installation needed |
| **Spring MVC** | Web framework — routing, controllers, dispatching |
| **Jackson** | JSON library — serializes Java objects to JSON and back |
| **Bean Validation** | Validation framework (`@Valid`, `@NotBlank`, etc.) |

When you add this dependency and run `@SpringBootApplication`, you get a fully functional web server on **port 8080** in seconds.

---

## Spring MVC: The Core Pattern

MVC = **Model - View - Controller**

For REST APIs:
- **Model** = your domain objects (entities + DTOs)
- **View** = JSON (Jackson converts your objects to JSON)
- **Controller** = `@RestController` — handles incoming requests and calls the service

---

## The DispatcherServlet — Front Controller Pattern

The heart of Spring MVC is the **DispatcherServlet**. Every HTTP request to your application goes through it first.

```
Client Request
     │
     ▼
DispatcherServlet ──► HandlerMapping ──► finds which @Controller handles this URL
     │
     ▼
     Controller Method ──► calls Service ──► calls Repository
     │
     ▼
     Returns Java object (e.g., ProductResponse)
     │
     ▼
Jackson Serializer ──► converts to JSON string
     │
     ▼
HTTP Response (200 OK, body = JSON)
     │
     ▼
   Client
```

**HandlerMapping** is the component that maps URLs to controller methods:
- `GET /api/products` → `ProductController.getProducts()`
- `POST /api/products` → `ProductController.createProduct()`

You configure these mappings with annotations (`@GetMapping`, `@PostMapping`, etc.).

---

## @RestController vs @Controller

| `@Controller` | `@RestController` |
|--------------|------------------|
| Returns view names (HTML templates like Thymeleaf) | Returns data (objects converted to JSON) |
| Used in traditional web apps | Used in REST APIs |
| Needs `@ResponseBody` on each method | `@ResponseBody` is automatic |
| `return "home"` → renders `home.html` | `return product` → renders `{"id":1,"name":"MacBook"}` |

```java
// Traditional web controller — returns HTML view
@Controller
public class WebController {
    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("user", "Alice");
        return "home";  // → looks for templates/home.html
    }
}

// REST API controller — returns JSON data
@RestController
public class ApiController {
    @GetMapping("/api/products")
    public List<ProductResponse> getAll() {
        return List.of(...);  // Jackson converts this to JSON array
    }
}
```

---

## How Jackson Serialization Works

**Jackson** is the JSON library bundled with Spring Boot. It automatically handles converting between Java objects and JSON.

### Java Object → JSON (Serialization — when you RETURN data)

```java
// Java object
ProductResponse response = ProductResponse.builder()
    .id(1L)
    .name("MacBook Pro")
    .price(new BigDecimal("1999.99"))
    .build();

// Jackson automatically converts this to:
// {
//   "id": 1,
//   "name": "MacBook Pro",
//   "price": 1999.99
// }
```

### JSON → Java Object (Deserialization — when you RECEIVE data)

```java
// Incoming JSON in request body:
// { "name": "MacBook Pro", "price": 1999.99, "categoryId": 1 }

// Spring + Jackson converts this to:
@PostMapping
public ResponseEntity<ProductResponse> create(@RequestBody ProductRequest request) {
    // request.getName()  → "MacBook Pro"
    // request.getPrice() → 1999.99
}
```

### Jackson Configuration in application.properties

```properties
# Don't include null fields in the JSON output
spring.jackson.default-property-inclusion=non_null

# Pretty-print (indent) JSON (for readability during dev)
spring.jackson.serialization.indent-output=true
```

---

## Auto-configuration Magic

`@SpringBootApplication` enables Spring Boot's auto-configuration. Based on what's on the classpath, Spring Boot automatically sets up:

| If you have... | Spring Boot auto-configures... |
|---------------|-------------------------------|
| `spring-boot-starter-web` | Embedded Tomcat, DispatcherServlet, Jackson |
| `spring-boot-starter-data-jpa` | EntityManagerFactory, JpaRepositories |
| `mysql-connector-j` | DataSource connected to MySQL |
| `spring-boot-starter-validation` | Bean Validation with Hibernate Validator |

You rarely write any configuration beans manually. That's the power of Spring Boot's "opinionated defaults."

---

## Request Lifecycle — Step by Step

Let's trace what happens when `POST /api/products` is called:

```
1. Client sends:
   POST /api/products
   Content-Type: application/json
   Body: {"name": "MacBook", "price": 1999.99, "categoryId": 1}

2. Tomcat receives the TCP connection, parses the HTTP request.

3. DispatcherServlet receives the parsed request.

4. HandlerMapping finds: ProductController.createProduct() handles POST /api/products.

5. @RequestBody + Jackson:
   - Spring reads the JSON body.
   - Jackson deserializes it into a ProductRequest object.

6. Bean Validation (@Valid):
   - Spring validates the ProductRequest fields.
   - If validation fails → MethodArgumentNotValidException → GlobalExceptionHandler handles it.

7. createProduct() method executes:
   - Calls productService.create(request).
   - Service validates business rules, calls repository.
   - Repository executes INSERT SQL into MySQL.

8. ProductResponse is returned from createProduct().

9. Jackson serializes ProductResponse → JSON string.

10. DispatcherServlet writes:
    HTTP/1.1 201 Created
    Content-Type: application/json
    Body: {"id": 5, "name": "MacBook", ...}

11. Client receives the response.
```

---

## Port and Context Path Configuration

```properties
# Change the default port (default is 8080)
server.port=9090

# Add a prefix to all endpoints
server.servlet.context-path=/v1

# With context-path = /v1:
# GET /v1/api/products   (not /api/products)
```

During development, keep the defaults (`8080`, no context path) for simplicity.
