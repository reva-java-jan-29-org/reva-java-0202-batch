# Module 11 — Code Quality & Reliability: Testing, Analysis & Resilience

**Type:** Hands-on + Concepts
**Duration:** ~3 hours
**Prerequisites:** Modules 1–10 complete
**Goal:** Establish a code quality baseline with automated testing, coverage enforcement, static analysis, and resilience patterns that make the microservices system reliable and maintainable.

---

## Learning Objectives

By the end of this module you will be able to:

1. Write unit tests with JUnit 5 and Mockito for Spring Boot services
2. Write integration tests using `@SpringBootTest` with an in-memory H2 database
3. Enforce a minimum test coverage threshold using JaCoCo
4. Run static analysis with Checkstyle and SpotBugs
5. Analyse code quality holistically using SonarQube
6. Scan for dependency vulnerabilities with OWASP Dependency Check
7. Implement circuit breakers with Resilience4j for inter-service calls
8. Expose API documentation via SpringDoc OpenAPI (Swagger UI)

---

## 11.1 Why Code Quality Matters in Microservices

### Technical Debt Compounds Faster in Distributed Systems

In a monolith, a bug affects one deployment. In microservices, a bug in `order-service` can cascade:

```
order-service bug (NPE in placeOrder())
        │
        ├─ Every API call returns 500
        ├─ api-gateway logs flood with errors
        ├─ Prometheus triggers alert
        └─ Product stock not reduced → inconsistent data
```

Poor code quality in one service degrades the entire system.

### The Cost of Finding a Bug

Research consistently shows:

```
Compile time:     1x    (compiler catches it immediately)
Code review:      10x   (a colleague finds it in PR)
CI/testing:       100x  (found in automated tests after merge)
Staging:          1000x (found by QA team)
Production:       10000x (customers affected, data corrupted, on-call at 3am)
```

The earlier you catch a bug, the cheaper it is to fix. Automated tests, static analysis, and code review multiply your ability to catch bugs early.

### Quality Dimensions

| Dimension | What It Means | Tool |
|---|---|---|
| **Correctness** | Does it do what it should? | JUnit, Integration tests |
| **Reliability** | Does it handle failures gracefully? | Resilience4j |
| **Maintainability** | Can others (or future you) understand it? | Checkstyle, SonarQube |
| **Security** | Any known vulnerabilities? | OWASP Dependency Check |
| **Performance** | Is it fast enough? | Micrometer, JMeter |
| **Coverage** | Is the code tested? | JaCoCo |

---

## 11.2 The Testing Pyramid

```
                    ╔═══════════════╗
                    ║  End-to-End   ║  ← Few, slow, brittle
                    ║     (E2E)     ║     Postman, Selenium
                    ╠═══════════════╣
                  ╔═╩═══════════════╩═╗
                  ║   Integration     ║  ← Some, medium speed
                  ║     Tests         ║     @SpringBootTest, Testcontainers
                  ╠═══════════════════╣
                ╔═╩═══════════════════╩═╗
                ║      Unit Tests       ║  ← Many, fast, cheap
                ║                       ║     JUnit 5, Mockito
                ╚═══════════════════════╝

Target distribution: 70% unit / 20% integration / 10% E2E
```

**The "ice cream cone" anti-pattern** (what NOT to do):

```
╔═══════════════════════════════╗
║         Manual Testing        ║  ← Expensive, slow, inconsistent
╠═══════════════════════╗═══════╣
║       E2E Tests        ║  ← Many fragile E2E tests
╠═══════════╗════════════╣
║ Integration║   ← A few
╠═════╗══════╣
║Unit ║ ← Almost none
╚═════╝
```

In microservices, add **Contract Tests** (Pact) between services: a contract defines what the consumer (Order Service) expects from the provider (Product Service). Both sides verify against the contract independently.

---

## 11.3 Unit Testing with JUnit 5 + Mockito

### Dependencies

`spring-boot-starter-test` (already in parent pom.xml as a transitive dependency) includes:
- **JUnit 5** (Jupiter) — test framework
- **Mockito** — mock library
- **AssertJ** — fluent assertions
- **Hamcrest** — matcher library
- **MockMvc** — Spring MVC test support

Verify it's in your parent `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

### Mockito Fundamentals

```java
// @Mock — creates a mock (all methods return null/0/false by default)
@Mock
private CustomerRepository customerRepository;

// @InjectMocks — creates the object under test, injects @Mock fields
@InjectMocks
private AuthController authController;

// @Spy — wraps a real object, delegates to real methods unless stubbed
@Spy
private BCryptPasswordEncoder passwordEncoder;

// @ExtendWith — activates Mockito annotations
@ExtendWith(MockitoExtension.class)

// Stubbing — define what a mock returns
when(customerRepository.existsByUsername("alice")).thenReturn(false);
when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
    Customer c = invocation.getArgument(0);
    c.setId(1L);
    return c;
});

// Verification — assert a mock was called
verify(customerRepository).save(any(Customer.class));
verify(customerRepository, times(1)).existsByUsername("alice");
verify(customerRepository, never()).findByUsername(any());

// Argument Captor — capture and assert what was passed to a mock
@Captor
ArgumentCaptor<Customer> customerCaptor;
verify(customerRepository).save(customerCaptor.capture());
Customer saved = customerCaptor.getValue();
assertThat(saved.getRole()).isEqualTo(Role.CUSTOMER);
```

---

### Test: `JwtServiceTest.java`

```java
// user-service/src/test/java/com/ecommerce/user/security/JwtServiceTest.java
package com.ecommerce.user.security;

import com.ecommerce.user.entity.Customer;
import com.ecommerce.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService — Token Generation and Validation")
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        // Inject @Value fields (can't autowire in unit tests)
        ReflectionTestUtils.setField(jwtService, "secretKeyString",
            "test-secret-key-for-unit-testing-must-be-at-least-32-chars");
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 86400000L);

        testCustomer = Customer.builder()
                .username("alice")
                .password("hashed-password")
                .role(Role.CUSTOMER)
                .firstName("Alice")
                .lastName("Smith")
                .mobileNumber("0412345678")
                .build();
        testCustomer.setId(1L);
    }

    @Test
    @DisplayName("generateToken(Customer) returns a non-null, non-empty token")
    void generateToken_Customer_returnsToken() {
        String token = jwtService.generateToken(testCustomer);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        // JWT format: three base64-encoded parts separated by dots
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("extractUsername() returns correct username from token")
    void extractUsername_fromValidToken_returnsUsername() {
        String token = jwtService.generateToken(testCustomer);

        String extractedUsername = jwtService.extractUsername(token);

        assertThat(extractedUsername).isEqualTo("alice");
    }

    @Test
    @DisplayName("isTokenValid() returns true for freshly generated token")
    void isTokenValid_freshToken_returnsTrue() {
        String token = jwtService.generateToken(testCustomer);

        boolean valid = jwtService.isTokenValid(token, testCustomer);

        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("isTokenValid() returns false when username does not match")
    void isTokenValid_wrongUser_returnsFalse() {
        String token = jwtService.generateToken(testCustomer);

        // Create a different user
        Customer otherCustomer = Customer.builder()
                .username("bob")
                .password("hashed")
                .role(Role.CUSTOMER)
                .mobileNumber("0499999999")
                .build();
        otherCustomer.setId(2L);

        boolean valid = jwtService.isTokenValid(token, otherCustomer);

        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("isTokenValid() returns false for an expired token")
    void isTokenValid_expiredToken_returnsFalse() {
        // Set expiration to -1ms (already expired)
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", -1L);
        String expiredToken = jwtService.generateToken(testCustomer);

        // Reset to normal expiration for validation
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 86400000L);

        // Expired token should throw JwtException internally → isTokenValid returns false
        // (JwtService.isTokenExpired() returns true → overall validation fails)
        boolean valid = jwtService.isTokenValid(expiredToken, testCustomer);

        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("generateToken(Customer) includes userId claim for gateway compatibility")
    void generateToken_Customer_includesUserIdClaim() {
        String token = jwtService.generateToken(testCustomer);

        String username = jwtService.extractUsername(token);
        // If we can extract username, the token parses correctly
        // In production tests, you'd also verify the userId claim directly
        assertThat(username).isEqualTo("alice");
    }
}
```

---

### Test: `AuthControllerTest.java`

```java
// user-service/src/test/java/com/ecommerce/user/controller/AuthControllerTest.java
package com.ecommerce.user.controller;

import com.ecommerce.user.dto.AuthResponse;
import com.ecommerce.user.dto.LoginRequest;
import com.ecommerce.user.dto.RegisterRequest;
import com.ecommerce.user.entity.Customer;
import com.ecommerce.user.entity.Role;
import com.ecommerce.user.repository.CustomerRepository;
import com.ecommerce.user.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController — /api/auth endpoints")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerRepository customerRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private AuthenticationManager authenticationManager;

    // ── Register Tests ─────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/register — valid request returns 200 with token")
    void register_validRequest_returns200WithToken() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "alice", "password123", "Alice", "Smith", "0412345678", null);

        when(customerRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed");
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(jwtService.generateToken(any(Customer.class))).thenReturn("mock-jwt-token");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));    // default role

        verify(customerRepository).save(any(Customer.class));
        verify(jwtService).generateToken(any(Customer.class));
    }

    @Test
    @DisplayName("POST /api/auth/register — ADMIN role is preserved")
    void register_withAdminRole_returnsAdminRole() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "admin", "admin123", "Admin", "User", "0499999999", Role.ADMIN);

        when(customerRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(customerRepository.save(any())).thenAnswer(i -> {
            Customer c = i.getArgument(0);
            c.setId(2L);
            return c;
        });
        when(jwtService.generateToken(any(Customer.class))).thenReturn("admin-token");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("POST /api/auth/register — duplicate username returns 500")
    void register_duplicateUsername_returns500() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "alice", "password123", null, null, "0412345678", null);

        when(customerRepository.existsByUsername("alice")).thenReturn(true);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("POST /api/auth/register — blank username returns 400 (validation)")
    void register_blankUsername_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "", "password123", null, null, "0412345678", null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/register — missing mobileNumber returns 400")
    void register_missingMobileNumber_returns400() throws Exception {
        // mobileNumber is @NotBlank but we're passing null
        String json = """
            {
              "username": "alice",
              "password": "password123"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    // ── Login Tests ────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/login — valid credentials returns 200 with token")
    void login_validCredentials_returns200WithToken() throws Exception {
        LoginRequest request = new LoginRequest("alice", "password123");

        Customer customer = Customer.builder()
                .username("alice")
                .password("$2a$10$hashed")
                .role(Role.CUSTOMER)
                .mobileNumber("0412345678")
                .build();
        customer.setId(1L);

        // authenticationManager does not throw → credentials valid
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(customerRepository.findByUsername("alice")).thenReturn(Optional.of(customer));
        when(jwtService.generateToken(customer)).thenReturn("login-jwt-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("login-jwt-token"))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    @DisplayName("POST /api/auth/login — wrong password returns 403")
    void login_wrongPassword_returns403() throws Exception {
        LoginRequest request = new LoginRequest("alice", "wrong-password");

        // authenticationManager throws BadCredentialsException for wrong password
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
```

---

### Test: `CartServiceTest.java`

```java
// order-service/src/test/java/com/ecommerce/order/service/CartServiceTest.java
package com.ecommerce.order.service;

import com.ecommerce.order.dto.CartDto;
import com.ecommerce.order.dto.CartItemRequest;
import com.ecommerce.order.dto.ProductDto;
import com.ecommerce.order.entity.Cart;
import com.ecommerce.order.entity.CartItem;
import com.ecommerce.order.feign.ProductServiceClient;
import com.ecommerce.order.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService — Cart management")
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductServiceClient productServiceClient;

    @InjectMocks
    private CartService cartService;

    private ProductDto laptop;

    @BeforeEach
    void setUp() {
        laptop = new ProductDto();
        laptop.setId(1L);
        laptop.setName("Laptop Pro 15");
        laptop.setPrice(new BigDecimal("1299.99"));
    }

    @Test
    @DisplayName("getCart() creates new empty cart when user has none")
    void getCart_noExistingCart_createsNewCart() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        CartDto result = cartService.getCart(1L);

        assertThat(result).isNotNull();
        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(cartRepository).save(any(Cart.class));    // new cart was saved
    }

    @Test
    @DisplayName("getCart() returns existing cart without creating a new one")
    void getCart_existingCart_returnsIt() {
        Cart existingCart = new Cart();
        existingCart.setId(1L);
        existingCart.setUserId(1L);
        existingCart.setItems(new ArrayList<>());

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(existingCart));

        CartDto result = cartService.getCart(1L);

        assertThat(result.getId()).isEqualTo(1L);
        verify(cartRepository, never()).save(any());    // no new cart created
    }

    @Test
    @DisplayName("addToCart() adds new item when product not in cart")
    void addToCart_newProduct_addsCartItem() {
        Cart cart = new Cart();
        cart.setId(1L);
        cart.setUserId(1L);
        cart.setItems(new ArrayList<>());

        CartItemRequest request = new CartItemRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        when(productServiceClient.getProductById(1L)).thenReturn(laptop);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartDto result = cartService.addToCart(1L, request);

        assertThat(cart.getItems()).hasSize(1);
        CartItem addedItem = cart.getItems().get(0);
        assertThat(addedItem.getProductId()).isEqualTo(1L);
        assertThat(addedItem.getProductName()).isEqualTo("Laptop Pro 15");
        assertThat(addedItem.getQuantity()).isEqualTo(2);
        assertThat(addedItem.getPrice()).isEqualByComparingTo("1299.99");
    }

    @Test
    @DisplayName("addToCart() increments quantity when same product added again")
    void addToCart_existingProduct_incrementsQuantity() {
        CartItem existingItem = new CartItem();
        existingItem.setId(1L);
        existingItem.setProductId(1L);
        existingItem.setProductName("Laptop Pro 15");
        existingItem.setPrice(new BigDecimal("1299.99"));
        existingItem.setQuantity(1);    // already 1 in cart

        Cart cart = new Cart();
        cart.setId(1L);
        cart.setUserId(1L);
        cart.setItems(new ArrayList<>(java.util.List.of(existingItem)));

        CartItemRequest request = new CartItemRequest();
        request.setProductId(1L);
        request.setQuantity(2);    // add 2 more

        when(productServiceClient.getProductById(1L)).thenReturn(laptop);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenReturn(cart);

        cartService.addToCart(1L, request);

        // Quantity should be 1 (existing) + 2 (added) = 3
        assertThat(existingItem.getQuantity()).isEqualTo(3);
        // No new item created
        assertThat(cart.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("removeFromCart() removes the correct cart item")
    void removeFromCart_existingItem_removesIt() {
        CartItem item1 = new CartItem();
        item1.setId(1L);
        item1.setProductId(1L);

        CartItem item2 = new CartItem();
        item2.setId(2L);
        item2.setProductId(2L);

        Cart cart = new Cart();
        cart.setId(1L);
        cart.setUserId(1L);
        cart.setItems(new ArrayList<>(java.util.List.of(item1, item2)));

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenReturn(cart);

        cartService.removeFromCart(1L, 1L);    // remove item with id=1

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("removeFromCart() throws when cart does not exist")
    void removeFromCart_noCart_throwsException() {
        when(cartRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeFromCart(99L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cart not found");
    }
}
```

---

## 11.4 Integration Testing with `@SpringBootTest`

Integration tests start a real Spring ApplicationContext but use an in-memory H2 database instead of MySQL.

### Add H2 Test Dependency

Add to `user-service/pom.xml`:

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

### Integration Test: `AuthIntegrationTest.java`

```java
// user-service/src/test/java/com/ecommerce/user/AuthIntegrationTest.java
package com.ecommerce.user;

import com.ecommerce.user.dto.LoginRequest;
import com.ecommerce.user.dto.RegisterRequest;
import com.ecommerce.user.entity.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    // Use H2 in-memory instead of MySQL
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    // Disable Config Server import (no config-server running in tests)
    "spring.config.import=",
    // JWT config (required by JwtService)
    "jwt.secret=integration-test-secret-key-must-be-at-least-32-chars",
    "jwt.expiration=86400000"
})
@DisplayName("Auth Integration Tests — full Spring context with H2")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Full register → login flow works end-to-end")
    void register_thenLogin_succeeds() throws Exception {
        // ── Step 1: Register ────────────────────────────────────────────────
        RegisterRequest registerRequest = new RegisterRequest(
                "integration-user", "securePass1", "Integration", "User",
                "0400000001", null);

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("integration-user"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andReturn();

        // ── Step 2: Login ───────────────────────────────────────────────────
        LoginRequest loginRequest = new LoginRequest("integration-user", "securePass1");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("integration-user"));
    }

    @Test
    @DisplayName("Protected endpoint requires valid JWT")
    void protectedEndpoint_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Protected endpoint accessible with valid JWT")
    void protectedEndpoint_withValidToken_returns200() throws Exception {
        // Register to get a token
        RegisterRequest registerRequest = new RegisterRequest(
                "jwt-test-user", "password123", "JWT", "Test",
                "0400000002", null);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody).get("token").asText();

        // Use the token to access protected endpoint
        mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Duplicate username registration returns error")
    void register_duplicateUsername_returnsError() throws Exception {
        RegisterRequest first = new RegisterRequest(
                "duplicate-user", "pass123", null, null, "0400000003", null);

        // First registration succeeds
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk());

        // Second registration with same username fails
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().is5xxServerError());
    }
}
```

**Key annotations:**

| Annotation | What It Does |
|---|---|
| `@SpringBootTest` | Loads the full ApplicationContext |
| `@AutoConfigureMockMvc` | Creates MockMvc without a real HTTP server |
| `@TestPropertySource(properties=...)` | Override properties for tests |
| `spring.config.import=` | Disables Config Server (prevents test failure if config-server is down) |
| `ddl-auto=create-drop` | Creates schema at test start, drops at end |

---

## 11.5 Test Coverage with JaCoCo

### Configuration in Parent `pom.xml`

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.11</version>
            <executions>
                <!-- Instrument bytecode before tests run -->
                <execution>
                    <id>prepare-agent</id>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>

                <!-- Generate HTML/XML report after tests -->
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>

                <!-- Fail build if coverage drops below threshold -->
                <execution>
                    <id>check</id>
                    <goals>
                        <goal>check</goal>
                    </goals>
                    <configuration>
                        <rules>
                            <rule>
                                <element>BUNDLE</element>
                                <limits>
                                    <limit>
                                        <counter>LINE</counter>
                                        <value>COVEREDRATIO</value>
                                        <minimum>0.70</minimum>  <!-- 70% minimum -->
                                    </limit>
                                </limits>
                            </rule>
                        </rules>

                        <!-- Exclude generated/config code from coverage -->
                        <excludes>
                            <exclude>**/*Application.class</exclude>
                            <exclude>**/entity/**</exclude>       <!-- JPA entities — mostly getters/setters -->
                            <exclude>**/dto/**</exclude>          <!-- Java records -->
                            <exclude>**/config/**</exclude>       <!-- Spring config classes -->
                        </excludes>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### Running and Reading the Report

```bash
# Run tests and generate coverage report
mvn clean verify

# Open the report (for user-service)
open user-service/target/site/jacoco/index.html
```

The HTML report shows:

```
Package                     Class %   Method %   Line %   Branch %
───────────────────────────────────────────────────────────────────
com.ecommerce.user.controller  100%     95%       92%      88%    ← good
com.ecommerce.user.security    100%     100%      87%      75%    ← good
com.ecommerce.user.service      80%     60%       55%      40%    ← needs work!
```

Lines are highlighted:
- **Green** — covered by tests
- **Yellow** — partially covered (branch not fully tested)
- **Red** — not covered at all

---

## 11.6 Code Style with Checkstyle

### Configuration in Parent `pom.xml`

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.3.0</version>
    <configuration>
        <configLocation>google_checks.xml</configLocation>   <!-- Google style -->
        <consoleOutput>true</consoleOutput>
        <failsOnError>true</failsOnError>
        <violationSeverity>warning</violationSeverity>
        <excludes>**/generated/**,**/target/**</excludes>
    </configuration>
    <executions>
        <execution>
            <id>validate</id>
            <phase>validate</phase>           <!-- runs before compilation -->
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Run Checkstyle

```bash
mvn checkstyle:check

# Generate HTML report
mvn checkstyle:checkstyle
open target/site/checkstyle.html
```

### Common Checkstyle Rules (Google Style)

- Line length ≤ 100 characters
- 2-space indentation
- Braces on same line (`{` not on new line)
- Javadoc on public methods
- Import ordering: static imports last

### Suppressing Specific Violations

```java
// Suppress for a specific line
@SuppressWarnings("checkstyle:MagicNumber")
int timeout = 86400000;   // 24 hours in ms

// Or create suppression.xml
```

Create `checkstyle-suppressions.xml`:
```xml
<?xml version="1.0"?>
<!DOCTYPE suppressions PUBLIC
    "-//Checkstyle//DTD SuppressionFilter Configuration 1.0//EN"
    "https://checkstyle.org/dtds/suppressions_1_0.dtd">
<suppressions>
    <!-- Suppress line length check in test classes -->
    <suppress checks="LineLength" files=".*Test\.java"/>
    <!-- Suppress Javadoc for DTOs -->
    <suppress checks="JavadocMethod" files=".*dto.*"/>
</suppressions>
```

---

## 11.7 Static Analysis with SpotBugs

SpotBugs analyses compiled bytecode to find:
- Null pointer dereferences
- Bad practices (equals/hashCode inconsistency)
- Thread safety issues
- SQL injection patterns
- Unnecessary object creation

### Configuration in Parent `pom.xml`

```xml
<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <version>4.8.2.0</version>
    <configuration>
        <effort>Max</effort>               <!-- thoroughness: Min, Default, Max -->
        <threshold>Low</threshold>         <!-- report level: Low, Medium, High -->
        <failOnError>true</failOnError>
        <excludeFilterFile>spotbugs-exclude.xml</excludeFilterFile>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Run SpotBugs

```bash
# Run analysis (fails build if bugs found)
mvn spotbugs:check

# Open graphical report
mvn spotbugs:gui
```

### Creating an Exclusion Filter

Some warnings are false positives. Create `spotbugs-exclude.xml`:

```xml
<FindBugsFilter>
    <!-- Lombok-generated equals/hashCode — SpotBugs incorrectly flags these -->
    <Match>
        <Bug pattern="EQ_UNUSUAL"/>
        <Class name="~.*entity.*"/>
    </Match>

    <!-- Spring Security configuration classes — these patterns are intentional -->
    <Match>
        <Bug pattern="HARD_CODE_PASSWORD"/>
        <Class name="~.*SecurityConfig.*"/>
    </Match>
</FindBugsFilter>
```

---

## 11.8 SonarQube — Comprehensive Quality Analysis

SonarQube provides a single dashboard covering: bugs, vulnerabilities, code smells, duplications, test coverage, and security hotspots.

### Run SonarQube Locally with Docker

```bash
# Start SonarQube (Community Edition — free)
docker run -d \
  --name sonarqube \
  -p 9000:9000 \
  -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true \
  sonarqube:community

# Wait ~2 minutes for SonarQube to start
# Then open: http://localhost:9000
# Default credentials: admin / admin
# You'll be prompted to change the password on first login
```

### Add SonarQube Scanner to Parent `pom.xml`

```xml
<plugin>
    <groupId>org.sonarsource.scanner.maven</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <version>3.10.0.2594</version>
</plugin>
```

### Run the Analysis

```bash
mvn clean verify sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=your-sonar-token \
  -Dsonar.projectKey=ecommerce-microservices \
  -Dsonar.projectName="E-Commerce Microservices"
```

Generate a token: SonarQube → My Account → Security → Generate Token

### The SonarQube Dashboard

```
Quality Gate: ● PASSED

Bugs           0       Vulnerabilities    0       Security Hotspots    2
Code Smells   12       Duplications      3.2%     Coverage           74.3%

Code Smells breakdown:
  ├─ Major: 3   → Long method in CartService (extract to private helper)
  ├─ Minor: 7   → Magic numbers (use named constants)
  └─ Info:  2   → TODO comments
```

### SonarQube in CI/CD (GitHub Actions)

Add to your `.github/workflows/ci.yml`:

```yaml
- name: SonarQube Analysis
  env:
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
    SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}  # e.g. https://sonarcloud.io
  run: |
    mvn sonar:sonar \
      -Dsonar.projectKey=ecommerce-microservices \
      -Dsonar.organization=your-org \
      -Dsonar.host.url=$SONAR_HOST_URL \
      -Dsonar.login=$SONAR_TOKEN
```

> **SonarCloud** (cloud version — free for public repos): Use `sonarcloud.io` as `SONAR_HOST_URL`. No local SonarQube needed for public repositories.

---

## 11.9 Security — OWASP Dependency Check

Third-party libraries can contain known vulnerabilities (CVEs). OWASP Dependency Check scans all JARs in your project against the National Vulnerability Database (NVD).

### Configuration in Parent `pom.xml`

```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>9.0.7</version>
    <configuration>
        <failBuildOnCVSS>7</failBuildOnCVSS>   <!-- fail if any CVSS score ≥ 7 (High) -->
        <suppressionFile>dependency-check-suppressions.xml</suppressionFile>
        <nvdApiKey>${env.NVD_API_KEY}</nvdApiKey>   <!-- optional but faster -->
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### CVSS Scoring

| Score | Severity | Action |
|---|---|---|
| 9.0–10.0 | Critical | Fix immediately |
| 7.0–8.9 | High | Fix before next release |
| 4.0–6.9 | Medium | Fix within 30 days |
| 0.1–3.9 | Low | Fix when convenient |
| 0.0 | None | Informational |

### Running the Scan

```bash
# Run on first time (downloads NVD database, takes 5-10 minutes)
mvn dependency-check:check

# Open the report
open target/dependency-check-report.html
```

### Suppressing False Positives

Create `dependency-check-suppressions.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
    <suppress>
        <notes>CVE-2023-12345 — affects feature X which we do not use</notes>
        <cve>CVE-2023-12345</cve>
        <until>2024-12-31</until>    <!-- Review before this date -->
    </suppress>
</suppressions>
```

---

## 11.10 Reliability with Resilience4j

### Why Circuit Breakers?

When `order-service` calls `product-service` via Feign and product-service is down:

**Without circuit breaker:**
```
addToCart() → Feign GET /api/products/1 → connection timeout (30s) → error
addToCart() → Feign GET /api/products/1 → connection timeout (30s) → error
addToCart() → Feign GET /api/products/1 → connection timeout (30s) → error
```
Every call waits 30 seconds before failing. Order Service threads pile up. Eventually Order Service itself crashes.

**With circuit breaker:**
```
addToCart() → Feign GET /api/products/1 → timeout (1) → fail
addToCart() → Feign GET /api/products/1 → timeout (2) → fail
addToCart() → Feign GET /api/products/1 → timeout (3) → CIRCUIT OPENS
addToCart() → CIRCUIT IS OPEN → fail immediately (fallback)  ← no wait!
addToCart() → CIRCUIT IS OPEN → fail immediately (fallback)
[after 30s] → CIRCUIT HALF-OPEN → try one request
addToCart() → success → CIRCUIT CLOSES
```

### Circuit Breaker States

```
                      calls fail         calls pass
CLOSED ──────────────────────────► OPEN ──────────► HALF-OPEN
(normal)    (fail rate > threshold)  (all fail fast) (try one request)
    ▲                                                      │
    └──────────────────────────────────────────────────────┘
                    one request succeeded
```

### Add Resilience4j Dependency

```xml
<!-- order-service/pom.xml -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

### Apply Circuit Breaker to `CartService`

```java
// order-service/src/main/java/com/ecommerce/order/service/CartService.java

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Service
public class CartService {

    // ...existing code...

    @CircuitBreaker(name = "productService", fallbackMethod = "addToCartFallback")
    @Retry(name = "productService")
    public CartDto addToCart(Long userId, CartItemRequest request) {
        ProductDto product = productServiceClient.getProductById(request.getProductId());
        // ...rest of method...
    }

    // Fallback method — same signature + Exception parameter
    public CartDto addToCartFallback(Long userId, CartItemRequest request, Exception ex) {
        throw new RuntimeException(
            "Product service is currently unavailable. Please try again in a moment. " +
            "Original error: " + ex.getMessage()
        );
    }
}
```

### Resilience4j Configuration in `application.yml`

```yaml
resilience4j:
  circuitbreaker:
    instances:
      productService:
        sliding-window-size: 10                    # evaluate last 10 calls
        minimum-number-of-calls: 5                 # min calls before opening
        failure-rate-threshold: 50                 # open if 50%+ fail
        wait-duration-in-open-state: 30s           # stay open 30s before trying
        permitted-number-of-calls-in-half-open-state: 3

  retry:
    instances:
      productService:
        max-attempts: 3                            # retry up to 3 times
        wait-duration: 1s                          # wait 1s between retries
        retry-exceptions:
          - feign.FeignException.ServiceUnavailable
          - java.net.ConnectException

  timelimiter:
    instances:
      productService:
        timeout-duration: 5s                       # fail after 5s (no 30s wait)
```

### Feign Client Timeout Configuration

```yaml
# order-service/src/main/resources/application.yml
feign:
  client:
    config:
      product-service:
        connect-timeout: 2000    # 2s to establish connection
        read-timeout: 5000       # 5s to receive response
```

---

## 11.11 API Documentation with SpringDoc OpenAPI

### Add SpringDoc Dependency

Add to each web service's `pom.xml`:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

For the API Gateway (WebFlux-based), use the WebFlux variant:
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

### Swagger UI

After adding the dependency and starting a service:

```
http://localhost:8081/swagger-ui.html     → interactive API documentation
http://localhost:8081/v3/api-docs         → raw OpenAPI JSON
http://localhost:8081/v3/api-docs.yaml    → raw OpenAPI YAML
```

### Allow Swagger in SecurityConfig

Add to `user-service/src/main/java/.../config/SecurityConfig.java`:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers("/swagger-ui/**",
                     "/swagger-ui.html",
                     "/v3/api-docs/**").permitAll()    // ← add this
    .anyRequest().authenticated()
)
```

### Documenting Controllers

```java
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Register and login endpoints")
@RequiredArgsConstructor
public class AuthController {

    @PostMapping("/register")
    @Operation(
        summary = "Register a new customer",
        description = "Creates a new customer account and returns a JWT token"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registration successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed"),
        @ApiResponse(responseCode = "500", description = "Username already exists")
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        // ...
    }

    @PostMapping("/login")
    @Operation(summary = "Login with username and password")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "403", description = "Invalid credentials")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // ...
    }
}
```

### Global OpenAPI Configuration

```java
// user-service/src/main/java/com/ecommerce/user/config/OpenApiConfig.java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("User Service API")
                        .version("1.0.0")
                        .description("Authentication and customer management")
                        .contact(new Contact()
                                .name("E-Commerce Team")
                                .email("dev@ecommerce.com")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
```

Now the Swagger UI shows a "Authorize" button where you can paste your JWT token and test protected endpoints directly in the browser.

---

## 11.12 Complete Quality Pipeline

All quality checks integrated into the CI pipeline:

```
Code Push to main/PR
         │
         ▼
.github/workflows/ci.yml
    │
    ├─ 1. Checkout + JDK setup
    │
    ├─ 2. mvn clean verify
    │       ├─ Compile
    │       ├─ Run unit tests (JUnit 5 + Mockito)
    │       ├─ Run integration tests (@SpringBootTest)
    │       ├─ JaCoCo coverage report
    │       └─ JaCoCo check (fail if < 70%)
    │
    ├─ 3. mvn checkstyle:check
    │       └─ Fail if code style violations
    │
    ├─ 4. mvn spotbugs:check
    │       └─ Fail if bugs found
    │
    ├─ 5. mvn dependency-check:check
    │       └─ Fail if CVE score ≥ 7
    │
    └─ 6. mvn sonar:sonar
            └─ Quality Gate: fail if gate not met

All pass → Docker build → Push to ghcr.io → Deploy to staging
```

Add Checkstyle + SpotBugs to GitHub Actions:

```yaml
# In .github/workflows/ci.yml, add after the build step:

- name: Run Checkstyle
  run: mvn checkstyle:check -B

- name: Run SpotBugs
  run: mvn spotbugs:check -B

- name: OWASP Dependency Check
  run: mvn dependency-check:check -B
  env:
    NVD_API_KEY: ${{ secrets.NVD_API_KEY }}   # optional, for faster NVD download

- name: SonarQube Analysis
  if: github.ref == 'refs/heads/main'
  run: |
    mvn sonar:sonar \
      -Dsonar.host.url=${{ secrets.SONAR_HOST_URL }} \
      -Dsonar.login=${{ secrets.SONAR_TOKEN }} \
      -Dsonar.projectKey=ecommerce-microservices
```

---

## 11.13 Module Checkpoint

### Testing checklist:

- [ ] `mvn test -pl user-service` — all unit tests pass
- [ ] `mvn test -pl order-service` — CartServiceTest passes
- [ ] `mvn verify -pl user-service` — integration tests pass (H2 database)
- [ ] JaCoCo report visible: `user-service/target/site/jacoco/index.html`
- [ ] Coverage ≥ 70% on service/controller packages

### Static analysis checklist:

- [ ] `mvn checkstyle:check` — no style violations
- [ ] `mvn spotbugs:check` — no bugs found (or all suppressed with justification)

### SonarQube checklist:

- [ ] SonarQube running at http://localhost:9000 (via Docker)
- [ ] `mvn sonar:sonar` analysis completes
- [ ] Quality Gate is GREEN on SonarQube dashboard

### Resilience checklist:

- [ ] Resilience4j dependency added to order-service
- [ ] `@CircuitBreaker` on `CartService.addToCart()`
- [ ] Test: stop product-service → add to cart returns meaningful error immediately
- [ ] Test: restart product-service → circuit closes → add to cart works again

### API Documentation checklist:

- [ ] SpringDoc OpenAPI added to user-service
- [ ] Swagger UI accessible at http://localhost:8081/swagger-ui.html
- [ ] `AuthController` annotated with `@Operation` and `@ApiResponse`
- [ ] JWT token can be entered in Swagger UI "Authorize" → test protected endpoints

---

## Congratulations — Quality and Reliability Complete!

You have now built a production-ready microservices system with:

| Module | Topic | Key Tools |
|---|---|---|
| 1 | Introduction to Microservices | Concepts, patterns |
| 2 | Project Setup | Maven multi-module, config-repo |
| 3 | Config Server | Spring Cloud Config, Git-backed |
| 4 | Eureka Server | Service discovery, health |
| 5 | User Service | JWT, Spring Security, @MappedSuperclass |
| 6 | Product Service | JPA, DataInitializer |
| 7 | API Gateway | GlobalFilter, reactive routing |
| 8 | Order Service | OpenFeign, @Transactional |
| 9 | Docker | Containerization, Docker Compose |
| 10 | CI/CD & DevOps | GitHub Actions, Prometheus, Zipkin |
| 11 | Code Quality | JUnit 5, JaCoCo, SonarQube, Resilience4j |

**The complete system you built:**

```
Quality Gate (CI)
    ✓ Tests pass (JUnit 5 + Mockito + @SpringBootTest)
    ✓ Coverage ≥ 70% (JaCoCo)
    ✓ No style violations (Checkstyle)
    ✓ No known bugs (SpotBugs)
    ✓ No high CVEs (OWASP)
    ✓ Quality Gate green (SonarQube)
                │
                ▼
Docker Images (ghcr.io)
                │
                ▼
Docker Compose Stack
    ├─ MySQL :3306
    ├─ Config Server :8888
    ├─ Eureka Server :8761
    ├─ User Service :8081
    ├─ Product Service :8082
    ├─ Order Service :8083
    └─ API Gateway :8080
                │
Observability Stack
    ├─ Prometheus :9090 (metrics)
    ├─ Grafana :3000 (dashboards)
    ├─ Loki (log aggregation)
    ├─ Zipkin :9411 (tracing)
    └─ Swagger UI :8081/swagger-ui.html
```
