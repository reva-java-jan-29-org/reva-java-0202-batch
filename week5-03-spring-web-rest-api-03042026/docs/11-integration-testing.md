# 11 — Integration Testing: REST APIs, MockMvc & Database Interactions

## Navigation
← [10 Spring Security & JWT](10-spring-security-jwt.md)

---

## Table of Contents
1. [Unit vs Integration Testing — What's the Difference?](#1-unit-vs-integration-testing)
2. [Testing Pyramid & Strategy](#2-testing-pyramid--strategy)
3. [Spring Boot Test Slices — Choosing the Right Tool](#3-spring-boot-test-slices)
4. [Test Configuration — application-test.properties + H2](#4-test-configuration)
5. [MockMvc — Testing the Web Layer](#5-mockmvc--testing-the-web-layer)
   - 5a. [@WebMvcTest — Web Layer Slice (No DB)](#5a-webmvctest--web-layer-slice-no-db)
   - 5b. [@SpringBootTest + MockMvc — Full Context](#5b-springboottest--mockmvc--full-context)
   - 5c. [Handling Spring Security in Tests](#5c-handling-spring-security-in-tests)
   - 5d. [Testing All HTTP Verbs](#5d-testing-all-http-verbs)
   - 5e. [Testing Validation Errors](#5e-testing-validation-errors)
   - 5f. [Testing Exception / Error Responses](#5f-testing-exception--error-responses)
6. [RestTemplate — Full HTTP Integration Tests](#6-resttemplate--full-http-integration-tests)
7. [Database Interaction Testing — @DataJpaTest](#7-database-interaction-testing--datajpatest)
8. [Summary Cheat Sheet](#8-summary-cheat-sheet)
9. [Interview Questions](#9-interview-questions)

---

## 1. Unit vs Integration Testing

```
Unit Test                           Integration Test
─────────────────────────────       ──────────────────────────────────────
Tests ONE class in isolation.       Tests multiple components TOGETHER.
All dependencies are mocked.        Real Spring context, real DB (or H2).
Fast (~milliseconds).               Slower (~seconds), but catches real bugs.
Great for business logic.           Great for HTTP layer, DB queries, wiring.
```

**Example: the same `create product` scenario**

```
Unit Test:
  ProductService.create() is called with a mock ProductRepository and
  mock CategoryRepository. We verify the save() was called.
  → Confirms the logic, but not whether the query actually works.

Integration Test:
  POST /api/products goes through the real filter chain, real controller,
  real service, hits H2 in-memory DB, and returns a real JSON response.
  → Confirms the whole stack works end-to-end.
```

---

## 2. Testing Pyramid & Strategy

```
            ┌────────────┐
            │  E2E Tests │  ← fewest, slowest, most expensive
            │  (Postman) │
           ─┴────────────┴─
          ┌──────────────────┐
          │  Integration     │  ← @SpringBootTest, @DataJpaTest, MockMvc
          │  Tests           │
         ─┴──────────────────┴─
        ┌──────────────────────────┐
        │  Unit Tests              │  ← most, fastest (@ExtendWith(Mockito))
        │  (Mockito / JUnit 5)     │
        └──────────────────────────┘
```

**Strategy for this project:**

| What to test                     | Tool to use                             |
|----------------------------------|-----------------------------------------|
| Service business logic           | Unit test with Mockito                  |
| Repository custom queries        | `@DataJpaTest` + H2                     |
| Controller layer (HTTP → JSON)   | `@WebMvcTest` + MockMvc (mocked service)|
| Full stack (HTTP → DB)           | `@SpringBootTest` + MockMvc or TestRestTemplate |

---

## 3. Spring Boot Test Slices

Spring Boot provides **test slices** — partial application contexts that load only what is needed for a specific layer. This makes tests faster and focused.

```
@WebMvcTest
  → Loads: controllers, filters, security config, Jackson
  → Does NOT load: services, repositories, database
  → Use for: testing HTTP routing, serialization, security rules, validation

@DataJpaTest
  → Loads: JPA entities, repositories, Hibernate, DataSource (H2)
  → Does NOT load: controllers, services
  → Use for: testing custom queries, derived methods, relationships

@SpringBootTest
  → Loads: ENTIRE application context (like running the app)
  → Use for: full end-to-end tests, or when slices don't cover your need
  → Two modes:
      webEnvironment = MOCK        → fake servlet, use MockMvc
      webEnvironment = RANDOM_PORT → real HTTP server, use TestRestTemplate
```

---

## 4. Test Configuration

### Step 1 — Create `src/test/resources/application-test.properties`

This file overrides `application.properties` ONLY during tests, switching from MySQL to H2.

```properties
# ── Use H2 in-memory database instead of MySQL ─────────────────────────────
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# ── H2 dialect (not MySQL) ──────────────────────────────────────────────────
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect

# ── Create tables fresh on each test run ────────────────────────────────────
spring.jpa.hibernate.ddl-auto=create-drop

# ── Suppress SQL noise in test output ───────────────────────────────────────
spring.jpa.show-sql=false

# ── Dummy JWT secret (same algorithm, just a test value) ────────────────────
app.jwt.secret=dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLW9ubHkx
app.jwt.expiration-ms=86400000
```

**Why `MODE=MySQL`?** H2 can mimic MySQL syntax (e.g. `AUTO_INCREMENT`) so your entity DDL works without changes.

### Step 2 — Activate the test profile in each test class

Add this annotation to your test class:

```java
@ActiveProfiles("test")
```

This tells Spring to load `application-test.properties` on top of `application.properties` (test values override main values).

### pom.xml verification — H2 is already in test scope

```xml
<!-- Already in pom.xml — no changes needed -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>   <!-- only available during mvn test -->
</dependency>
```

---

## 5. MockMvc — Testing the Web Layer

**MockMvc** simulates HTTP requests inside Spring's DispatcherServlet — without starting a real TCP server. It gives you full control to:
- Set headers, path variables, query params, request body
- Assert status code, response headers, JSON body fields
- Test error responses, security rules, and validation

### MockMvc Request Flow

```
Test → MockMvc.perform(request)
           ↓
  DispatcherServlet (real, in-memory)
           ↓
  Security Filter Chain (real)
           ↓
  Controller (real)
           ↓
  Service (mocked in @WebMvcTest, real in @SpringBootTest)
           ↓
MockMvc captures the response → you assert on it
```

---

### 5a. @WebMvcTest — Web Layer Slice (No DB)

Use this when you want to test the **controller + security** in isolation. The service is mocked with Mockito.

**File:** `src/test/java/com/training/product/ProductControllerTest.java`

```java
package com.training.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @WebMvcTest(ProductController.class) — loads ONLY the web layer for ProductController
// MockBeans are required for all beans that ProductController depends on
@WebMvcTest(ProductController.class)
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;   // Jackson: object ↔ JSON

    // ProductController depends on ProductService — mock it
    @MockBean
    private ProductService productService;

    // ─── Test: GET /api/products → 200 OK ─────────────────────────────────────

    @Test
    @WithMockUser          // any authenticated user can GET (see SecurityConfig)
    void getProducts_returnsListOf200() throws Exception {

        // ARRANGE: define what the mocked service returns
        ProductResponse laptop = ProductResponse.builder()
                .id(1L).name("Laptop Pro").price(new BigDecimal("1299.99"))
                .stockQuantity(10).categoryId(1L).categoryName("Electronics")
                .build();

        when(productService.findAll()).thenReturn(List.of(laptop));

        // ACT + ASSERT
        mockMvc.perform(get("/api/products"))          // HTTP GET /api/products
                .andExpect(status().isOk())            // response status = 200
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Laptop Pro"))
                .andExpect(jsonPath("$[0].price").value(1299.99));
    }

    // ─── Test: GET /api/products/{id} → 200 OK ────────────────────────────────

    @Test
    @WithMockUser
    void getProductById_whenExists_returns200() throws Exception {

        ProductResponse response = ProductResponse.builder()
                .id(5L).name("Wireless Mouse").price(new BigDecimal("29.99"))
                .stockQuantity(100).categoryId(2L).categoryName("Accessories")
                .build();

        when(productService.findById(5L)).thenReturn(response);

        mockMvc.perform(get("/api/products/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Wireless Mouse"));
    }

    // ─── Test: GET /api/products/{id} → 404 Not Found ─────────────────────────

    @Test
    @WithMockUser
    void getProductById_whenNotFound_returns404() throws Exception {

        when(productService.findById(999L))
                .thenThrow(new ResourceNotFoundException("Product", 999L));

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Product with id 999 not found"));
    }

    // ─── Test: GET /api/products?name=laptop (search) ─────────────────────────

    @Test
    @WithMockUser
    void searchProducts_byName_returns200() throws Exception {

        ProductResponse result = ProductResponse.builder()
                .id(1L).name("Laptop Pro").price(new BigDecimal("1299.99"))
                .stockQuantity(10).categoryId(1L).categoryName("Electronics")
                .build();

        when(productService.search("laptop")).thenReturn(List.of(result));

        mockMvc.perform(get("/api/products").param("name", "laptop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Laptop Pro"));
    }

    // ─── Test: POST /api/products → 201 Created ───────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")   // POST requires ADMIN role (see SecurityConfig)
    void createProduct_withValidRequest_returns201() throws Exception {

        ProductRequest request = new ProductRequest();
        request.setName("Gaming Keyboard");
        request.setDescription("Mechanical RGB keyboard");
        request.setPrice(new BigDecimal("89.99"));
        request.setStockQuantity(50);
        request.setCategoryId(1L);

        ProductResponse saved = ProductResponse.builder()
                .id(10L).name("Gaming Keyboard").price(new BigDecimal("89.99"))
                .stockQuantity(50).categoryId(1L).categoryName("Electronics")
                .build();

        when(productService.create(any(ProductRequest.class))).thenReturn(saved);

        mockMvc.perform(post("/api/products")
                        .with(csrf())                              // CSRF token required
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())                   // 201
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Gaming Keyboard"));
    }

    // ─── Test: POST without ADMIN role → 403 Forbidden ────────────────────────

    @Test
    @WithMockUser(roles = "USER")    // USER role cannot POST
    void createProduct_withoutAdminRole_returns403() throws Exception {

        ProductRequest request = new ProductRequest();
        request.setName("Test Product");
        request.setPrice(new BigDecimal("9.99"));
        request.setStockQuantity(1);
        request.setCategoryId(1L);

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());                // 403
    }

    // ─── Test: DELETE /api/products/{id} → 204 No Content ────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProduct_whenExists_returns204() throws Exception {

        // delete() returns void — no need to mock a return value
        mockMvc.perform(delete("/api/products/1").with(csrf()))
                .andExpect(status().isNoContent());                // 204
    }

    // ─── Test: PATCH /api/products/{id}/stock → 200 OK ───────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateStock_withValidQuantity_returns200() throws Exception {

        ProductResponse updated = ProductResponse.builder()
                .id(1L).name("Laptop Pro").price(new BigDecimal("1299.99"))
                .stockQuantity(200).categoryId(1L).categoryName("Electronics")
                .build();

        when(productService.updateStock(eq(1L), eq(200))).thenReturn(updated);

        mockMvc.perform(patch("/api/products/1/stock")
                        .with(csrf())
                        .param("quantity", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(200));
    }
}
```

---

### 5b. @SpringBootTest + MockMvc — Full Context

When you need the **real service and real DB** but still want MockMvc (no TCP port):

**File:** `src/test/java/com/training/product/ProductControllerIntegrationTest.java`

```java
package com.training.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.category.Category;
import com.training.category.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @SpringBootTest — boots the full context (controller + service + repo + DB)
// @AutoConfigureMockMvc — auto-creates MockMvc bean wired into the full context
// @Transactional — rolls back DB changes after EACH test method
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;

    private Category savedCategory;

    // @BeforeEach runs BEFORE every test method
    // We seed the H2 DB with a category so products can be linked to it
    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        savedCategory = categoryRepository.save(
            Category.builder()
                .name("Electronics")
                .description("Electronic devices")
                .build()
        );
    }

    // ─── Full stack: POST then GET ─────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAndGetProduct_fullStack() throws Exception {

        // Build a valid request JSON
        ProductRequest request = new ProductRequest();
        request.setName("MacBook Air");
        request.setDescription("Apple M2 chip");
        request.setPrice(new BigDecimal("1199.99"));
        request.setStockQuantity(25);
        request.setCategoryId(savedCategory.getId());

        // POST — create product, capture the response
        String responseJson = mockMvc.perform(
                post("/api/products")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("MacBook Air"))
                .andExpect(jsonPath("$.categoryName").value("Electronics"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract the id from the response to use in the next request
        ProductResponse created = objectMapper.readValue(responseJson, ProductResponse.class);

        // GET — fetch the product we just created
        mockMvc.perform(get("/api/products/" + created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("MacBook Air"))
                .andExpect(jsonPath("$.price").value(1199.99));
    }

    // ─── Full stack: create then delete ───────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProduct_thenGetReturns404() throws Exception {

        // Create a product directly via repository (faster than going through HTTP)
        Product product = productRepository.save(
            Product.builder()
                .name("Old Laptop")
                .price(new BigDecimal("499.99"))
                .stockQuantity(5)
                .category(savedCategory)
                .build()
        );

        // DELETE via HTTP
        mockMvc.perform(delete("/api/products/" + product.getId()).with(csrf()))
                .andExpect(status().isNoContent());

        // GET same id — must return 404 now
        mockMvc.perform(get("/api/products/" + product.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ─── Full stack: PUT (full update) ────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProduct_changesName() throws Exception {

        Product product = productRepository.save(
            Product.builder()
                .name("Budget Phone")
                .price(new BigDecimal("199.99"))
                .stockQuantity(30)
                .category(savedCategory)
                .build()
        );

        ProductRequest updateRequest = new ProductRequest();
        updateRequest.setName("Premium Phone");
        updateRequest.setDescription("Updated description");
        updateRequest.setPrice(new BigDecimal("599.99"));
        updateRequest.setStockQuantity(15);
        updateRequest.setCategoryId(savedCategory.getId());

        mockMvc.perform(put("/api/products/" + product.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Premium Phone"))
                .andExpect(jsonPath("$.price").value(599.99));
    }
}
```

---

### 5c. Handling Spring Security in Tests

This project uses **JWT + Spring Security**, which means tests must deal with authentication. Here are the tools available:

```
@WithMockUser                    — creates a fake authenticated user in the SecurityContext
@WithMockUser(roles = "ADMIN")   — creates a fake user with ROLE_ADMIN
@WithAnonymousUser               — simulates an unauthenticated request
.with(csrf())                    — adds a CSRF token to mutating requests (POST/PUT/DELETE/PATCH)
.with(SecurityMockMvcRequestPostProcessors.user("username")) — inline user setup
```

**Why do we need `.with(csrf())`?**

Even though we disabled CSRF in `SecurityConfig`, Spring Security test framework still includes CSRF protection by default when using `@WebMvcTest`. The `.with(csrf())` post-processor injects a valid token to bypass this.

**Why `@WithMockUser` and not a real JWT?**

In unit/integration tests we don't want to depend on JWT token generation. `@WithMockUser` directly injects a mock principal into Spring Security's `SecurityContext`, completely bypassing the JWT filter. This makes tests simpler and more focused.

```java
// For public endpoints (GET /api/products) — any user works
@WithMockUser

// For admin-only endpoints (POST, PUT, DELETE)
@WithMockUser(roles = "ADMIN")

// To test that unauthenticated requests are rejected
// Don't add @WithMockUser — Spring will see no principal → 401/403
```

---

### 5d. Testing All HTTP Verbs

Quick reference for MockMvc request builders:

```java
// GET with path variable
mockMvc.perform(get("/api/products/{id}", 5L))

// GET with query parameters
mockMvc.perform(get("/api/products").param("name", "laptop"))
mockMvc.perform(get("/api/products").param("minPrice", "100").param("maxPrice", "500"))

// POST with JSON body
mockMvc.perform(post("/api/products")
    .with(csrf())
    .contentType(MediaType.APPLICATION_JSON)
    .content(objectMapper.writeValueAsString(requestObject)))

// PUT with path variable + JSON body
mockMvc.perform(put("/api/products/{id}", 5L)
    .with(csrf())
    .contentType(MediaType.APPLICATION_JSON)
    .content(objectMapper.writeValueAsString(requestObject)))

// PATCH with path variable + query param
mockMvc.perform(patch("/api/products/{id}/stock", 5L)
    .with(csrf())
    .param("quantity", "100"))

// DELETE with path variable
mockMvc.perform(delete("/api/products/{id}", 5L).with(csrf()))
```

**MockMvc ResultMatchers — what you can assert:**

```java
// Status codes
.andExpect(status().isOk())             // 200
.andExpect(status().isCreated())        // 201
.andExpect(status().isNoContent())      // 204
.andExpect(status().isBadRequest())     // 400
.andExpect(status().isUnauthorized())   // 401
.andExpect(status().isForbidden())      // 403
.andExpect(status().isNotFound())       // 404
.andExpect(status().isConflict())       // 409

// Content type
.andExpect(content().contentType(MediaType.APPLICATION_JSON))

// JSON field assertions using JsonPath
.andExpect(jsonPath("$.id").value(1))
.andExpect(jsonPath("$.name").value("Laptop Pro"))
.andExpect(jsonPath("$.price").value(1299.99))
.andExpect(jsonPath("$[0].name").value("First item in array"))
.andExpect(jsonPath("$", hasSize(3)))           // array has 3 elements
.andExpect(jsonPath("$.name").exists())         // field is present
.andExpect(jsonPath("$.deleted").doesNotExist()) // field is absent

// Print full request/response for debugging
.andDo(print())
```

---

### 5e. Testing Validation Errors

```java
@Test
@WithMockUser(roles = "ADMIN")
void createProduct_withBlankName_returns400() throws Exception {

    ProductRequest invalidRequest = new ProductRequest();
    invalidRequest.setName("");                    // @NotBlank violation
    invalidRequest.setPrice(new BigDecimal("9.99"));
    invalidRequest.setStockQuantity(10);
    invalidRequest.setCategoryId(1L);

    mockMvc.perform(post("/api/products")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())    // 400
            // GlobalExceptionHandler puts errors in "validationErrors" array
            .andExpect(jsonPath("$.validationErrors").isArray())
            .andExpect(jsonPath("$.message")
                .value("Validation failed. Please check the request body."));
}

@Test
@WithMockUser(roles = "ADMIN")
void createProduct_withNegativePrice_returns400() throws Exception {

    ProductRequest invalidRequest = new ProductRequest();
    invalidRequest.setName("Valid Name");
    invalidRequest.setPrice(new BigDecimal("-5.00"));   // @DecimalMin("0.01") violation
    invalidRequest.setStockQuantity(10);
    invalidRequest.setCategoryId(1L);

    mockMvc.perform(post("/api/products")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.validationErrors[0]")
                .value("price: Price must be greater than 0"));
}
```

---

### 5f. Testing Exception / Error Responses

```java
@Test
@WithMockUser
void getProductById_whenNotFound_returnsErrorResponse() throws Exception {

    when(productService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Product", 999L));

    mockMvc.perform(get("/api/products/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.path").value("/api/products/999"))
            .andExpect(jsonPath("$.timestamp").exists());
}
```

---

## 6. RestTemplate — Full HTTP Integration Tests

`TestRestTemplate` starts a **real embedded Tomcat server** on a random port and makes actual HTTP calls. This tests everything including serialization, filter chain, and HTTP transport.

Use this for testing the **complete end-to-end HTTP contract** of your API.

**File:** `src/test/java/com/training/product/ProductApiIntegrationTest.java`

```java
package com.training.product;

import com.training.category.Category;
import com.training.category.CategoryRepository;
import com.training.controllers.AuthController;
import com.training.dto.LoginRequest;
import com.training.dto.AuthResponse;
import com.training.dto.RegisterRequest;
import com.training.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// webEnvironment = RANDOM_PORT — starts a real Tomcat on a random free port
// This avoids port conflicts when running multiple test classes in parallel
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProductApiIntegrationTest {

    // @LocalServerPort injects the random port the server started on (e.g., 54231)
    @LocalServerPort
    private int port;

    // TestRestTemplate — a test-friendly wrapper around RestTemplate
    // It does NOT throw exceptions on 4xx/5xx (unlike plain RestTemplate)
    // This lets you assert on error responses directly
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private String baseUrl;
    private Category savedCategory;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;

        productRepository.deleteAll();
        categoryRepository.deleteAll();

        savedCategory = categoryRepository.save(
            Category.builder()
                .name("Electronics")
                .description("Electronic devices")
                .build()
        );
    }

    // ─── Helper: build URL ─────────────────────────────────────────────────────

    private String url(String path) {
        return baseUrl + path;
    }

    // ─── Helper: get JWT token for admin ──────────────────────────────────────

    private String getAdminJwtToken() {
        // Register admin if not exists
        RegisterRequest register = new RegisterRequest();
        register.setUsername("testadmin");
        register.setPassword("Admin123!");
        register.setRole(Role.ADMIN);
        restTemplate.postForEntity(url("/api/auth/register"), register, Void.class);

        // Login to get JWT
        LoginRequest login = new LoginRequest();
        login.setUsername("testadmin");
        login.setPassword("Admin123!");

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
            url("/api/auth/login"), login, AuthResponse.class);

        return response.getBody().getToken();
    }

    // ─── Helper: create Authorization headers with Bearer JWT ─────────────────

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // ─── Test: GET /api/products (public endpoint) ────────────────────────────

    @Test
    void getProducts_publicEndpoint_returns200() {

        // No token needed — GET is public per SecurityConfig
        ResponseEntity<List<ProductResponse>> response = restTemplate.exchange(
            url("/api/products"),
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<ProductResponse>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();   // no products seeded yet
    }

    // ─── Test: POST /api/products (requires ADMIN JWT) ────────────────────────

    @Test
    void createProduct_withAdminJwt_returns201() {

        String token = getAdminJwtToken();

        ProductRequest request = new ProductRequest();
        request.setName("MacBook Pro");
        request.setDescription("Apple M3");
        request.setPrice(new BigDecimal("1999.99"));
        request.setStockQuantity(10);
        request.setCategoryId(savedCategory.getId());

        HttpEntity<ProductRequest> entity = new HttpEntity<>(request, authHeaders(token));

        ResponseEntity<ProductResponse> response = restTemplate.exchange(
            url("/api/products"),
            HttpMethod.POST,
            entity,
            ProductResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("MacBook Pro");
        assertThat(response.getBody().getId()).isPositive();
    }

    // ─── Test: POST without token → 401/403 ───────────────────────────────────

    @Test
    void createProduct_withoutToken_returns401or403() {

        ProductRequest request = new ProductRequest();
        request.setName("Unauthorized Product");
        request.setPrice(new BigDecimal("9.99"));
        request.setStockQuantity(1);
        request.setCategoryId(savedCategory.getId());

        // No auth header
        HttpEntity<ProductRequest> entity = new HttpEntity<>(request);

        ResponseEntity<String> response = restTemplate.exchange(
            url("/api/products"),
            HttpMethod.POST,
            entity,
            String.class
        );

        // Spring Security returns 403 for unauthenticated requests when CSRF is off
        assertThat(response.getStatusCode().value()).isIn(401, 403);
    }

    // ─── Test: Full CRUD lifecycle with RestTemplate ───────────────────────────

    @Test
    void fullCrudLifecycle() {

        String token = getAdminJwtToken();
        HttpHeaders headers = authHeaders(token);

        // ── Step 1: CREATE ────────────────────────────────────────────────────
        ProductRequest createReq = new ProductRequest();
        createReq.setName("Gaming Chair");
        createReq.setPrice(new BigDecimal("299.99"));
        createReq.setStockQuantity(50);
        createReq.setCategoryId(savedCategory.getId());

        ResponseEntity<ProductResponse> created = restTemplate.exchange(
            url("/api/products"), HttpMethod.POST,
            new HttpEntity<>(createReq, headers), ProductResponse.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long productId = created.getBody().getId();

        // ── Step 2: READ ──────────────────────────────────────────────────────
        ResponseEntity<ProductResponse> fetched = restTemplate.exchange(
            url("/api/products/" + productId), HttpMethod.GET,
            new HttpEntity<>(headers), ProductResponse.class);

        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody().getName()).isEqualTo("Gaming Chair");

        // ── Step 3: UPDATE ────────────────────────────────────────────────────
        ProductRequest updateReq = new ProductRequest();
        updateReq.setName("Premium Gaming Chair");
        updateReq.setPrice(new BigDecimal("399.99"));
        updateReq.setStockQuantity(30);
        updateReq.setCategoryId(savedCategory.getId());

        ResponseEntity<ProductResponse> updated = restTemplate.exchange(
            url("/api/products/" + productId), HttpMethod.PUT,
            new HttpEntity<>(updateReq, headers), ProductResponse.class);

        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().getName()).isEqualTo("Premium Gaming Chair");

        // ── Step 4: DELETE ────────────────────────────────────────────────────
        ResponseEntity<Void> deleted = restTemplate.exchange(
            url("/api/products/" + productId), HttpMethod.DELETE,
            new HttpEntity<>(headers), Void.class);

        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // ── Step 5: VERIFY DELETION ───────────────────────────────────────────
        ResponseEntity<String> notFound = restTemplate.exchange(
            url("/api/products/" + productId), HttpMethod.GET,
            new HttpEntity<>(headers), String.class);

        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
```

**MockMvc vs TestRestTemplate — when to use which:**

```
MockMvc                                 TestRestTemplate
─────────────────────────────────       ────────────────────────────────────
No real server — fast startup.          Real embedded Tomcat — realistic.
Can inspect internals (model, view).    Tests real HTTP (headers, cookies).
Easier to set up auth (@WithMockUser).  Must produce a real JWT token.
Better for controller/security tests.  Better for contract / smoke tests.
```

---

## 7. Database Interaction Testing — @DataJpaTest

`@DataJpaTest` boots only the JPA layer:
- Configures H2 (from `application-test.properties`)
- Loads all `@Entity` classes
- Loads all `@Repository` interfaces
- Does NOT load controllers or services

Every test method is wrapped in a transaction that is **rolled back automatically** after the test.

**File:** `src/test/java/com/training/product/ProductRepositoryTest.java`

```java
package com.training.product;

import com.training.category.Category;
import com.training.category.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// @DataJpaTest — JPA slice only: entities + repositories + H2 datasource
// Each test method is @Transactional and rolled back automatically
@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    // TestEntityManager wraps EntityManager for test utilities
    // Use it to persist/flush/find entities without going through repositories
    @Autowired
    private TestEntityManager entityManager;

    private Category electronics;
    private Category accessories;

    @BeforeEach
    void setUp() {
        // Seed categories used across all tests
        electronics = categoryRepository.save(
            Category.builder().name("Electronics").description("Gadgets").build());
        accessories = categoryRepository.save(
            Category.builder().name("Accessories").description("Add-ons").build());
    }

    // ─── Helper: save product directly via EntityManager ──────────────────────

    private Product saveProduct(String name, BigDecimal price, int stock, Category cat) {
        Product p = Product.builder()
                .name(name).price(price).stockQuantity(stock).category(cat).build();
        return entityManager.persistAndFlush(p);
        // persistAndFlush() persists AND forces Hibernate to flush (write to DB)
        // This is important — tests sometimes fail if the insert is only queued
    }

    // ─── Test: findByNameContainingIgnoreCase ─────────────────────────────────

    @Test
    void findByNameContainingIgnoreCase_returnsMatchingProducts() {

        saveProduct("Laptop Pro", new BigDecimal("1299.99"), 10, electronics);
        saveProduct("Budget Laptop", new BigDecimal("499.99"), 5, electronics);
        saveProduct("Wireless Mouse", new BigDecimal("29.99"), 100, accessories);

        List<Product> results = productRepository.findByNameContainingIgnoreCase("laptop");

        assertThat(results).hasSize(2);
        assertThat(results).extracting(Product::getName)
                .containsExactlyInAnyOrder("Laptop Pro", "Budget Laptop");
    }

    @Test
    void findByNameContainingIgnoreCase_caseInsensitive_finds() {
        saveProduct("MacBook Air", new BigDecimal("999.99"), 15, electronics);

        List<Product> results = productRepository.findByNameContainingIgnoreCase("MACBOOK");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("MacBook Air");
    }

    // ─── Test: findByPriceBetween ──────────────────────────────────────────────

    @Test
    void findByPriceBetween_returnsProductsInRange() {

        saveProduct("Budget Phone",  new BigDecimal("99.99"),  20, electronics);
        saveProduct("Mid Phone",     new BigDecimal("299.99"), 15, electronics);
        saveProduct("Premium Phone", new BigDecimal("899.99"),  5, electronics);

        List<Product> results = productRepository.findByPriceBetween(
            new BigDecimal("100.00"), new BigDecimal("500.00"));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Mid Phone");
    }

    // ─── Test: findByCategoryIdWithCategory (JPQL with JOIN FETCH) ───────────

    @Test
    void findByCategoryIdWithCategory_returnsProductsWithCategoryLoaded() {

        saveProduct("Laptop", new BigDecimal("999.99"), 10, electronics);
        saveProduct("Phone",  new BigDecimal("499.99"), 20, electronics);
        saveProduct("Mouse",  new BigDecimal("29.99"),  50, accessories);

        List<Product> results = productRepository
                .findByCategoryIdWithCategory(electronics.getId());

        assertThat(results).hasSize(2);
        // Since we used JOIN FETCH, category should be loaded (not a proxy)
        results.forEach(p -> assertThat(p.getCategory().getName()).isEqualTo("Electronics"));
    }

    // ─── Test: findCheaperThan (Native SQL query) ─────────────────────────────

    @Test
    void findCheaperThan_returnsProductsBelowMaxPrice() {

        saveProduct("Budget Item", new BigDecimal("9.99"),   100, accessories);
        saveProduct("Mid Item",    new BigDecimal("49.99"),   50, accessories);
        saveProduct("Pricey Item", new BigDecimal("199.99"),  10, accessories);

        List<Product> results = productRepository
                .findCheaperThan(new BigDecimal("50.00"));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Budget Item");
    }

    // ─── Test: findByStockQuantityGreaterThan ─────────────────────────────────

    @Test
    void findByStockQuantityGreaterThan_returnsInStockProducts() {

        saveProduct("In Stock A",  new BigDecimal("10.00"), 100, accessories);
        saveProduct("In Stock B",  new BigDecimal("20.00"),   5, accessories);
        saveProduct("Out of Stock",new BigDecimal("30.00"),   0, accessories);

        List<Product> results = productRepository.findByStockQuantityGreaterThan(0);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(Product::getStockQuantity)
                .allSatisfy(qty -> assertThat(qty).isGreaterThan(0));
    }

    // ─── Test: save and findById ───────────────────────────────────────────────

    @Test
    void save_persistsProductWithGeneratedId() {

        Product product = Product.builder()
                .name("Test Product")
                .price(new BigDecimal("49.99"))
                .stockQuantity(5)
                .category(electronics)
                .build();

        Product saved = productRepository.save(product);
        entityManager.flush();
        entityManager.clear();  // clear first-level cache — forces DB read on next findById

        Optional<Product> found = productRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Product");
        assertThat(found.get().getPrice()).isEqualByComparingTo("49.99");
    }

    // ─── Test: delete removes from DB ─────────────────────────────────────────

    @Test
    void deleteById_removesProduct() {

        Product product = saveProduct("Temp", new BigDecimal("1.00"), 1, accessories);
        Long id = product.getId();

        productRepository.deleteById(id);
        entityManager.flush();

        assertThat(productRepository.findById(id)).isEmpty();
    }

    // ─── Test: existsById ─────────────────────────────────────────────────────

    @Test
    void existsById_returnsTrueForSavedProduct() {

        Product product = saveProduct("Exists", new BigDecimal("1.00"), 1, accessories);
        assertThat(productRepository.existsById(product.getId())).isTrue();
        assertThat(productRepository.existsById(9999L)).isFalse();
    }
}
```

**File:** `src/test/java/com/training/category/CategoryRepositoryTest.java`

```java
package com.training.category;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void save_persistsCategory() {

        Category category = Category.builder()
                .name("Books")
                .description("All kinds of books")
                .build();

        Category saved = categoryRepository.save(category);

        assertThat(saved.getId()).isPositive();
        assertThat(saved.getName()).isEqualTo("Books");
    }

    @Test
    void findById_returnsSavedCategory() {

        Category category = entityManager.persistAndFlush(
            Category.builder().name("Furniture").build());

        Optional<Category> found = categoryRepository.findById(category.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Furniture");
    }

    @Test
    void uniqueConstraint_throwsOnDuplicateName() {

        categoryRepository.save(Category.builder().name("Clothing").build());
        entityManager.flush();

        // Attempting to save another category with the same name should fail
        // because of @Column(unique = true) on Category.name
        Category duplicate = Category.builder().name("Clothing").build();

        assertThrows(Exception.class, () -> {
            categoryRepository.save(duplicate);
            entityManager.flush();   // must flush to trigger the DB constraint
        });
    }
}
```

---

## 8. Summary Cheat Sheet

```
┌───────────────────┬──────────────────────┬──────────────────────────────────┐
│ Test Type         │ Annotation           │ What it tests                    │
├───────────────────┼──────────────────────┼──────────────────────────────────┤
│ Unit              │ @ExtendWith(Mockito) │ Single class, all deps mocked    │
│ Web Layer Slice   │ @WebMvcTest          │ Controller + Security, no DB     │
│ JPA Slice         │ @DataJpaTest         │ Repos + H2, no controllers       │
│ Full Context Mock │ @SpringBootTest      │ Full stack, MockMvc (no port)    │
│                   │ + @AutoConfigMockMvc │                                  │
│ Full Context HTTP │ @SpringBootTest      │ Full stack, real HTTP port       │
│                   │ RANDOM_PORT          │                                  │
└───────────────────┴──────────────────────┴──────────────────────────────────┘

Key annotations for auth in tests:
  @WithMockUser               → authenticated user (any role)
  @WithMockUser(roles="ADMIN")→ admin user
  .with(csrf())               → required for POST/PUT/PATCH/DELETE in MockMvc

Key H2 config:
  spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL
  spring.jpa.hibernate.ddl-auto=create-drop
  @ActiveProfiles("test") on each test class

TestEntityManager tips:
  entityManager.persistAndFlush(entity)  → persist + force write
  entityManager.clear()                  → clear L1 cache (force DB re-read)
  entityManager.flush()                  → flush pending changes to DB
```

---

## 9. Interview Questions

**Q1. What is the difference between `@WebMvcTest` and `@SpringBootTest`?**

`@WebMvcTest` is a test slice that loads only the web layer (controllers, filters, security config, Jackson). Services and repositories are NOT loaded — you mock them with `@MockBean`. It's fast and focused on testing HTTP behavior.

`@SpringBootTest` boots the full application context, exactly like running the app. It's slower but tests the real wiring. You typically combine it with `@AutoConfigureMockMvc` for MockMvc tests, or `WebEnvironment.RANDOM_PORT` for real HTTP tests.

---

**Q2. Why do we use H2 instead of MySQL in tests?**

H2 is an in-memory database — it starts fresh for each test run, requires no external process, and is extremely fast. Tests should be self-contained and reproducible; relying on a running MySQL server would make tests fragile (data left over from previous runs, port conflicts, network issues). H2 with `MODE=MySQL` mimics MySQL syntax closely enough for most scenarios.

---

**Q3. What does `@Transactional` do on a test class?**

When `@Transactional` is applied to a test method or class, Spring wraps each test in a transaction and **rolls it back** after the test finishes. This means DB changes made during one test don't leak into the next test, keeping tests isolated without needing to manually clean up data.

---

**Q4. What is `TestEntityManager` and when do you use it?**

`TestEntityManager` is a test-oriented wrapper around JPA's `EntityManager`. It provides utilities like `persistAndFlush()` (persist + immediately write to DB) and is useful in `@DataJpaTest` to seed test data without going through a repository. The key method is `flush()` which forces Hibernate to flush its write queue so you can test DB constraints like `unique = true`.

---

**Q5. What is the difference between `MockMvc` and `TestRestTemplate`?**

`MockMvc` simulates HTTP requests inside the DispatcherServlet without a real TCP server. It's fast, allows fine-grained assertions (jsonPath, status codes), and makes it easy to fake auth via `@WithMockUser`.

`TestRestTemplate` makes real HTTP calls to an embedded Tomcat server (started with `WebEnvironment.RANDOM_PORT`). It tests the full HTTP stack including serialization and transport. It's better for end-to-end contract tests where you want to verify the real token-based authentication flow.

---

**Q6. Why do MockMvc POST/PUT/DELETE tests need `.with(csrf())`?**

Spring Security Test auto-configures CSRF protection in MockMvc even if you have disabled it in `SecurityConfig`. The `.with(csrf())` post-processor injects a valid CSRF token into the request, satisfying the test-level CSRF check. Without it, mutating requests return `403 Forbidden` in tests regardless of your actual security config.

---

**Q7. What does `entityManager.clear()` do in a repository test, and why is it needed?**

`clear()` clears Hibernate's first-level (L1) cache, which lives inside the `EntityManager`. Without it, a `findById()` call after a `save()` might return the already-in-memory object rather than actually querying the database. Calling `clear()` forces the next `findById()` to execute a real SQL SELECT, confirming the data was actually persisted.

---

**Q8. How does `@ActiveProfiles("test")` work?**

Spring loads property files in layers. `application.properties` is always loaded. When `@ActiveProfiles("test")` is active, Spring also loads `application-test.properties`, and its values override the main file. This lets you switch from MySQL to H2, change log levels, or set test-specific secrets without modifying production config.

---

**Q9. What is `@MockBean` and how is it different from Mockito's `@Mock`?**

`@Mock` (from Mockito) creates a mock but does NOT register it in the Spring context. It only works in plain unit tests where Spring is not involved.

`@MockBean` creates a Mockito mock AND registers it as a Spring bean, replacing any existing bean of that type in the application context. It is used in Spring test slices (`@WebMvcTest`, `@SpringBootTest`) when you want a real Spring context but with specific beans mocked out.

---

**Q10. What is `@AutoConfigureMockMvc` and when is it needed?**

When using `@SpringBootTest`, Spring boots the full application context but does NOT automatically create a `MockMvc` bean — that is a web-layer concern. Adding `@AutoConfigureMockMvc` tells Spring to configure and inject a `MockMvc` instance wired to the full context. Without it, you'd have to build `MockMvc` manually using `MockMvcBuilders.webAppContextSetup(context).build()`.

---

**Q11. What is `WebEnvironment.RANDOM_PORT` and why use a random port?**

`WebEnvironment.RANDOM_PORT` tells `@SpringBootTest` to start a real embedded Tomcat server on a random available port (e.g., 54321). The random port avoids port conflicts when multiple test classes or CI jobs run in parallel on the same machine. You inject the actual port value using `@LocalServerPort` to build the base URL for `TestRestTemplate` calls.

---

**Q12. How do you test that a `@Valid` constraint produces a `400 Bad Request`?**

Send a request with an intentionally invalid field (e.g., a blank name when `@NotBlank` is present). Spring's `MethodArgumentNotValidException` is thrown before the controller method body runs. The `GlobalExceptionHandler` catches it and returns a structured `ErrorResponse` with status 400. In the MockMvc test, assert `status().isBadRequest()` and verify the `validationErrors` array in the JSON response body.

---

**Q13. What is the difference between `@DataJpaTest` and `@SpringBootTest` for testing repositories?**

`@DataJpaTest` is a focused slice — it loads only JPA entities, repositories, H2, and Hibernate configuration. It is fast and the right choice for testing custom queries, derived method names, and DB constraints.

`@SpringBootTest` loads everything. You would only use it for repository testing if you need the full context (e.g., repository methods that depend on Spring Security or application events). For pure data access testing, `@DataJpaTest` is always preferred.

---

**Q14. How do you handle Spring Security when writing `@WebMvcTest` tests?**

`@WebMvcTest` loads the security config, so endpoints are protected. You have three options:
1. `@WithMockUser` — inject a mock principal with a default `USER` role.
2. `@WithMockUser(roles = "ADMIN")` — inject a mock principal with `ADMIN` role for admin-only endpoints.
3. `SecurityMockMvcRequestPostProcessors.user("name").roles("ADMIN")` — inline in a specific `perform()` call.

Do NOT disable security in tests — the security rules themselves should be tested.

---

**Q15. What is the purpose of `jsonPath()` in MockMvc assertions?**

`jsonPath()` evaluates a [JsonPath](https://github.com/json-path/JsonPath) expression against the response body and asserts the result. Examples:
- `$.name` — root-level field
- `$[0].name` — first element of a JSON array
- `$.items[*].id` — all ids inside an items array
- `hasSize(3)` — assert array length

It allows asserting on specific fields without deserializing the entire response, making assertions precise and readable.

---

**Q16. When would you use `@Sql` annotation in a test?**

`@Sql` executes SQL scripts before (or after) a test method or class, useful when you need to seed complex data that's hard to express in Java. For example:

```java
@Test
@Sql("/test-data/products.sql")          // runs before test
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
void testWithSeededData() { ... }
```

Use `@Sql` for large datasets or scenarios where the data is defined in a shared SQL file maintained separately from test code.

---

**Q17. What is the difference between `persistAndFlush()` and `save()` + `flush()` in tests?**

`repository.save(entity)` schedules the INSERT in Hibernate's write queue but may not immediately execute the SQL (depends on flush mode). If you then call `repository.findById()` in the same transaction, Hibernate may return the cached entity without hitting the DB.

`entityManager.persistAndFlush(entity)` persists and immediately flushes — the SQL executes right away. This is critical when testing DB constraints (like `unique = true`) because the constraint violation only happens at flush time.

---

**Q18. What is test isolation and how do Spring Boot tests achieve it?**

Test isolation means each test starts from a known, clean state and does not affect other tests. Spring Boot tests achieve this via:
- `@Transactional` on test classes — each test rolls back DB changes automatically.
- `@DirtiesContext` — forces Spring to destroy and re-create the application context after a test (expensive, use sparingly).
- `@BeforeEach` with `repository.deleteAll()` — explicit cleanup (useful when `@Transactional` rollback doesn't work, e.g., in `RANDOM_PORT` tests).

---

**Q19. Why does `TestRestTemplate` not throw exceptions on 4xx/5xx responses?**

Regular `RestTemplate` throws `HttpClientErrorException` (4xx) or `HttpServerErrorException` (5xx) by default. In tests, throwing exceptions would prevent you from asserting on the error response body, status code, or headers of failure scenarios.

`TestRestTemplate` wraps `RestTemplate` with an error handler that suppresses these exceptions, returning the `ResponseEntity` as-is. This lets you write assertions like `assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)`.

---

**Q20. What is the Testing Pyramid and how does it apply to a Spring Boot REST project?**

The Testing Pyramid describes the recommended distribution of test types:

```
   E2E (few)      — Postman / real browser, slow, expensive
  Integration     — @SpringBootTest, MockMvc, @DataJpaTest
 Unit (many)      — Mockito, JUnit5, no Spring context
```

For a Spring Boot REST project:
- **Unit tests** cover `ProductService` business rules using Mockito-mocked repositories. Fast and numerous.
- **Integration tests** use `@WebMvcTest` for HTTP routing/security, `@DataJpaTest` for custom queries, and `@SpringBootTest` for end-to-end flows.
- **E2E tests** use Postman or a browser against a deployed environment. Minimal — only for critical user journeys.

The pyramid encourages pushing as many tests as possible to the unit level (fast, cheap) and using integration tests only where the interaction between components matters.
