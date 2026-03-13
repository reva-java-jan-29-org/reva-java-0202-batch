# Module 7 — API Gateway: Routing, JWT Validation & Header Injection

**Type:** Hands-on Implementation
**Duration:** ~2 hours
**Prerequisites:** Modules 3–6 complete — Config Server, Eureka, User Service (:8081), and Product Service (:8082) all running
**Goal:** Understand how the API Gateway acts as the single entry point — validating JWT tokens, routing requests to downstream services via Eureka, and forwarding user identity as HTTP headers.

---

## Learning Objectives

By the end of this module you will be able to:

1. Explain what an API Gateway is and why it is the single entry point for all client traffic
2. Configure Spring Cloud Gateway routes using `lb://` URIs with Eureka
3. Implement a `GlobalFilter` to intercept every request
4. Distinguish between public and protected endpoints programmatically
5. Validate a JWT token and reject invalid requests before they reach downstream services
6. Forward `X-User-Id` and `X-Username` headers to downstream services
7. Explain the reactive WebFlux model used by Spring Cloud Gateway
8. Validate all routes through the gateway using Postman

---

## Recap — Where We Are

```
✅ Config Server    :8888  — Running
✅ Eureka Server    :8761  — Running
✅ User Service     :8081  — Running (generates JWT)
✅ Product Service  :8082  — Running (8 products seeded)
⬜ API Gateway      :8080  ← This module
⬜ Order Service    :8083
```

---

## 7.1 What the API Gateway Does

```
Internet / Postman / Browser
          │
          │  ALL traffic enters here
          ▼
    API Gateway :8080
          │
          ├─ /api/auth/**     → lb://user-service    (public — no JWT check)
          ├─ /api/customers/** → lb://user-service   (protected — JWT required)
          ├─ /api/products/**  → lb://product-service (GET = public, others = protected)
          ├─ /api/cart/**      → lb://order-service   (protected)
          └─ /api/orders/**    → lb://order-service   (protected)
                │
                └─ Downstream services never see the internet directly
                   They trust X-User-Id and X-Username headers from the gateway
```

**The gateway's responsibilities:**
1. **Single entry point** — all traffic goes through port 8080
2. **JWT validation** — checks signature, expiry; rejects invalid tokens
3. **Header injection** — adds `X-User-Id` and `X-Username` for downstream services
4. **Load-balanced routing** — `lb://service-name` resolves via Eureka

---

## 7.2 Maven Dependencies — `api-gateway/pom.xml`

```xml
<dependencies>
    <!-- Spring Cloud Gateway (reactive WebFlux-based) -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>

    <!-- Eureka client — for lb:// URI resolution -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>

    <!-- JJWT — JWT validation (same version as user-service) -->
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

    <!-- Actuator -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
</dependencies>
```

> **Important:** `spring-cloud-starter-gateway` uses **WebFlux** (reactive / non-blocking). Do NOT add `spring-boot-starter-web` — it will cause a conflict between the servlet and reactive stacks.

---

## 7.3 Configuration — `application.yml`

```yaml
# api-gateway/src/main/resources/application.yml

server:
  port: 8080

spring:
  application:
    name: api-gateway
  main:
    web-application-type: reactive      # prevents servlet/WebFlux conflict
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins: "*"
            allowedMethods: "*"
            allowedHeaders: "*"
      routes:
        - id: user-service-auth
          uri: lb://user-service
          predicates:
            - Path=/api/auth/**

        - id: user-service-customers
          uri: lb://user-service
          predicates:
            - Path=/api/customers/**

        - id: product-service
          uri: lb://product-service
          predicates:
            - Path=/api/products/**

        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/cart/**,/api/orders/**

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
    initial-instance-info-replication-interval-seconds: 5
    registry-fetch-interval-seconds: 5
  instance:
    prefer-ip-address: true
    instance-id: ${spring.application.name}:${server.port}
    lease-renewal-interval-in-seconds: 5
    lease-expiration-duration-in-seconds: 15

jwt:
  secret: ecommerce-jwt-secret-key-for-microservices-project-2024-secure

management:
  endpoints:
    web:
      exposure:
        include: "*"

logging:
  level:
    org.springframework.cloud.gateway: DEBUG
```

### Route configuration explained

Each route has three parts:

```yaml
- id: product-service           # ① Unique route identifier (for logs/debug)
  uri: lb://product-service     # ② Where to forward the request
  predicates:
    - Path=/api/products/**     # ③ Which paths match this route
```

**`lb://product-service`** — the `lb://` scheme tells Spring Cloud Gateway to use load-balanced resolution. It queries Eureka for instances registered as `PRODUCT-SERVICE` and picks one. Without Eureka, you would hard-code `http://localhost:8082`.

**Why split user-service into two routes?**

The user-service handles two distinct path prefixes:
- `/api/auth/**` — public (register, login)
- `/api/customers/**` — protected (list, update, delete customers)

They must be separate routes because the `JwtAuthFilter` applies different logic to each path. Both routes route to `lb://user-service` — the same service instance.

---

## 7.4 `JwtUtil.java` — Token Parsing

```java
// api-gateway/src/main/java/com/ecommerce/gateway/util/JwtUtil.java
package com.ecommerce.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return extractAllClaims(token).get("userId", Long.class);
    }
}
```

**Why the Gateway only validates, not generates:**

The Gateway reads the JWT secret from `application.yml` and uses it **only to verify** the token signature. It never generates tokens — that is exclusively the User Service's responsibility. This separation means:
- The secret is needed in only two places: user-service (sign) and api-gateway (verify)
- Other downstream services (product, order) never touch the JWT secret

**Key derivation — critical alignment:**

```java
// JwtUtil (gateway)       → raw UTF-8 bytes
Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))

// JwtService (user-service) → also raw UTF-8 bytes
Keys.hmacShaKeyFor(secretKeyString.getBytes(StandardCharsets.UTF_8))
```

These must match. If one used Base64 decoding and the other used raw bytes, every token would fail verification with `SignatureException`.

---

## 7.5 `JwtAuthFilter.java` — The GlobalFilter

```java
// api-gateway/src/main/java/com/ecommerce/gateway/filter/JwtAuthFilter.java
package com.ecommerce.gateway.filter;

import com.ecommerce.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtil jwtUtil;

    // Public endpoints that do not require JWT
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/auth/register",
            "/api/auth/login"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        // Allow public endpoints without auth
        if (isPublicEndpoint(path, method)) {
            return chain.filter(exchange);
        }

        // Check for Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        // Extract user info and inject into downstream headers
        Claims claims = jwtUtil.extractAllClaims(token);
        String username = claims.getSubject();
        Long userId = claims.get("userId", Long.class);

        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", userId != null ? userId.toString() : "")
                .header("X-Username", username != null ? username : "")
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private boolean isPublicEndpoint(String path, String method) {
        // Public: register and login
        if (PUBLIC_ENDPOINTS.contains(path)) {
            return true;
        }
        // Public: GET requests to /api/products/**
        if (method.equals("GET") && path.startsWith("/api/products")) {
            return true;
        }
        return false;
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -1;   // run before all other filters
    }
}
```

### The filter decision tree

```
Every incoming request
    │
    ▼
JwtAuthFilter.filter()
    │
    ├─ isPublicEndpoint(path, method)?
    │   ├─ /api/auth/register → YES → chain.filter() (pass through)
    │   ├─ /api/auth/login    → YES → chain.filter() (pass through)
    │   ├─ GET /api/products/** → YES → chain.filter() (public catalogue)
    │   └─ everything else    → NO → require JWT
    │
    ├─ Has "Authorization: Bearer <token>" header? NO → 401 UNAUTHORIZED
    │
    ├─ jwtUtil.isTokenValid(token)? NO → 401 UNAUTHORIZED
    │       (wrong signature, expired, malformed)
    │
    └─ Valid token:
            ├─ extractAllClaims() → get subject (username) + userId claim
            ├─ Mutate request: add X-User-Id and X-Username headers
            └─ chain.filter(modifiedExchange) → forward to downstream service
```

### Why `Ordered` with `getOrder() = -1`?

Spring Cloud Gateway applies filters in order. Returning `-1` from `getOrder()` places `JwtAuthFilter` **before** all default gateway filters, ensuring authentication happens first — before routing, load-balancing, or any other processing.

### Reactive model — `Mono<Void>` vs `void`

The Gateway uses **Project Reactor / WebFlux** instead of the standard Servlet API:

| Servlet (blocking) | WebFlux (reactive) |
|---|---|
| `void doFilterInternal(...)` | `Mono<Void> filter(...)` |
| `HttpServletRequest` | `ServerHttpRequest` |
| `HttpServletResponse` | `ServerHttpResponse` |
| Blocks a thread per request | Shares threads; non-blocking I/O |

`Mono<Void>` represents an asynchronous operation that completes with no value. `chain.filter(exchange)` returns a `Mono<Void>` representing the rest of the filter chain — reactive all the way through.

### Header injection — how downstream services identify the caller

```java
ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
        .header("X-User-Id", userId.toString())     // e.g. "1"
        .header("X-Username", username)              // e.g. "alice"
        .build();
```

`exchange.getRequest().mutate()` creates a **new immutable request** with the added headers. The original request is not modified. Downstream services (Order Service) read these headers:

```java
// OrderController — reads the injected header
@GetMapping
public ResponseEntity<List<OrderDto>> getMyOrders(
        @RequestHeader("X-User-Id") Long userId) {
    return ResponseEntity.ok(orderService.getUserOrders(userId));
}
```

This means downstream services never need to validate JWTs themselves — they simply trust the headers injected by the gateway. In production, this trust is enforced at the network level (services only accept connections from the gateway's internal IP range).

---

## 7.6 Package Structure

```
api-gateway/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── com/ecommerce/gateway/
        │       ├── ApiGatewayApplication.java
        │       ├── filter/
        │       │   └── JwtAuthFilter.java    ← GlobalFilter + Ordered
        │       └── util/
        │           └── JwtUtil.java          ← token parsing/validation
        └── resources/
            └── application.yml              ← port, routes, jwt.secret
```

> **Note:** No `SecurityConfig.java` in the gateway for this project. The JWT validation is fully handled by `JwtAuthFilter`. A reactive `SecurityFilterChain` could be added in production for defence in depth (e.g., rate limiting, IP filtering).

---

## 7.7 Startup & Validation

### Step 1 — Start the API Gateway

```bash
cd api-gateway
mvn spring-boot:run
```

Watch for:
```
Started ApiGatewayApplication in X.XXX seconds
Registering application API-GATEWAY with eureka
```

### Step 2 — Verify Eureka

Open `http://localhost:8761` — you should now see all four services registered:
- CONFIG-SERVER
- EUREKA (self)
- USER-SERVICE
- PRODUCT-SERVICE
- API-GATEWAY

### Step 3 — Test the gateway routing (DEBUG logs)

With `logging.level.org.springframework.cloud.gateway: DEBUG` set, watch the gateway logs when you send requests. You will see route matching and upstream forwarding.

---

## 7.8 Postman Validation

> **Setup:** Change `{{baseUrl}}` in your collection to `http://localhost:8080` (the gateway port) for all requests below.

---

### Test 1: Public endpoint — register through gateway

**Request:** `POST http://localhost:8080/api/auth/register`

**Body:**
```json
{
  "username": "gateway-user",
  "password": "pass123",
  "mobileNumber": "0411111111"
}
```

**Expected:** `200 OK` with token — the gateway forwarded to user-service without requiring auth.

---

### Test 2: Public endpoint — get all products through gateway

**Request:** `GET http://localhost:8080/api/products`

**Headers:** *(no Authorization header)*

**Expected:** `200 OK` — array of 8 products returned from product-service.

This is a GET request to `/api/products/**` — the gateway's `isPublicEndpoint()` method allows it through without a token.

---

### Test 3: Protected endpoint — get products by ID through gateway (no token)

**Request:** `GET http://localhost:8080/api/customers`

**Headers:** *(no Authorization header)*

**Expected:** `401 Unauthorized` — gateway rejects before forwarding.

---

### Test 4: Login through gateway and get token

**Request:** `POST http://localhost:8080/api/auth/login`

**Body:**
```json
{
  "username": "alice",
  "password": "password123"
}
```

**Expected:** `200 OK` with token. The test script saves it to `{{token}}`.

---

### Test 5: Access protected endpoint through gateway (with token)

**Request:** `GET http://localhost:8080/api/customers`

**Headers:** `Authorization: Bearer {{token}}`

**Expected:** `200 OK` — customer list returned.

**What happened:**
1. Gateway received the request
2. `JwtAuthFilter` verified the token signature and expiry
3. Extracted `userId=1`, `username="alice"` from token claims
4. Injected `X-User-Id: 1` and `X-Username: alice` headers
5. Forwarded to `http://user-service-instance:8081/api/customers`
6. User Service responded — gateway returned the response to Postman

---

### Test 6: Expired/tampered token

**Request:** `GET http://localhost:8080/api/customers`

**Headers:** `Authorization: Bearer invalidtoken123`

**Expected:** `401 Unauthorized` — `JwtUtil.isTokenValid()` returns false.

---

### Test 7: Search products through gateway

**Request:** `GET http://localhost:8080/api/products/search?q=laptop`

**Expected:** `200 OK` — returns "Laptop Pro 15" — no token needed (GET to `/api/products/**`).

---

## 7.9 End-to-End Request Trace

Here is the complete flow when an authenticated user fetches their cart:

```
Postman
  POST /api/auth/login → gateway → user-service → 200 with JWT
  (token saved: eyJhbGciOiJIUzI1NiJ9...)

Postman
  GET /api/cart (Authorization: Bearer eyJ...)
       │
       ▼
  API Gateway :8080
       │
       ├─ JwtAuthFilter:
       │     path = /api/cart → not public
       │     header = "Bearer eyJ..."
       │     jwtUtil.isTokenValid(token) → true
       │     claims → userId=1, username="alice"
       │     mutate request: add X-User-Id=1, X-Username=alice
       │
       ├─ Route matching: /api/cart/** → lb://order-service
       │
       ├─ Eureka lookup: ORDER-SERVICE → 127.0.0.1:8083
       │
       └─ Forward: GET http://127.0.0.1:8083/api/cart
                      + X-User-Id: 1
                      + X-Username: alice
                      + Authorization: Bearer eyJ... (still present)

  Order Service :8083
       │
       ├─ CartController.getCart(@RequestHeader("X-User-Id") Long userId)
       │       userId = 1
       │
       └─ return CartDto for user 1

  API Gateway
       └─ return CartDto to Postman
```

---

## 7.10 Common Mistakes

### Mistake 1: Adding `spring-boot-starter-web` alongside `spring-cloud-starter-gateway`

```xml
<!-- WRONG — causes ApplicationContext startup failure -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>

<!-- CORRECT — gateway only, no spring-boot-starter-web -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
```

Error you will see: `Spring MVC found on classpath, which is incompatible with Spring Cloud Gateway`

### Mistake 2: Forgetting `web-application-type: reactive`

Without this, Spring Boot may try to start a Servlet context even though the gateway is reactive:
```yaml
spring:
  main:
    web-application-type: reactive   # required
```

### Mistake 3: Route order matters for overlapping paths

```yaml
routes:
  - id: user-service-auth
    uri: lb://user-service
    predicates:
      - Path=/api/auth/**

  - id: user-service-customers
    uri: lb://user-service
    predicates:
      - Path=/api/customers/**
```

If you had a wildcard route `/api/**` first, it would match all paths. More specific routes must come before less specific ones — or use `Order` on routes.

### Mistake 4: JWT secret mismatch between gateway and user-service

```yaml
# api-gateway/application.yml
jwt:
  secret: ecommerce-jwt-secret-key-for-microservices-project-2024-secure

# config-repo/user-service.properties
jwt.secret=ecommerce-jwt-secret-key-for-microservices-project-2024-secure
```

These must be character-for-character identical. A single space difference → `SignatureException` → 401 on every request.

---

## Module Checkpoint

Before moving to Module 8 (Order Service), confirm:

- [ ] API Gateway starts and registers with Eureka (`API-GATEWAY` visible at `http://localhost:8761`)
- [ ] `POST http://localhost:8080/api/auth/register` → 200 (forwarded to user-service)
- [ ] `GET http://localhost:8080/api/products` → 200 with 8 products (no token needed)
- [ ] `GET http://localhost:8080/api/customers` → 401 (no token)
- [ ] `POST http://localhost:8080/api/auth/login` → 200 with token
- [ ] `GET http://localhost:8080/api/customers` with valid token → 200
- [ ] `GET http://localhost:8080/api/customers` with tampered token → 401
- [ ] Gateway DEBUG logs show route matching and upstream forwarding

---

## What's Next — Module 8

With the gateway protecting all endpoints, Module 8 builds the **Order Service** — the most complex service in the system. It introduces:
- Cart management (add items, update quantity, remove items)
- **OpenFeign** — declarative HTTP client for service-to-service calls to Product Service
- Order placement — reads the cart, reduces product stock via Feign, creates an Order record
- End-to-end Postman validation: login → add to cart → place order → check order history
