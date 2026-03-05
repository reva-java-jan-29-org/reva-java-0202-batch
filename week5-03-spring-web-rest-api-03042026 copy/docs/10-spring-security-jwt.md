# 10 — Spring Security & JWT Authentication

## Navigation
[← 09 Interview Questions](09-interview-questions.md) | [↑ Overview](00-overview.md)

---

## Table of Contents

1. [What is Spring Security?](#1-what-is-spring-security)
2. [Spring Security Architecture](#2-spring-security-architecture)
3. [What is JWT?](#3-what-is-jwt)
4. [How JWT Authentication Works](#4-how-jwt-authentication-works)
5. [Step 1 — Add Dependencies](#step-1--add-dependencies)
6. [Step 2 — Create Role Enum](#step-2--create-role-enum)
7. [Step 3 — Create User Entity](#step-3--create-user-entity)
8. [Step 4 — UserRepository](#step-4--userrepository)
9. [Step 5 — UserDetailsService Implementation](#step-5--userdetailsservice-implementation)
10. [Step 6 — JWT Utility Class](#step-6--jwt-utility-class)
11. [Step 7 — JWT Authentication Filter](#step-7--jwt-authentication-filter)
12. [Step 8 — Security Configuration](#step-8--security-configuration)
13. [Step 9 — Auth DTOs](#step-9--auth-dtos)
14. [Step 10 — AuthController](#step-10--authcontroller)
15. [Step 11 — Securing Existing Endpoints](#step-11--securing-existing-endpoints)
16. [Step 12 — application.properties Updates](#step-12--applicationproperties-updates)
17. [Testing with curl](#testing-with-curl)
18. [How Each Piece Fits Together](#how-each-piece-fits-together)
19. [Common Errors & Fixes](#common-errors--fixes)

---

## 1. What is Spring Security?

Spring Security is a **powerful, highly customizable authentication and authorization framework** for Java applications. It is the de-facto standard for securing Spring-based applications.

### What does it give you?

| Feature | Description |
|---------|-------------|
| **Authentication** | Who are you? (login, verify identity) |
| **Authorization** | What can you do? (roles, permissions) |
| **Password encoding** | BCrypt hashing out-of-the-box |
| **CSRF protection** | Prevents cross-site request forgery |
| **Session management** | Stateless (JWT) or stateful (session cookie) |
| **Filter chain** | Every HTTP request passes through a chain of security filters |

### Key Concepts

- **Authentication** — The process of verifying *who* a user is. Example: checking username + password.
- **Authorization** — The process of verifying *what* a user is allowed to do. Example: only `ADMIN` can delete products.
- **Principal** — The currently authenticated user object.
- **SecurityContext** — A thread-local holder for the current authentication. Spring Security stores the authenticated user here after login.
- **GrantedAuthority** — Represents a permission/role granted to a user (e.g., `ROLE_ADMIN`, `ROLE_USER`).

---

## 2. Spring Security Architecture

When a request comes in, it passes through the **Security Filter Chain** before reaching your controller.

```
HTTP Request
     │
     ▼
┌─────────────────────────────────────────────────────┐
│              Security Filter Chain                  │
│                                                     │
│  ┌──────────────────────────────────────────────┐  │
│  │  JwtAuthenticationFilter  (your custom filter)│  │
│  │  → reads Authorization header                 │  │
│  │  → validates JWT token                        │  │
│  │  → sets SecurityContext                       │  │
│  └──────────────────────────────────────────────┘  │
│                         │                           │
│  ┌──────────────────────▼───────────────────────┐  │
│  │  AuthorizationFilter                          │  │
│  │  → checks if user has required role/permission│  │
│  └──────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
     │
     ▼
DispatcherServlet → Controller
```

### What happens without a valid token?

- If no token or invalid token → `401 Unauthorized`
- If valid token but wrong role → `403 Forbidden`
- If valid token with correct role → request reaches controller

### The SecurityContext

```
Per-Thread Storage (ThreadLocal)
┌─────────────────────────────┐
│  SecurityContext            │
│  ┌─────────────────────┐   │
│  │  Authentication     │   │
│  │  ├── principal      │   │  ← UserDetails object (your User)
│  │  ├── credentials    │   │  ← null after auth (cleared for safety)
│  │  └── authorities   │   │  ← [ROLE_ADMIN], [ROLE_USER]
│  └─────────────────────┘   │
└─────────────────────────────┘
```

After your JWT filter validates the token, it places an `Authentication` object into the `SecurityContext`. From that point forward, Spring Security knows who the user is.

---

## 3. What is JWT?

**JWT (JSON Web Token)** is a compact, URL-safe way of representing claims (data) between two parties. It is a **self-contained token** — the server does not need to look up a session in a database to verify it.

### JWT Structure

A JWT has three parts separated by dots (`.`):

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9    ← Header (Base64 encoded)
.
eyJzdWIiOiJ2aXNoYWwiLCJyb2xlIjoiQURNSU4iLCJpYXQiOjE3MDk1MDQwMDAsImV4cCI6MTcwOTU5MDQwMH0   ← Payload (Base64 encoded)
.
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c   ← Signature (HMAC-SHA256)
```

**Decoded Header:**
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**Decoded Payload:**
```json
{
  "sub": "vishal",
  "role": "ADMIN",
  "iat": 1709504000,
  "exp": 1709590400
}
```

| Claim | Meaning |
|-------|---------|
| `sub` | Subject — who the token is about (username) |
| `iat` | Issued At — when the token was created |
| `exp` | Expiration — when the token expires |
| `role` | Custom claim — the user's role |

### How the Signature Works

```
Signature = HMAC-SHA256(
    base64Url(header) + "." + base64Url(payload),
    SECRET_KEY
)
```

The **SECRET_KEY** is known only to your server. Any tampering with header or payload invalidates the signature, so the server immediately detects it.

> **Important:** JWT payload is only Base64 encoded, NOT encrypted. Do not store sensitive data (passwords, credit cards) in JWT.

---

## 4. How JWT Authentication Works

### Registration Flow

```
Client                          Server
  │                               │
  │  POST /api/auth/register      │
  │  { username, password, role } │
  │ ─────────────────────────────►│
  │                               │  1. Validate input
  │                               │  2. Hash password with BCrypt
  │                               │  3. Save User to database
  │                               │  4. Generate JWT token
  │◄─────────────────────────────│
  │  { token: "eyJ..." }          │
```

### Login Flow

```
Client                          Server
  │                               │
  │  POST /api/auth/login         │
  │  { username, password }       │
  │ ─────────────────────────────►│
  │                               │  1. Load user from DB
  │                               │  2. Compare BCrypt hash
  │                               │  3. If match → generate JWT
  │◄─────────────────────────────│
  │  { token: "eyJ..." }          │
```

### Subsequent Authenticated Request Flow

```
Client                          Server
  │                               │
  │  GET /api/products            │
  │  Authorization: Bearer eyJ..  │
  │ ─────────────────────────────►│
  │                               │  JwtAuthenticationFilter:
  │                               │  1. Extract token from header
  │                               │  2. Validate signature + expiry
  │                               │  3. Extract username from token
  │                               │  4. Load UserDetails from DB
  │                               │  5. Set Authentication in SecurityContext
  │                               │
  │                               │  AuthorizationFilter:
  │                               │  6. Check if user has required role
  │                               │
  │                               │  Controller:
  │                               │  7. Process request
  │◄─────────────────────────────│
  │  200 OK + response body       │
```

---

## Step 1 — Add Dependencies

Open `pom.xml` and add these three dependencies inside `<dependencies>`:

```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JJWT API — JWT library for creating and parsing JWTs -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>

<!-- JJWT Implementation — runtime only, not needed at compile time -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>

<!-- JJWT Jackson — for JSON serialization of JWT claims -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

### Why JJWT?

JJWT (Java JWT) is the most popular JWT library for Java. Version 0.12.x introduced a cleaner, fluent API. We split it into three artifacts:

- `jjwt-api` — interfaces and builder classes (compile scope)
- `jjwt-impl` — the actual implementation (runtime — Spring loads it automatically)
- `jjwt-jackson` — uses Jackson to serialize claims to/from JSON (runtime)

> **What happens after adding `spring-boot-starter-security`?**
> Spring Boot auto-configures a default security setup that blocks ALL endpoints with HTTP Basic auth and prints a random password to the console. We will completely replace this with our own config.

---

## Step 2 — Create Role Enum

**File:** `src/main/java/com/training/security/Role.java`

```java
package com.training.security;

public enum Role {
    USER,
    ADMIN
}
```

### Why an enum?

Enums in Java are type-safe constants. By storing the role as an enum in the database (as a `VARCHAR`), we get:
- Compile-time checking (can't typo a role name)
- Clean, readable code
- Easy comparison in security rules

> Spring Data JPA will store this as the string `"USER"` or `"ADMIN"` in the database by default (using `@Enumerated(EnumType.STRING)`).

---

## Step 3 — Create User Entity

**File:** `src/main/java/com/training/security/User.java`

```java
package com.training.security;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;  // BCrypt hashed, NEVER plain text

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // ── UserDetails interface methods ──────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring Security expects role names prefixed with "ROLE_"
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;  // Simplification: accounts never expire
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;  // Simplification: accounts never get locked
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;  // Simplification: credentials never expire
    }

    @Override
    public boolean isEnabled() {
        return true;  // Simplification: all accounts are enabled
    }
}
```

### Key Concepts Here

**Why does `User` implement `UserDetails`?**

`UserDetails` is a Spring Security interface that represents a user in the security context. By implementing it, our `User` entity is directly usable by Spring Security without any adapter class. Spring Security will call `getAuthorities()` to know what roles the user has.

**What is `GrantedAuthority`?**

A `GrantedAuthority` represents a permission granted to a user. `SimpleGrantedAuthority` is the simplest implementation — just a string like `"ROLE_ADMIN"`.

> **ROLE_ prefix convention:** Spring Security's `hasRole("ADMIN")` automatically prepends `ROLE_`. So `hasRole("ADMIN")` checks for the authority `"ROLE_ADMIN"`. Always store authorities with the `ROLE_` prefix when using `hasRole()`.

**`@Enumerated(EnumType.STRING)`**

Without this, JPA stores the enum as its **ordinal** (0 for USER, 1 for ADMIN). This is fragile — if you reorder the enum, data is corrupted. `EnumType.STRING` stores the actual name `"USER"` or `"ADMIN"`, which is safe.

---

## Step 4 — UserRepository

**File:** `src/main/java/com/training/security/UserRepository.java`

```java
package com.training.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
```

`Optional<User>` — Spring Security's `UserDetailsService` requires us to find a user by username, so we need this derived query method.

---

## Step 5 — UserDetailsService Implementation

**File:** `src/main/java/com/training/security/UserDetailsServiceImpl.java`

```java
package com.training.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
```

### Why does Spring Security need `UserDetailsService`?

This is the **bridge** between Spring Security and your database. Spring Security knows nothing about MySQL or JPA. During authentication (login), it calls `loadUserByUsername()` to fetch the user from wherever you stored them. You implement this method and do the database lookup.

```
Spring Security                    Your Code
      │                                │
      │  loadUserByUsername("vishal") ──►  │
      │                                │  userRepository.findByUsername("vishal")
      │◄── UserDetails object ─────────│  return User from DB
      │                                │
      │  compare passwords (BCrypt)    │
```

---

## Step 6 — JWT Utility Class

**File:** `src/main/java/com/training/security/JwtService.java`

```java
package com.training.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secretKeyString;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    // ── Key ───────────────────────────────────────────────────────────────

    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKeyString);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ── Generate Token ────────────────────────────────────────────────────

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        // Add the role as a custom claim in the token payload
        extraClaims.put("role", userDetails.getAuthorities()
                .iterator().next().getAuthority());
        return buildToken(extraClaims, userDetails);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())         // "sub" claim
                .issuedAt(new Date(System.currentTimeMillis()))  // "iat" claim
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs)) // "exp" claim
                .signWith(getSigningKey())
                .compact();  // serializes to "header.payload.signature"
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
                .verifyWith(getSigningKey())  // verifies signature
                .build()
                .parseSignedClaims(token)     // parses and validates
                .getPayload();                // returns claims map
    }
}
```

### Understanding Each Method

**`getSigningKey()`**
Converts the Base64-encoded secret string from `application.properties` into a `SecretKey` object. `Keys.hmacShaKeyFor()` creates a key suitable for HMAC-SHA256 signing.

**`generateToken()`**
Builds a JWT with:
- Custom claims (extra data — we store the role here)
- `subject` = username
- `issuedAt` = now
- `expiration` = now + configured duration
- Signs with our secret key

**`isTokenValid()`**
Two checks:
1. The username in the token matches the loaded `UserDetails`
2. The token is not expired

**`extractAllClaims()`**
Parses the JWT, automatically verifying the signature. If the signature doesn't match or the token is expired, JJWT throws a `JwtException`.

> **Why `Function<Claims, T> claimsResolver`?**
> This is a functional interface pattern. Instead of writing separate methods for each claim (`extractSubject()`, `extractExpiration()`...), we write one generic `extractClaim()` and pass a method reference: `Claims::getSubject`, `Claims::getExpiration`.

---

## Step 7 — JWT Authentication Filter

**File:** `src/main/java/com/training/security/JwtAuthenticationFilter.java`

```java
package com.training.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

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
            filterChain.doFilter(request, response);  // pass to next filter
            return;
        }

        // 3. Extract the JWT token (remove "Bearer " prefix = 7 characters)
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
                        null,               // credentials — set to null after auth
                        userDetails.getAuthorities()  // roles/permissions
                );

                // 9. Attach request details (IP, session id, etc.)
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

### Why extend `OncePerRequestFilter`?

Spring's filter chain can invoke filters multiple times per request in some configurations (e.g., with forwards and includes). `OncePerRequestFilter` guarantees your filter runs **exactly once per HTTP request**, which is what we want for authentication.

### The Filter Logic Step by Step

```
Request: GET /api/products
         Authorization: Bearer eyJhbGci...

JwtAuthenticationFilter:
  Step 1: Read "Authorization" header
  Step 2: Does it start with "Bearer "?
          NO  → skip, pass to next filter (unauthenticated request)
          YES → continue
  Step 3: jwt = "eyJhbGci..."
  Step 4: username = jwtService.extractUsername(jwt) → "vishal"
  Step 5: Is SecurityContext empty? YES → continue
  Step 6: userDetails = loadUserByUsername("vishal") → User from DB
  Step 7: Is token valid and not expired? YES → continue
  Step 8: Create UsernamePasswordAuthenticationToken
          principal = userDetails (User object)
          authorities = [ROLE_ADMIN]
  Step 9: Attach request metadata
  Step 10: SecurityContextHolder.getContext().setAuthentication(authToken)
           ↑ NOW Spring Security knows who this user is
  Step 11: filterChain.doFilter() → passes to AuthorizationFilter → Controller
```

### What is `UsernamePasswordAuthenticationToken`?

This is Spring Security's concrete `Authentication` implementation for username/password-based authentication. It holds:
- `principal` — the authenticated user (`UserDetails`)
- `credentials` — the password (we set to `null` after auth — no need to keep it in memory)
- `authorities` — the roles/permissions

### `SecurityContextHolder.getContext().setAuthentication()`

This is the critical line. It stores the authentication in a thread-local variable. For the rest of this request's lifecycle (in any filter, service, or controller), you can retrieve the current user with:

```java
SecurityContextHolder.getContext().getAuthentication().getPrincipal()
```

---

## Step 8 — Security Configuration

**File:** `src/main/java/com/training/security/SecurityConfig.java`

```java
package com.training.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    // ── Security Filter Chain ─────────────────────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — not needed for stateless REST APIs
            // CSRF is for browser-based form submissions with session cookies
            .csrf(AbstractHttpConfigurer::disable)

            // Configure endpoint authorization rules
            .authorizeHttpRequests(auth -> auth

                // ── Public endpoints — no authentication required ─────────────
                .requestMatchers("/api/auth/**").permitAll()             // login, register
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()  // browse categories
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()    // browse products

                // ── Admin-only endpoints ──────────────────────────────────────
                .requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")

                .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

                // ── Everything else requires at least authentication ──────────
                .anyRequest().authenticated()
            )

            // Stateless session — Spring Security will NOT create HTTP sessions
            // Each request must carry its own JWT
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Register our custom authentication provider (DAO-based)
            .authenticationProvider(authenticationProvider())

            // Add JWT filter BEFORE Spring's built-in UsernamePasswordAuthenticationFilter
            // This ensures JWT is checked before any other authentication attempt
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ── Authentication Provider ────────────────────────────────────────────

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        // Tell Spring Security how to load users
        authProvider.setUserDetailsService(userDetailsService);
        // Tell Spring Security how to verify passwords (BCrypt)
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // ── Authentication Manager ─────────────────────────────────────────────

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    // ── Password Encoder ───────────────────────────────────────────────────

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### Understanding the Configuration

**`@EnableWebSecurity`**
Enables Spring Security's web security support. Without this, security is not applied.

**`@EnableMethodSecurity`**
Enables method-level security annotations like `@PreAuthorize("hasRole('ADMIN')")`. You can then annotate individual service or controller methods for fine-grained access control (optional, in addition to URL-based rules).

**Why disable CSRF?**
CSRF (Cross-Site Request Forgery) attacks require the browser to submit requests with session cookies. Since we use JWT in the `Authorization` header (not cookies), CSRF is irrelevant for our API. Disabling it simplifies our setup.

**`SessionCreationPolicy.STATELESS`**
Tells Spring Security never to create an `HttpSession`. Every request is completely independent and must carry its own JWT. This is the correct setting for REST APIs.

**`DaoAuthenticationProvider`**
A Spring Security `AuthenticationProvider` that uses a `UserDetailsService` to load users from a data store and a `PasswordEncoder` to verify passwords. "Dao" = Data Access Object pattern.

**`BCryptPasswordEncoder`**
BCrypt is a password hashing function designed to be slow (to resist brute-force attacks). Every call to `encode("password")` produces a different hash (using a random salt), but `matches("password", hash)` always returns true for the same input.

```
Plain text:  "mypassword"
BCrypt hash: "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
             ↑ cost factor 10 (logarithmic, default)
```

**`addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)`**
Inserts our JWT filter into the chain before the standard username/password filter. This ensures that for every request, we first try to authenticate via JWT. If the JWT filter populates the `SecurityContext`, the standard filter skips its logic.

### Authorization Rules — Reading the Order

Spring Security evaluates `requestMatchers` **in order** and uses the **first matching rule**. This is crucial:

```java
// More specific rules first
.requestMatchers("/api/auth/**").permitAll()              // 1st: auth endpoints, public
.requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()  // 2nd: GET products, public
.requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")  // 3rd: POST products, admin only
.anyRequest().authenticated()                            // Last: catch-all
```

If you put `anyRequest().authenticated()` first, everything would be blocked regardless of later rules.

---

## Step 9 — Auth DTOs

**File:** `src/main/java/com/training/security/RegisterRequest.java`

```java
package com.training.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    String username,

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    String password,

    Role role  // optional — defaults to USER if null
) {}
```

**File:** `src/main/java/com/training/security/LoginRequest.java`

```java
package com.training.security;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

    @NotBlank(message = "Username is required")
    String username,

    @NotBlank(message = "Password is required")
    String password
) {}
```

**File:** `src/main/java/com/training/security/AuthResponse.java`

```java
package com.training.security;

public record AuthResponse(
    String token,
    String username,
    String role
) {}
```

### Why Java Records?

Java records (introduced in Java 16, stable in Java 17) are perfect for simple, immutable data carriers like DTOs. They automatically generate:
- `final` fields
- Constructor
- Getters (field name without `get` prefix)
- `equals()`, `hashCode()`, `toString()`

---

## Step 10 — AuthController

**File:** `src/main/java/com/training/security/AuthController.java`

```java
package com.training.security;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // ── Register ──────────────────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {

        // Check if username is already taken
        if (userRepository.existsByUsername(request.username())) {
            throw new RuntimeException("Username already exists: " + request.username());
        }

        // Build the User entity
        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))  // HASH the password
                .role(request.role() != null ? request.role() : Role.USER)  // default to USER
                .build();

        // Save to database
        userRepository.save(user);

        // Generate JWT for the new user
        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(new AuthResponse(
                token,
                user.getUsername(),
                user.getRole().name()
        ));
    }

    // ── Login ─────────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {

        // AuthenticationManager handles the authentication:
        // 1. Loads user via UserDetailsService
        // 2. Verifies password using PasswordEncoder
        // 3. Throws AuthenticationException if invalid
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        // If authenticate() didn't throw, credentials are valid
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(new AuthResponse(
                token,
                user.getUsername(),
                user.getRole().name()
        ));
    }
}
```

### The Login Flow — `authenticationManager.authenticate()`

```
authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken("vishal", "password123")
)
     │
     ▼
DaoAuthenticationProvider
  1. userDetailsService.loadUserByUsername("vishal")
     → queries DB → returns User object
  2. passwordEncoder.matches("password123", user.getPassword())
     → BCrypt comparison
     → true? → Authentication successful
     → false? → throws BadCredentialsException → 401 Unauthorized
```

### Why not put auth logic in a Service?

For simplicity in this guide, the logic is in the controller. In production, you would typically create an `AuthService` to keep controllers thin. The pattern remains identical — just extract the logic.

---

## Step 11 — Securing Existing Endpoints

The security rules are already configured in `SecurityConfig.java`. Here is a summary of what is secured:

### Access Matrix

| Endpoint | Method | PUBLIC | USER | ADMIN |
|----------|--------|:------:|:----:|:-----:|
| `/api/auth/register` | POST | ✓ | ✓ | ✓ |
| `/api/auth/login` | POST | ✓ | ✓ | ✓ |
| `/api/categories` | GET | ✓ | ✓ | ✓ |
| `/api/categories/{id}` | GET | ✓ | ✓ | ✓ |
| `/api/categories` | POST | ✗ | ✗ | ✓ |
| `/api/categories/{id}` | PUT | ✗ | ✗ | ✓ |
| `/api/categories/{id}` | DELETE | ✗ | ✗ | ✓ |
| `/api/products` | GET | ✓ | ✓ | ✓ |
| `/api/products/{id}` | GET | ✓ | ✓ | ✓ |
| `/api/products` | POST | ✗ | ✗ | ✓ |
| `/api/products/{id}` | PUT | ✗ | ✗ | ✓ |
| `/api/products/{id}/stock` | PATCH | ✗ | ✗ | ✓ |
| `/api/products/{id}` | DELETE | ✗ | ✗ | ✓ |

### Optional: Method-Level Security with `@PreAuthorize`

If you prefer to annotate individual controller methods (enabled by `@EnableMethodSecurity`):

```java
// In ProductController.java

@PreAuthorize("hasRole('ADMIN')")
@PostMapping
public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
    // ...
}

@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
    // ...
}
```

This approach gives you fine-grained control per method and is very readable. Both URL-based and method-level rules can coexist — URL rules are checked first.

### Getting the Current User in a Controller

```java
// Option 1: Via SecurityContextHolder (works anywhere)
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
User currentUser = (User) authentication.getPrincipal();
String username = currentUser.getUsername();

// Option 2: Via @AuthenticationPrincipal (inject directly into method params)
@GetMapping("/me")
public ResponseEntity<String> me(@AuthenticationPrincipal User currentUser) {
    return ResponseEntity.ok("Hello, " + currentUser.getUsername());
}
```

`@AuthenticationPrincipal` is the cleaner approach — Spring automatically resolves the principal from the `SecurityContext` and injects it as a method parameter.

---

## Step 12 — application.properties Updates

Add these properties to `src/main/resources/application.properties`:

```properties
# ── JWT Configuration ──────────────────────────────────────────────────────
# Secret key: must be Base64-encoded, at least 256 bits (32 bytes) for HS256
# Generate with: openssl rand -base64 32
# IMPORTANT: Use environment variables or a secrets manager in production, NEVER hardcode!
app.jwt.secret=dGhpcyBpcyBhIHZlcnkgc2VjcmV0IGtleSBmb3IgSldUIDMyY2hhcnM=

# Token validity: 86400000 ms = 24 hours
app.jwt.expiration-ms=86400000
```

### Generating a Secure Secret Key

In a terminal:
```bash
openssl rand -base64 32
```
This generates a cryptographically secure, Base64-encoded, 32-byte (256-bit) key. Use this output as your `app.jwt.secret`.

> **Production Warning:** Never commit your secret key to Git. Use:
> - Environment variables (`JWT_SECRET=...`)
> - Spring Cloud Config with encryption
> - HashiCorp Vault or AWS Secrets Manager
> - Kubernetes Secrets

In Spring Boot, environment variables override `application.properties`:
```bash
export APP_JWT_SECRET=your_production_secret_here
```
Spring Boot automatically maps `APP_JWT_SECRET` → `app.jwt.secret`.

---

## Testing with curl

### 1. Register an admin user

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123",
    "role": "ADMIN"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin",
  "role": "ADMIN"
}
```

### 2. Register a regular user

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "vishal",
    "password": "vishal123"
  }'
```

### 3. Login and save the token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

Save the `token` value from the response as `ADMIN_TOKEN`.

### 4. Access a public endpoint (no token required)

```bash
curl http://localhost:8080/api/products
# 200 OK
```

### 5. Create a product as admin (token required)

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "MacBook Pro",
    "description": "Laptop",
    "price": 1999.99,
    "stockQuantity": 50,
    "categoryId": 1
  }'
# 201 Created
```

### 6. Try to create a product without a token

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{ "name": "Test" }'
# 401 Unauthorized
```

### 7. Try to create a product as a regular USER

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{ "name": "Test" }'
# 403 Forbidden
```

---

## How Each Piece Fits Together

```
┌──────────────────────────────────────────────────────────────────┐
│  Request: POST /api/products  +  Authorization: Bearer eyJ...    │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  JwtAuthenticationFilter                                         │
│  1. Extract token from header                                    │
│  2. jwtService.extractUsername(token)  → "admin"                 │
│  3. userDetailsService.loadUserByUsername("admin") → User(ADMIN) │
│  4. jwtService.isTokenValid(token, user) → true                  │
│  5. SecurityContextHolder.setAuthentication(authToken)           │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  AuthorizationFilter                                             │
│  SecurityConfig rule: POST /api/products → hasRole("ADMIN")      │
│  Current user authorities: [ROLE_ADMIN]                          │
│  Access granted ✓                                                │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  ProductController.createProduct()                               │
│  → ProductService → ProductRepository → DB                       │
│  ← 201 Created + ProductResponse                                 │
└──────────────────────────────────────────────────────────────────┘
```

### Component Responsibilities Summary

| Class | Package | Responsibility |
|-------|---------|----------------|
| `Role` | `security` | Enum defining available roles |
| `User` | `security` | JPA entity + UserDetails implementation |
| `UserRepository` | `security` | DB access for User entity |
| `UserDetailsServiceImpl` | `security` | Bridge between Spring Security and DB |
| `JwtService` | `security` | Generate, validate, and parse JWT tokens |
| `JwtAuthenticationFilter` | `security` | Per-request JWT extraction and validation |
| `SecurityConfig` | `security` | All security rules wired together |
| `RegisterRequest` | `security` | Incoming DTO for registration |
| `LoginRequest` | `security` | Incoming DTO for login |
| `AuthResponse` | `security` | Outgoing DTO with token |
| `AuthController` | `security` | `/api/auth/register` and `/api/auth/login` |

---

## Common Errors & Fixes

### `403 Forbidden` instead of `401 Unauthorized`

**Cause:** Your `SecurityConfig` `requestMatchers` order is wrong — a more general rule matches before the specific `permitAll()` rule.

**Fix:** Put `permitAll()` rules before `.anyRequest().authenticated()`. Always order from most-specific to least-specific.

---

### `JwtException: JWT signature does not match`

**Cause:** The secret key used to generate the token doesn't match the key used to verify it (e.g., after a server restart with a new random key, or misconfiguration).

**Fix:** Make sure `app.jwt.secret` in `application.properties` is consistent and does not change between restarts.

---

### `ClassCastException: User cannot be cast to UserDetails`

**Cause:** `User` class name conflicts with `org.springframework.security.core.userdetails.User` from Spring Security.

**Fix:** Use a different class name for your entity (e.g., `AppUser`) or use the fully qualified class name in imports.

---

### Password always invalid

**Cause:** Storing plain-text password and comparing with BCrypt (or vice versa).

**Fix:** Always use `passwordEncoder.encode()` when saving, and never compare raw passwords. `DaoAuthenticationProvider` handles comparison automatically.

---

### `IllegalArgumentException: Encoded password does not look like BCrypt`

**Cause:** Saved password is not BCrypt hashed (plain text in DB).

**Fix:** Clear the `users` table and re-register. Make sure `passwordEncoder.encode()` is called in the register endpoint.

---

### Token expired immediately

**Cause:** `app.jwt.expiration-ms` value is too small or there's a clock skew between client and server.

**Fix:** Check the value — `86400000` = 24 hours. Make sure server time is synchronized (NTP).

---

## Package Structure After Adding Security

```
src/main/java/com/training/
├── SpringWebRestApiApplication.java
├── category/
│   ├── Category.java
│   ├── CategoryController.java
│   ├── CategoryRepository.java
│   ├── CategoryRequest.java
│   ├── CategoryResponse.java
│   └── CategoryService.java
├── exception/
│   ├── DuplicateResourceException.java
│   ├── ErrorResponse.java
│   ├── GlobalExceptionHandler.java
│   └── ResourceNotFoundException.java
├── product/
│   ├── Product.java
│   ├── ProductController.java
│   ├── ProductRepository.java
│   ├── ProductRequest.java
│   ├── ProductResponse.java
│   └── ProductService.java
└── security/                      ← NEW
    ├── AuthController.java
    ├── AuthResponse.java
    ├── JwtAuthenticationFilter.java
    ├── JwtService.java
    ├── LoginRequest.java
    ├── RegisterRequest.java
    ├── Role.java
    ├── SecurityConfig.java
    ├── User.java
    ├── UserDetailsServiceImpl.java
    └── UserRepository.java
```

---

[← 09 Interview Questions](09-interview-questions.md) | [↑ Overview](00-overview.md)
