# Module 5 — User Service: Registration, Login & JWT Generation

**Type:** Hands-on Implementation
**Duration:** ~3 hours
**Prerequisites:** Modules 3 & 4 complete — Config Server (:8888) and Eureka Server (:8761) both running
**Goal:** Build the complete User Service — the only service that generates JWT tokens — using Spring Security, `@MappedSuperclass` entity inheritance, Lombok `@SuperBuilder`, and Java records for DTOs.

---

## Learning Objectives

By the end of this module you will be able to:

1. Design a `@MappedSuperclass` base entity that implements `UserDetails`
2. Use `@SuperBuilder` to support builder pattern across an inheritance hierarchy
3. Explain the difference between `Role` as an enum vs a String column
4. Write Java records as immutable, validated DTOs
5. Implement `JwtService` using JJWT 0.12.3 (HS256) to generate tokens
6. Build `JwtAuthenticationFilter` extending `OncePerRequestFilter`
7. Wire `DaoAuthenticationProvider` + `AuthenticationManager` in `SecurityConfig`
8. Expose `/api/auth/register` and `/api/auth/login` endpoints
9. Validate all endpoints using Postman — register and login as both CUSTOMER and ADMIN

---

## Recap — Where We Are

```
✅ Config Server   :8888  — Running, serving datasource config
✅ Eureka Server   :8761  — Running, dashboard shows CONFIG-SERVER
⬜ User Service    :8081  ← This module
⬜ Product Service  :8082
⬜ Order Service    :8083
⬜ API Gateway      :8080
```

---

## 5.1 Where User Service Fits

```
Postman / Browser
      │
      │  POST /api/auth/register
      │  POST /api/auth/login
      ▼
 User Service :8081
      │  reads config from
      ▼
 Config Server :8888   (datasource URL, JWT secret)
      │  registers via
      ▼
 Eureka Server :8761
      │  persists to
      ▼
   user_db (MySQL)   → customers table
```

The User Service is the **only service that issues JWT tokens**. Every other service trusts tokens that arrive pre-validated by the API Gateway.

---

## 5.2 Maven Dependencies — `user-service/pom.xml`

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

    <artifactId>user-service</artifactId>
    <name>User Service</name>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-config</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- JJWT — JWT generation and parsing -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.3</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.3</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.3</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok — @Data, @SuperBuilder, @RequiredArgsConstructor -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

**Key dependency groups:**
- `spring-boot-starter-security` — provides Spring Security, `UserDetails`, `BCryptPasswordEncoder`, `SecurityFilterChain`
- `jjwt-api` (compile) + `jjwt-impl`/`jjwt-jackson` (runtime) — JJWT split across three artifacts
- `lombok` — reduces boilerplate with `@Data`, `@SuperBuilder`, `@RequiredArgsConstructor`

---

## 5.3 Configuration

### `user-service/src/main/resources/application.yml`

```yaml
server:
  port: 8081

spring:
  application:
    name: user-service          # must match the config-repo filename
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

### `config-repo/user-service.properties`

This file lives in your local Git config repository and is served by Config Server:

```properties
# Datasource — user_db
spring.datasource.url=jdbc:mysql://localhost:3306/user_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=Root123
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# JWT — must match the secret used by the API Gateway
jwt.secret=ecommerce-jwt-secret-key-for-microservices-project-2024-secure
jwt.expiration=86400000
```

> **Important:** The `jwt.secret` value here **must be identical** to the `jwt.secret` in `api-gateway/src/main/resources/application.yml`. Both services derive the HMAC signing key from the same UTF-8 bytes.

---

## 5.4 Entity Layer — Role, User, Customer

### 5.4.1 `Role.java` — the permission enum

```java
// user-service/src/main/java/com/ecommerce/user/entity/Role.java
package com.ecommerce.user.entity;

public enum Role {
    USER,
    ADMIN,
    CUSTOMER
}
```

Using an enum instead of a String column gives you:
- Compile-time safety — you cannot accidentally assign `"ADMIN123"`
- `@Enumerated(EnumType.STRING)` stores the name (`"CUSTOMER"`, `"ADMIN"`) — readable in the DB, safe if enum order changes
- Type-safe comparisons in business logic: `if (customer.getRole() == Role.ADMIN)`

---

### 5.4.2 `User.java` — `@MappedSuperclass` implementing `UserDetails`

```java
// user-service/src/main/java/com/ecommerce/user/entity/User.java
package com.ecommerce.user.entity;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // ── UserDetails interface methods ──────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
```

**Key design decisions:**

| Annotation / Concept | Why |
|---|---|
| `@MappedSuperclass` | Fields (`id`, `username`, `password`, `role`) are inherited into the `customers` table — no separate `users` table is created |
| `implements UserDetails` | Spring Security's `DaoAuthenticationProvider` and `JwtAuthenticationFilter` can use `Customer` objects directly without adapters |
| `@JsonProperty(WRITE_ONLY)` | Password is accepted in request bodies (register) but never serialized into JSON responses |
| `@SuperBuilder` | Enables `Customer.builder().username(...).password(...).firstName(...)...build()` — the builder spans the full inheritance hierarchy |
| `getAuthorities()` | Returns `ROLE_CUSTOMER` or `ROLE_ADMIN` — Spring Security's convention requires the `ROLE_` prefix |

**`@MappedSuperclass` vs `@Entity` inheritance:**

```
@MappedSuperclass (what we use):
  → No "users" table in the database
  → Customer table has ALL columns: id, username, password, role, firstName, lastName, mobileNumber, createdAt
  → Simple, single-table design

@Inheritance (alternative):
  → Creates a "users" table + a "customers" table (joined or single)
  → More complex SQL joins for every query
  → Overkill when you have one concrete subclass
```

---

### 5.4.3 `Customer.java` — the concrete `@Entity`

```java
// user-service/src/main/java/com/ecommerce/user/entity/Customer.java
package com.ecommerce.user.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "customers")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends User {

    private String firstName;

    private String lastName;

    @Column(unique = true)
    private String mobileNumber;

    @Builder.Default
    @Column(name = "createdAt")
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

**Why `@Builder.Default` on `createdAt`?**

When using `@SuperBuilder`, Lombok generates a builder that ignores field-level defaults (e.g. `= LocalDateTime.now()`). Without `@Builder.Default`, the builder would set `createdAt` to `null`. The annotation tells Lombok to use the declared default when the builder method is not called.

```java
// Without @Builder.Default:
Customer.builder().username("alice").build()
// → createdAt = null ← WRONG

// With @Builder.Default:
Customer.builder().username("alice").build()
// → createdAt = LocalDateTime.now() ← correct
```

**Resulting database table structure (`customers`):**

```
id           BIGINT AUTO_INCREMENT PRIMARY KEY
username     VARCHAR(255) UNIQUE NOT NULL
password     VARCHAR(255) NOT NULL          ← BCrypt hash
role         VARCHAR(255) NOT NULL          ← "CUSTOMER" or "ADMIN"
firstName    VARCHAR(255)
lastName     VARCHAR(255)
mobileNumber VARCHAR(255) UNIQUE
createdAt    DATETIME
```

---

## 5.5 Repository — `CustomerRepository.java`

```java
// user-service/src/main/java/com/ecommerce/user/repository/CustomerRepository.java
package com.ecommerce.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.user.entity.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByUsername(String username);

    boolean existsByUsername(String username);
}
```

`findByUsername` is a **derived query** — Spring Data JPA reads the method name and generates:
```sql
SELECT * FROM customers WHERE username = ?
```

`existsByUsername` generates:
```sql
SELECT COUNT(*) > 0 FROM customers WHERE username = ?
```

This is used in `AuthController.register()` to prevent duplicate registrations without needing to load the full entity.

---

## 5.6 DTOs — Java Records

Java records (introduced in Java 16) are perfect for DTOs: immutable, auto-generate `equals`/`hashCode`/`toString`, and compact syntax.

### `RegisterRequest.java`

```java
// user-service/src/main/java/com/ecommerce/user/dto/RegisterRequest.java
package com.ecommerce.user.dto;

import com.ecommerce.user.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,

        String firstName,

        String lastName,

        @NotBlank(message = "Mobile Number is required")
        String mobileNumber,

        Role role   // optional — defaults to CUSTOMER if null in AuthController
) {}
```

### `LoginRequest.java`

```java
// user-service/src/main/java/com/ecommerce/user/dto/LoginRequest.java
package com.ecommerce.user.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {}
```

### `AuthResponse.java`

```java
// user-service/src/main/java/com/ecommerce/user/dto/AuthResponse.java
package com.ecommerce.user.dto;

public record AuthResponse(
        String token,
        String username,
        String role
) {}
```

**Why records instead of classes?**

```java
// Traditional class DTO (verbose):
public class AuthResponse {
    private String token;
    private String username;
    private String role;
    // constructor, getters, equals, hashCode, toString...
}

// Record (compact):
public record AuthResponse(String token, String username, String role) {}
```

Records automatically provide:
- A canonical constructor with all fields
- Accessor methods (`token()`, `username()`, `role()`)
- `equals()`, `hashCode()`, `toString()`

> **Accessing record fields:** Use `request.username()`, not `request.getUsername()`. Records use accessor methods without the `get` prefix.

---

## 5.7 Security Layer

### 5.7.1 `JwtService.java`

`JwtService` handles token generation, validation, and claim extraction. It lives in the `security` package.

```java
// user-service/src/main/java/com/ecommerce/user/security/JwtService.java
package com.ecommerce.user.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.ecommerce.user.entity.Customer;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKeyString;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpirationMs;

    // ── Key ───────────────────────────────────────────────────────────────

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKeyString.getBytes(StandardCharsets.UTF_8));
    }

    // ── Generate Token ────────────────────────────────────────────────────

    public String generateToken(Customer customer) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", customer.getAuthorities().iterator().next().getAuthority());
        extraClaims.put("userId", customer.getId());
        return buildToken(extraClaims, customer);
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", userDetails.getAuthorities().iterator().next().getAuthority());
        return buildToken(extraClaims, userDetails);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    // ── Validate Token ────────────────────────────────────────────────────

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ── Extract Claims ────────────────────────────────────────────────────

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

**JWT structure — what's inside the token:**

A JWT has three Base64-encoded parts separated by `.`:

```
eyJhbGciOiJIUzI1NiJ9          ← Header:  {"alg":"HS256"}
.
eyJyb2xlIjoiUk9MRV9DVVNUT01FUiIsInVzZXJJZCI6MSwic3ViIjoiYWxpY2UiLCJpYXQiOjE2OTk5OTk5OTksImV4cCI6MTcwMDA4NjM5OX0
                                ← Payload: {"role":"ROLE_CUSTOMER","userId":1,"sub":"alice","iat":...,"exp":...}
.
<signature>                    ← HMAC-SHA256 of header + payload using the secret key
```

**Custom claims added by `JwtService`:**
- `role` — e.g. `"ROLE_CUSTOMER"` or `"ROLE_ADMIN"`
- `userId` — the database ID, forwarded as `X-User-Id` by the API Gateway

**Key derivation — why UTF-8 bytes (not Base64):**

The API Gateway's `JwtUtil` uses:
```java
Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))
```

`JwtService` uses the **same approach** — this is critical. If one used Base64 decoding and the other used raw bytes, the Gateway would reject every token generated by User Service with a signature verification failure.

---

### 5.7.2 `JwtAuthenticationFilter.java`

This filter intercepts all requests to the User Service (for the `/api/customers/**` protected endpoints) and validates the JWT before Spring Security processes the request.

```java
// user-service/src/main/java/com/ecommerce/user/security/JwtAuthenticationFilter.java
package com.ecommerce.user.security;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Extract the Authorization header
        final String authHeader = request.getHeader("Authorization");

        // 2. If header is missing or doesn't start with "Bearer ", skip this filter
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract the JWT token (remove "Bearer " prefix)
        final String jwt = authHeader.substring(7);

        // 4. Extract the username from the token
        final String username = jwtService.extractUsername(jwt);

        // 5. If username found and no authentication set yet in context
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 6. Load user from database
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 7. Validate the token against the loaded user
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // 8. Create an authentication token
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                // 9. Attach request details
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 10. Set the authentication in the SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 11. Always continue the filter chain
        filterChain.doFilter(request, response);
    }
}
```

**Request processing flow — step by step:**

```
Incoming request (e.g. GET /api/customers)
    │
    ▼
JwtAuthenticationFilter.doFilterInternal()
    │
    ├─ No "Authorization: Bearer ..." header?
    │       └─ Pass through (will be rejected by SecurityFilterChain as unauthenticated)
    │
    ├─ Has header → extract JWT → extract username from claims
    │
    ├─ Username found + no existing authentication in SecurityContext?
    │       └─ Load Customer from database (via UserDetailsServiceImpl)
    │       └─ Validate: token signature matches + not expired + username matches
    │       └─ Set UsernamePasswordAuthenticationToken into SecurityContext
    │
    └─ Always call filterChain.doFilter() — pass to next filter / controller
```

`OncePerRequestFilter` guarantees the filter runs **exactly once per request**, even in redirect scenarios.

---

### 5.7.3 `UserDetailsServiceImpl.java`

Spring Security's `DaoAuthenticationProvider` calls this during the login flow to load a user by username:

```java
// user-service/src/main/java/com/ecommerce/user/service/UserDetailsServiceImpl.java
package com.ecommerce.user.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ecommerce.user.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final CustomerRepository customerRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return customerRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
```

Because `Customer` extends `User` which implements `UserDetails`, the `Customer` object returned by the repository is directly usable as a `UserDetails` — no adapter class needed.

---

## 5.8 Service Layer — `CustomerService.java`

```java
// user-service/src/main/java/com/ecommerce/user/service/CustomerService.java
package com.ecommerce.user.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ecommerce.user.entity.Customer;
import com.ecommerce.user.repository.CustomerRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    public Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
    }

    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }

    public void deleteById(Long id) {
        customerRepository.deleteById(id);
    }
}
```

---

## 5.9 Controllers

### 5.9.1 `AuthController.java` — `/api/auth`

```java
// user-service/src/main/java/com/ecommerce/user/controller/AuthController.java
package com.ecommerce.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.user.dto.AuthResponse;
import com.ecommerce.user.dto.LoginRequest;
import com.ecommerce.user.dto.RegisterRequest;
import com.ecommerce.user.entity.Customer;
import com.ecommerce.user.entity.Role;
import com.ecommerce.user.repository.CustomerRepository;
import com.ecommerce.user.security.JwtService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {

        if (customerRepository.existsByUsername(request.username())) {
            throw new RuntimeException("Username already exists: " + request.username());
        }

        Customer customer = Customer.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role() != null ? request.role() : Role.CUSTOMER)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .mobileNumber(request.mobileNumber())
                .build();

        customerRepository.save(customer);

        String token = jwtService.generateToken(customer);

        return ResponseEntity.ok(new AuthResponse(token, customer.getUsername(), customer.getRole().name()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        Customer customer = customerRepository.findByUsername(request.username())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtService.generateToken(customer);

        return ResponseEntity.ok(new AuthResponse(token, customer.getUsername(), customer.getRole().name()));
    }
}
```

**Register flow (step by step):**

```
POST /api/auth/register
    │
    ├─ @Valid → validate RegisterRequest fields
    ├─ existsByUsername() → 400 if username taken
    ├─ Customer.builder() → build entity with BCrypt-encoded password
    │       role defaults to CUSTOMER if not provided in request
    ├─ customerRepository.save() → INSERT into customers table
    ├─ jwtService.generateToken(customer) → sign JWT with userId + role claims
    └─ return AuthResponse(token, username, role)
```

**Login flow (step by step):**

```
POST /api/auth/login
    │
    ├─ authenticationManager.authenticate()
    │       → DaoAuthenticationProvider
    │               → UserDetailsServiceImpl.loadUserByUsername()
    │               → BCryptPasswordEncoder.matches(rawPassword, storedHash)
    │               → throws BadCredentialsException if mismatch
    │
    ├─ customerRepository.findByUsername() → load full Customer entity
    ├─ jwtService.generateToken(customer) → sign JWT
    └─ return AuthResponse(token, username, role)
```

**Why call `authenticationManager.authenticate()` in login?**

Instead of loading the user and checking the password manually, we delegate to Spring Security's `AuthenticationManager`. It:
1. Calls `loadUserByUsername()` via `DaoAuthenticationProvider`
2. Verifies the raw password against the BCrypt hash using `PasswordEncoder`
3. Throws `BadCredentialsException` automatically if the password is wrong
4. Sets a proper authentication event in the security event system

This is cleaner than writing your own `if (!BCrypt.checkpw(...)) throw new RuntimeException(...)`.

---

### 5.9.2 `CustomerController.java` — `/api/customers`

```java
// user-service/src/main/java/com/ecommerce/user/controller/CustomerController.java
package com.ecommerce.user.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ecommerce.user.entity.Customer;
import com.ecommerce.user.service.CustomerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<List<Customer>> findAll() {
        return ResponseEntity.ok(customerService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> findById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Customer> update(@PathVariable Long id, @RequestBody Customer customer) {
        customer.setId(id);
        return ResponseEntity.ok(customerService.save(customer));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customerService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
```

All `/api/customers/**` endpoints require authentication (configured in `SecurityConfig`). In production, these would additionally require `ROLE_ADMIN`, but for this project any authenticated user can access them.

---

## 5.10 `SecurityConfig.java`

```java
// user-service/src/main/java/com/ecommerce/user/config/SecurityConfig.java
package com.ecommerce.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.ecommerce.user.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**Bean wiring explained:**

```
SecurityFilterChain
    │
    ├─ csrf disabled (REST API — no browser session cookies)
    ├─ SessionCreationPolicy.STATELESS (no HttpSession — JWT is the session)
    ├─ /api/auth/** → permitAll (register + login are public)
    ├─ everything else → authenticated
    ├─ authenticationProvider(DaoAuthenticationProvider)
    │       ├─ UserDetailsService → UserDetailsServiceImpl (loads Customer from DB)
    │       └─ PasswordEncoder → BCryptPasswordEncoder
    └─ JwtAuthenticationFilter added BEFORE UsernamePasswordAuthenticationFilter

AuthenticationManager
    └─ delegates to DaoAuthenticationProvider for username/password auth
       (used explicitly in AuthController.login())

PasswordEncoder
    └─ BCrypt — one-way hash with random salt; same password hashes differently each time
```

---

## 5.11 Package Structure

```
user-service/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── com/ecommerce/user/
        │       ├── UserServiceApplication.java
        │       ├── config/
        │       │   └── SecurityConfig.java           ← beans + filter chain
        │       ├── controller/
        │       │   ├── AuthController.java           ← /api/auth/register + /login
        │       │   └── CustomerController.java       ← /api/customers (protected)
        │       ├── dto/
        │       │   ├── RegisterRequest.java          ← Java record
        │       │   ├── LoginRequest.java             ← Java record
        │       │   └── AuthResponse.java             ← Java record
        │       ├── entity/
        │       │   ├── Role.java                     ← enum: USER, ADMIN, CUSTOMER
        │       │   ├── User.java                     ← @MappedSuperclass + UserDetails
        │       │   └── Customer.java                 ← @Entity, extends User
        │       ├── repository/
        │       │   └── CustomerRepository.java       ← findByUsername, existsByUsername
        │       ├── security/
        │       │   ├── JwtService.java               ← generate + validate tokens
        │       │   └── JwtAuthenticationFilter.java  ← OncePerRequestFilter
        │       └── service/
        │           ├── CustomerService.java          ← CRUD
        │           └── UserDetailsServiceImpl.java   ← loads Customer for Spring Security
        └── resources/
            └── application.yml
```

---

## 5.12 Startup & Validation

### Step 1 — Verify prerequisites

```
✓ Config Server  → http://localhost:8888
✓ Eureka Server  → http://localhost:8761
```

Verify Config Server is serving user-service properties:
```
GET http://localhost:8888/user-service/default
```
You should see the datasource URL and `jwt.secret` in the response.

### Step 2 — Start User Service

```bash
cd user-service
mvn spring-boot:run
```

Watch for these key log lines:
```
Fetching config from server at: http://localhost:8888
Located property source: CompositePropertySource [...]
Started UserServiceApplication in X.XXX seconds
Registering application USER-SERVICE with eureka
```

### Step 3 — Verify Eureka registration

Open `http://localhost:8761` — `USER-SERVICE` should appear in the instances list.

---

## 5.13 Postman Validation

> **Setup:** Open your Postman collection. All requests should use `{{baseUrl}}` = `http://localhost:8081`.
> The login request automatically saves the token to `{{token}}` via its test script.

---

### Scenario 1: Register as CUSTOMER

**Request:** `POST {{baseUrl}}/api/auth/register`

**Body (JSON):**
```json
{
  "username": "alice",
  "password": "password123",
  "firstName": "Alice",
  "lastName": "Smith",
  "mobileNumber": "0412345678"
}
```

Note: `role` is omitted — it will default to `CUSTOMER` in `AuthController`.

**Expected response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "alice",
  "role": "CUSTOMER"
}
```

**What to verify:**
- Status is `200 OK`
- `token` field is present and non-empty
- `role` is `"CUSTOMER"`
- Copy the token — the test script should save it to `{{token}}` automatically

---

### Scenario 2: Register as ADMIN

**Request:** `POST {{baseUrl}}/api/auth/register`

**Body (JSON):**
```json
{
  "username": "admin",
  "password": "admin123",
  "firstName": "Admin",
  "lastName": "User",
  "mobileNumber": "0499999999",
  "role": "ADMIN"
}
```

**Expected response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin",
  "role": "ADMIN"
}
```

**What to verify:**
- `role` is `"ADMIN"` (not `"CUSTOMER"`)

---

### Scenario 3: Login as CUSTOMER

**Request:** `POST {{baseUrl}}/api/auth/login`

**Body (JSON):**
```json
{
  "username": "alice",
  "password": "password123"
}
```

**Expected response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "alice",
  "role": "CUSTOMER"
}
```

**What to verify:**
- A new token is returned (different from the register token due to different `iat` timestamp)
- Wrong password → should return `403 Forbidden` from Spring Security

---

### Scenario 4: Login as ADMIN

**Request:** `POST {{baseUrl}}/api/auth/login`

**Body (JSON):**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Expected response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin",
  "role": "ADMIN"
}
```

---

### Scenario 5: Access protected endpoint WITH token

**Request:** `GET {{baseUrl}}/api/customers`

**Headers:** `Authorization: Bearer {{token}}`

**Expected response (200 OK):** JSON array of all registered customers:
```json
[
  {
    "id": 1,
    "username": "alice",
    "firstName": "Alice",
    "lastName": "Smith",
    "mobileNumber": "0412345678",
    "role": "CUSTOMER",
    "createdAt": "2024-01-15T10:30:00"
  },
  ...
]
```

Note: `password` is NOT included — `@JsonProperty(WRITE_ONLY)` suppresses it.

---

### Scenario 6: Access protected endpoint WITHOUT token

**Request:** `GET {{baseUrl}}/api/customers`

**Headers:** *(no Authorization header)*

**Expected response:** `403 Forbidden`

This confirms `SecurityConfig` is correctly protecting the `/api/customers/**` endpoints.

---

### Scenario 7: Duplicate username check

**Request:** `POST {{baseUrl}}/api/auth/register` with `"username": "alice"` again

**Expected response:** `500` with `"Username already exists: alice"` in the error message

---

## 5.14 JWT Anatomy — Inspecting Your Token

Paste any token from the above steps into [jwt.io](https://jwt.io) and you should see:

**Header:**
```json
{
  "alg": "HS256"
}
```

**Payload:**
```json
{
  "role": "ROLE_CUSTOMER",
  "userId": 1,
  "sub": "alice",
  "iat": 1699999999,
  "exp": 1700086399
}
```

- `sub` — the subject (username)
- `iat` — issued at (Unix timestamp)
- `exp` — expiration (iat + 86400000ms = iat + 24 hours)
- `role` — with `ROLE_` prefix (Spring Security convention)
- `userId` — the database `id` — the Gateway extracts this to inject `X-User-Id`

---

## 5.15 Common Mistakes

### Mistake 1: Using `@Builder` instead of `@SuperBuilder` on parent class

```java
// WRONG — Customer.builder() won't include username/password from User
@Builder          // ← wrong annotation on @MappedSuperclass
public class User implements UserDetails { ... }

// CORRECT
@SuperBuilder     // ← must be on both User and Customer
public class User implements UserDetails { ... }
```

### Mistake 2: Missing `@Builder.Default` on `createdAt`

```java
// WRONG — createdAt will be null when using builder
private LocalDateTime createdAt = LocalDateTime.now();

// CORRECT
@Builder.Default
private LocalDateTime createdAt = LocalDateTime.now();
```

### Mistake 3: Record field access syntax

```java
// WRONG — records don't have getters with "get" prefix
request.getUsername()   // CompileError: cannot find symbol

// CORRECT — records use accessor methods
request.username()
request.password()
```

### Mistake 4: JWT key derivation mismatch

```java
// User Service (JwtService.java) — uses raw UTF-8 bytes
Keys.hmacShaKeyFor(secretKeyString.getBytes(StandardCharsets.UTF_8))

// API Gateway (JwtUtil.java) — must also use raw UTF-8 bytes
Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))

// If one uses Base64 and the other uses UTF-8 → signature verification fails
// → Gateway returns 401 for every token issued by User Service
```

### Mistake 5: Using `@Autowired` on final fields with `@RequiredArgsConstructor`

```java
// WRONG — mixing injection styles, @RequiredArgsConstructor won't inject @Autowired fields
@Autowired
private final JwtService jwtService;

// CORRECT — just declare final + use @RequiredArgsConstructor
private final JwtService jwtService;
// Lombok generates: public JwtAuthenticationFilter(JwtService jwtService, ...) {}
```

---

## Module Checkpoint

Before moving to Module 6 (Product Service), confirm:

- [ ] `GET http://localhost:8888/user-service/default` returns datasource config and `jwt.secret`
- [ ] User Service starts without errors (`Started UserServiceApplication`)
- [ ] `USER-SERVICE` appears in the Eureka dashboard at `http://localhost:8761`
- [ ] `POST /api/auth/register` with CUSTOMER role → 200 with token and `"role":"CUSTOMER"`
- [ ] `POST /api/auth/register` with `"role":"ADMIN"` → 200 with `"role":"ADMIN"`
- [ ] `POST /api/auth/login` with correct credentials → 200 with fresh token
- [ ] `POST /api/auth/login` with wrong password → 403
- [ ] `GET /api/customers` with Bearer token → 200 with customer list
- [ ] `GET /api/customers` without token → 403
- [ ] Token decoded at jwt.io shows `userId`, `role`, `sub`, `iat`, `exp`

---

## What's Next — Module 6

With the User Service running and JWT generation working, Module 6 builds the **Product Service** — the product catalogue. Products are publicly readable (no token required), and their stock is managed by the Order Service via OpenFeign. You will also validate calling product endpoints **through the API Gateway** for the first time.
