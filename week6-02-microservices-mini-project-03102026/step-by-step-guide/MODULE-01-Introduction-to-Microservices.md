# Module 1 — Introduction to Microservices & Spring Cloud

**Type:** Theory / Lecture
**Duration:** ~3 hours (including discussions)
**Prerequisites:** Basic Java, Spring Boot fundamentals, REST APIs
**Goal:** Understand *why* microservices exist, *what* the core patterns are, and *how* Spring Cloud maps to those patterns in our project

---

## Learning Objectives

By the end of this module, participants will be able to:

1. Explain what microservices architecture is and why it emerged
2. Compare monolithic and microservices approaches with real trade-offs
3. Describe the six core patterns used in this project
4. Identify each Spring Cloud component and what problem it solves
5. Map the architecture diagram of our e-commerce project to real Spring Cloud modules

---

## Part 1 — The World Before Microservices

### 1.1 What Is a Monolithic Application?

Before microservices became mainstream, almost every enterprise application was built as a **monolith** — a single, unified codebase packaged and deployed as one unit.

Think of an e-commerce monolith like this:

```
┌─────────────────────────────────────────────────────┐
│              ecommerce-app.war / .jar               │
│                                                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────┐  │
│  │  Users   │  │ Products │  │     Orders        │  │
│  │  Module  │  │  Module  │  │     Module        │  │
│  └──────────┘  └──────────┘  └──────────────────┘  │
│                                                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────┐  │
│  │Payments  │  │ Shipping │  │   Notifications   │  │
│  │  Module  │  │  Module  │  │      Module       │  │
│  └──────────┘  └──────────┘  └──────────────────┘  │
│                                                     │
│         Single Database (shared schema)             │
└─────────────────────────────────────────────────────┘
         Deployed as ONE artifact → ONE server
```

All modules live in the same codebase, share the same database, and are deployed together as a single artifact.

---

### 1.2 How Monoliths Work — and Why They Worked

In the early stages of a product, monoliths are genuinely the right choice:

- **Simple to develop:** One project, one IDE, one run configuration
- **Simple to test:** Integration tests cover the whole system in one shot
- **Simple to deploy:** One artifact to ship to one server
- **Simple to debug:** One log file, one stack trace, one process

For a small team building a startup product, a monolith is often the fastest path to a working system.

---

### 1.3 The Monolith Pain Points at Scale

As the system and organisation grow, the monolith begins to fight back.

#### Problem 1 — Deployment Coupling

Every change — no matter how small — requires the entire application to be redeployed.

> A one-line fix to the product description field forces a full regression test of Users, Orders, Payments, Shipping, and Notifications before the fix can go live.

Teams that deploy multiple times per day on a monolith quickly find that every deployment is a shared risk across every team.

#### Problem 2 — Scaling Inefficiency

Suppose the Product Search feature is under heavy load during a sale event. In a monolith, you can only scale **the entire application** — you cannot scale just the search feature.

```
                   Sale event: product search is the bottleneck

Monolith scaling:                  What you want:
┌──────────────┐                   ┌──────────┐  ┌──────────┐  ┌──────────────────────┐
│  Full App x3 │ ← wasteful        │  Users   │  │ Products │  │ Products x5 (scaled) │
├──────────────┤                   └──────────┘  └──────────┘  └──────────────────────┘
│  Full App x3 │
└──────────────┘
```

You pay for three full copies of your Payments module just to scale your search page.

#### Problem 3 — Technology Lock-in

Once a monolith is built on a particular stack (say, Java 8 + MySQL), it is nearly impossible to adopt new technology for specific parts of the system. You cannot write the recommendation engine in Python if the rest of the codebase is Java — everything must use the same runtime.

#### Problem 4 — Team Scaling Conflicts

As the engineering team grows, multiple teams work on the same codebase. This creates:

- **Merge conflicts** — every team touches shared files
- **Coordination overhead** — one team's API change breaks another's tests
- **Slow build times** — the entire codebase compiles and tests for every change
- **Fear of touching shared code** — shared utilities become untouchable legacy

#### Problem 5 — Fault Propagation

A memory leak in the Notifications module can crash the entire application, taking down Users, Products, and Orders with it. There is no isolation boundary.

---

### 1.4 Discussion Point

> **Question for the class:**
> Imagine you work at an e-commerce company. The Black Friday sale starts in 2 hours. Your product search is returning 503 errors. Your monolith is deployed on 10 servers. What are your options?
>
> Now imagine that Product Search is its own independent service. What changes?

---

## Part 2 — Microservices Architecture

### 2.1 Definition

**Microservices** is an architectural style where an application is composed of **small, independently deployable services**, each running its own process and communicating over a well-defined API.

The seminal definition from Martin Fowler and James Lewis (2014):

Project: E-Commerce Microservices (Spring Boot + AngularJS)

A full-stack e-commerce app built with the microservices pattern. The backend is Java 17 / Spring Boot 3.2.3 with Spring Cloud; the frontend is a single-page AngularJS 1.8.3 app.

Infrastructure Services

Service	Port	Role
Config Server	8888	Centralized config via local git (config-repo/)
Eureka Server	8761	Service discovery registry
API Gateway	8080	Single entry point — JWT auth + routing
Business Services

Service	Port	DB	Responsibility
user-service	8081	user_db	Register, login, JWT generation
product-service	8082	product_db	Product CRUD + sample data seeder
order-service	8083	order_db	Cart management + order placement
Key Patterns & Decisions

Auth at the gateway: JwtAuthFilter validates JWT and injects X-User-Id / X-Username headers downstream. Individual services trust these headers (permit all).
Public endpoints: POST /api/users/register, POST /api/users/login, GET /api/products/** — no token required.
Inter-service communication: Order service calls product-service via OpenFeign (ProductServiceClient) to fetch product details when placing orders.
Config: Each service pulls its config (datasource, jwt.secret, etc.) from the Config Server at startup.
JWT: HS256, secret shared across gateway and user-service via config.
Each business service follows the same internal layout:


controller/ → REST endpoints
service/    → business logic
repository/ → Spring Data JPA
entity/     → JPA entities
dto/        → request/response objects
config/     → SecurityConfig (permit all — trust gateway)
Frontend (frontend/) is a standalone AngularJS SPA with views for: login, register, products, cart, and orders. Talks directly to the API Gateway on port 8080.

Database: MySQL, one schema per service (user_db, product_db, order_db). Credentials managed in config-repo/*.properties.> *"The microservice architectural style is an approach to developing a single application as a suite of small services, each running in its own process and communicating with lightweight mechanisms, often an HTTP resource API."*

### 2.2 The Core Idea — Decomposition by Business Capability

The fundamental principle is to align service boundaries with **business capabilities**, not technical layers.

A **wrong** decomposition (by technical layer):
```
Frontend Service → API Service → Database Service
```
This is still a distributed monolith — all services must deploy together for any change.

A **correct** decomposition (by business capability):
```
User Service      → owns everything about customers
Product Service   → owns everything about the catalogue
Order Service     → owns everything about purchases
Payment Service   → owns everything about money
```

Each service owns its own data, its own logic, and its own deployment lifecycle.

### 2.3 What Makes a Service "Micro"?

The "micro" in microservices does not strictly mean small in lines of code. It means:

| Characteristic | What It Means |
|---|---|
| **Single Business Responsibility** | Does one thing well; aligned with one bounded context |
| **Independently Deployable** | Can be shipped to production without touching other services |
| **Owns Its Own Data** | Has its own database; no sharing data stores with other services |
| **Communicates via API** | Only interface to the outside world is its HTTP (or messaging) API |
| **Decentralised Governance** | Teams can make independent technology decisions |
| **Designed for Failure** | Assumes other services will fail and handles it gracefully |

### 2.4 The Microservices Trade-off

Microservices are not universally better than monoliths. They solve specific problems at the cost of introducing new complexity.

| Aspect | Monolith | Microservices |
|---|---|---|
| **Initial setup** | Simple | Complex (infra, CI/CD, orchestration) |
| **Development speed (early)** | Fast | Slower — distributed system complexity |
| **Development speed (at scale)** | Slows dramatically | Teams move independently |
| **Deployment** | Simple, risky | Complex, but safer (isolated deploys) |
| **Scaling** | Scale everything | Scale individual services |
| **Testing** | Easier integration testing | Harder — requires mocking/contracts |
| **Debugging** | One log, one process | Distributed tracing required |
| **Data consistency** | Easy — same DB transaction | Hard — eventual consistency, sagas |
| **Team autonomy** | Low | High |
| **Operational overhead** | Low | High — multiple services to monitor |

**Key lesson:** Start with a monolith. Move to microservices when organisational and scaling pain justifies the operational overhead.

---

## Part 3 — Core Microservices Patterns

These are the six patterns that our e-commerce project implements. Every pattern solves a specific distributed systems problem.

---

### Pattern 1 — Database per Service

**Problem:** If all services share one database, a schema change by the Orders team breaks the Users team's queries. Services are coupled through the database even if their code is separate.

**Solution:** Each service owns its own database. No other service may access it directly — only through the owning service's API.

```
User Service  ──→  user_db      (MySQL)
Product Service ──→ product_db  (MySQL)
Order Service  ──→  order_db    (MySQL)
```

**Consequences:**
- Services are truly independent — schema changes don't propagate
- Each service can choose a different database technology (e.g. Redis for caching, MongoDB for documents)
- **Cross-service queries require API calls**, not SQL joins — this is a significant design shift
- Data that belongs to multiple services (e.g. product name in an order record) must be **denormalised** — copied at the time of the transaction

**In our project:** Order Service copies `product_name` and `price` into `order_items` when an order is placed, rather than joining with `product_db`. This means historical order records remain accurate even if the product's price changes later.

---

### Pattern 2 — API Gateway

**Problem:** The frontend knows the address of 5 different services. Every time a service moves, or a new one is added, the frontend must be updated. Authentication must be implemented in every service. CORS must be configured everywhere.

**Solution:** An **API Gateway** is the single entry point for all client traffic. It handles cross-cutting concerns centrally.

```
Browser / Mobile App
         │
         ▼
    API Gateway  (single public address)
    ┌────────────────────────────────────┐
    │  • Authentication (JWT validation) │
    │  • Routing                         │
    │  • CORS                            │
    │  • Rate limiting                   │
    │  • Request logging                 │
    └───────┬──────────┬─────────────────┘
            │          │
        User Svc   Product Svc  Order Svc
```

**Consequences:**
- Clients only need to know one address — the gateway
- Auth logic lives in one place
- Services can move (change ports/IPs) without clients knowing
- The gateway is a potential single point of failure → must be made highly available

**In our project:** Spring Cloud Gateway runs on port 8080. It validates JWT tokens using `JwtAuthFilter` before routing requests to user-service (8081), product-service (8082), or order-service (8083).

---

### Pattern 3 — Service Discovery

**Problem:** In a distributed system, services start and stop dynamically. How does Service A know the address of Service B? Hard-coding `http://192.168.1.45:8082` breaks the moment Service B moves or scales to multiple instances.

**Solution:** A **Service Registry** acts as a phone book. Every service registers itself when it starts. Callers look up the registry to find where to send requests.

```
                    ┌──────────────────────┐
                    │   Eureka Registry    │
                    │                      │
                    │  user-service:8081   │
                    │  product-service:8082│
                    │  order-service:8083  │
                    └──────────────────────┘
                         ↑           ↑
           Register self │           │ Look up "product-service"
                         │           │
                   Order Service   API Gateway
```

Two styles of service discovery:

| Style | How It Works | Example |
|---|---|---|
| **Client-side** | Caller queries registry, picks an instance, calls it directly | Netflix Eureka (our project) |
| **Server-side** | Caller asks a load balancer; LB queries registry internally | AWS ALB, Kubernetes |

**In our project:** Netflix Eureka is the registry (port 8761). Every service registers on startup. The API Gateway and Order Service (via Feign) use Eureka to find service instances. The `lb://user-service` URI in the gateway config means "look up user-service in Eureka and load balance."

---

### Pattern 4 — Centralised Configuration

**Problem:** You have 6 services, each with its own `application.yml`. Database credentials, JWT secrets, and environment-specific URLs are scattered across every codebase. Changing the DB password means updating and redeploying all 6 services.

**Solution:** A **Config Server** externalises all configuration into a central store (typically a Git repository). Services fetch their config at startup.

```
Git Repository (config-repo/)
├── user-service.properties      ← datasource, jwt.secret
├── product-service.properties   ← datasource
└── order-service.properties     ← datasource

         ↓ served by

Config Server (port 8888)
  GET /user-service/default → user-service.properties content

         ↓ fetched by

User Service (at startup)
  merges fetched config with its local application.yml
```

**Benefits:**
- One place to change DB credentials — all services pick it up on restart
- Different profiles (`dev`, `staging`, `prod`) serve different configs without code changes
- Config history is tracked in Git — full audit trail

**In our project:** Spring Cloud Config Server reads from `config-repo/` (a local Git repo). Business services declare `spring.config.import: "optional:configserver:http://localhost:8888"` to fetch their datasource config.

---

### Pattern 5 — Synchronous Inter-Service Communication

**Problem:** Services need to call each other. Order Service needs product details from Product Service. How do we make this clean, maintainable, and integrated with service discovery?

**Solution:** Use a **declarative HTTP client** (OpenFeign) that:
- Defines the call as a Java interface — no URL strings, no boilerplate
- Integrates with Eureka — uses service name instead of hard-coded IP
- Integrates with load balancing — automatically distributes across instances

```java
// No URL, no RestTemplate, no boilerplate
@FeignClient(name = "product-service")
public interface ProductServiceClient {
    @GetMapping("/api/products/{id}")
    ProductDto getProductById(@PathVariable Long id);
}
```

Compare with manual RestTemplate (the old way):
```java
// Hard-coded URL, error-prone, no load balancing
String url = "http://localhost:8082/api/products/" + id;
ProductDto product = restTemplate.getForObject(url, ProductDto.class);
```

**Tradeoff — Synchronous vs Asynchronous:**

| Synchronous (our project) | Asynchronous (message broker) |
|---|---|
| Simple, predictable | Complex setup (Kafka/RabbitMQ) |
| Caller waits for response | Caller fires and forgets |
| If Product Service is down, Order Service fails | Services are fully decoupled |
| Good for: fetching data needed immediately | Good for: events (order placed, email sent) |

For this training project, synchronous Feign calls are used. In production, high-throughput operations (like sending confirmation emails) would use async messaging.

**In our project:** Order Service calls Product Service via Feign to fetch product details when adding to cart, and to reduce stock when placing an order.

---

### Pattern 6 — Stateless Authentication with JWT

**Problem:** Traditional session-based authentication stores session data on the server. With multiple service instances, the user might get routed to an instance that doesn't have their session.

**Solution:** Use **JSON Web Tokens (JWT)** — self-contained tokens that carry authentication state. The server validates the token's signature; no session storage is needed.

```
Login Request:
  Client → POST /api/users/login → Server validates credentials
  Server → Generates JWT, signs it with secret key
  Client ← Receives JWT, stores in localStorage

Protected Request:
  Client → GET /api/cart (Authorization: Bearer eyJhbG...)
  Gateway → Validates JWT signature + expiry (no DB lookup needed)
  Gateway → Extracts userId, forwards request with X-User-Id header
  Order Service → Reads X-User-Id header, processes request
```

**JWT Structure:**

```
Header.Payload.Signature

Header:  { "alg": "HS256", "typ": "JWT" }
Payload: { "sub": "jane@example.com", "userId": 1, "exp": 1709380800 }
Signature: HMACSHA256(base64(header) + "." + base64(payload), secret)
```

**Why stateless matters for microservices:**
- Any instance of any service can validate a JWT — no shared session store needed
- The gateway validates the token once; downstream services trust the injected headers
- Tokens scale horizontally with zero additional infrastructure

**In our project:** User Service generates JWT on login using `JwtUtil`. The API Gateway validates JWT using the same `JwtUtil` (shared secret). Downstream services read `X-User-Id` from the header — they never touch the JWT directly.

---

## Part 4 — Introduction to Spring Cloud

### 4.1 What Is Spring Cloud?

**Spring Boot** gives you an opinionated, auto-configured way to build individual Spring applications.

**Spring Cloud** builds on top of Spring Boot and gives you the infrastructure patterns needed to build *distributed systems* — connecting multiple Spring Boot applications together.

```
Spring Cloud
    └── Built on Spring Boot
            └── Built on Spring Framework
```

Spring Cloud is not a single library — it is a **family of projects**, each solving a specific distributed systems problem:

| Spring Cloud Project | Solves |
|---|---|
| Spring Cloud Config | Centralised configuration from Git |
| Spring Cloud Netflix (Eureka) | Service discovery and registration |
| Spring Cloud Gateway | API Gateway with routing and filtering |
| Spring Cloud OpenFeign | Declarative HTTP clients |
| Spring Cloud LoadBalancer | Client-side load balancing |
| Spring Cloud Sleuth / Micrometer | Distributed tracing |
| Spring Cloud Stream | Event-driven messaging (Kafka/RabbitMQ) |
| Spring Cloud Circuit Breaker | Resilience (Resilience4j / Hystrix) |

### 4.2 Spring Cloud Release Train

Spring Cloud versions are coordinated releases called **release trains**, named to stay in sync with specific Spring Boot versions:

| Spring Cloud Version | Spring Boot Version | Codename |
|---|---|---|
| 2023.0.x (Leyton) | 3.2.x | Latest |
| 2022.0.x (Kilburn) | 3.0.x / 3.1.x | |
| 2021.0.x (Jubilee) | 2.6.x / 2.7.x | |
| Hoxton | 2.2.x / 2.3.x | Legacy |

> **Our project uses:** Spring Boot 3.2.3 + Spring Cloud 2023.0.0 (Leyton)

The version is managed via the **Spring Cloud BOM** (Bill of Materials) in the parent POM — this ensures all Spring Cloud modules are version-compatible with each other.

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2023.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Once this BOM is declared in the parent, child modules can declare Spring Cloud dependencies **without specifying versions** — Maven pulls the correct version automatically.

---

### 4.3 Spring Cloud Config

**Maven dependency:** `spring-cloud-config-server` (server) / `spring-cloud-starter-config` (client)

**What it provides:**
- A REST-based configuration server that serves properties from a Git repository
- Clients fetch config at startup time using a standard HTTP call
- Supports multiple profiles (`dev`, `staging`, `prod`)
- Config changes can be pushed to running services without restart (via Spring Cloud Bus + `/actuator/refresh`)

**Key concepts:**

```yaml
# On the server (config-server/application.yml)
spring:
  cloud:
    config:
      server:
        git:
          uri: file://${user.home}/microservices-mini-project/config-repo
          default-label: main   # git branch

# On each client (e.g. user-service/application.yml)
spring:
  application:
    name: user-service          # must match filename in config-repo
  config:
    import: "optional:configserver:http://localhost:8888"
```

**How the client fetches config:**
When a client starts, it calls:
```
GET http://localhost:8888/user-service/default
```
The Config Server reads `config-repo/user-service.properties`, returns the properties as JSON, and the client merges them into its `Environment`.

---

### 4.4 Spring Cloud Netflix Eureka

**Maven dependency:** `spring-cloud-starter-netflix-eureka-server` (server) / `spring-cloud-starter-netflix-eureka-client` (client)

**What it provides:**
- A REST-based service registry where services register themselves
- A web dashboard to view registered services and their health
- Heartbeat mechanism — services send a heartbeat every 30 seconds; removed after 90 seconds of silence
- Client-side load balancing when combined with Spring Cloud LoadBalancer

**Key concepts:**

```yaml
# Eureka Server — don't register with yourself
eureka:
  client:
    register-with-eureka: false
    fetch-registry: false

# Eureka Client (in every other service)
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
    instance-id: ${spring.application.name}:${server.port}
```

**Dashboard:** `http://localhost:8761` shows all registered instances in real time.

---

### 4.5 Spring Cloud Gateway

**Maven dependency:** `spring-cloud-starter-gateway`

**What it provides:**
- A reactive (non-blocking) API gateway built on Spring WebFlux
- Route matching based on path, headers, methods, query parameters
- `GlobalFilter` — custom code that runs on every request (e.g. JWT validation, logging)
- `GatewayFilter` — custom code on specific routes (e.g. rate limiting, circuit breaking)
- Integration with Eureka — routes can use `lb://service-name` for load-balanced routing
- Built-in CORS support

**Key concepts:**

```yaml
# Route definition
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service    # lb: = load balanced via Eureka
          predicates:
            - Path=/api/users/**    # match incoming path
```

**Important:** Gateway uses **Spring WebFlux** (reactive), not Spring MVC. This means:
- Security uses `ServerHttpSecurity`, not `HttpSecurity`
- Filters implement `GlobalFilter`, not `OncePerRequestFilter`
- The web application type must be explicitly set to `reactive`

---

### 4.6 Spring Cloud OpenFeign

**Maven dependency:** `spring-cloud-starter-openfeign`

**What it provides:**
- Declarative HTTP client — define an interface, Feign generates the implementation
- Integrates with Eureka — uses service names, not hard-coded URLs
- Integrates with Spring Cloud LoadBalancer — automatic load balancing
- Supports request/response logging, retry, and circuit breaker integration

**Key concepts:**

```java
// Step 1 — Enable Feign on the application
@SpringBootApplication
@EnableFeignClients
public class OrderServiceApplication { ... }

// Step 2 — Define the client interface
@FeignClient(name = "product-service")   // service name as registered in Eureka
public interface ProductServiceClient {

    @GetMapping("/api/products/{id}")
    ProductDto getProductById(@PathVariable("id") Long id);
}

// Step 3 — Inject and use it like any Spring bean
@Service
public class CartService {
    @Autowired
    private ProductServiceClient productClient;

    public void addToCart(Long productId) {
        ProductDto product = productClient.getProductById(productId);
        // use product.getName(), product.getPrice(), etc.
    }
}
```

---

### 4.7 Spring Cloud LoadBalancer

**Maven dependency:** included transitively with `spring-cloud-starter-netflix-eureka-client`

**What it provides:**
- Client-side load balancing — the caller (not a proxy) selects which instance to call
- Replaces the deprecated Netflix Ribbon
- Works with Feign and Gateway transparently
- Default strategy: round-robin

**How it works with `lb://`:**
```
lb://product-service
    ↓
LoadBalancer queries Eureka: "give me all healthy instances of product-service"
    ↓
Eureka returns: [192.168.1.10:8082, 192.168.1.11:8082]
    ↓
LoadBalancer picks one (round-robin): 192.168.1.10:8082
    ↓
Request goes to http://192.168.1.10:8082/api/products/1
```

---

## Part 5 — Our Project Architecture Revisited

Now that we understand each pattern and Spring Cloud module, let's re-read the architecture diagram with informed eyes.

```
AngularJS SPA
    │
    │  All requests to http://localhost:8080
    ▼
┌─────────────────────────────────────────────────────┐
│         API Gateway  :8080                          │
│                                                     │
│  Spring Cloud Gateway (WebFlux/Reactive)            │
│  ┌─────────────────────────────────┐                │
│  │ JwtAuthFilter (GlobalFilter)    │ ← Pattern 6   │
│  │ - Validates JWT signature       │   Stateless    │
│  │ - Extracts userId               │   Auth         │
│  │ - Injects X-User-Id header      │                │
│  └─────────────────────────────────┘                │
│  Routes: lb://user-service          ← Pattern 3    │
│          lb://product-service         Service       │
│          lb://order-service           Discovery     │
└──────────┬──────────┬───────────────┬───────────────┘
           │          │               │
           ▼          ▼               ▼
      User Svc   Product Svc     Order Svc
      :8081       :8082           :8083
      user_db   product_db      order_db     ← Pattern 1
                                  │            DB per Service
                    OpenFeign ────┘          ← Pattern 5
                    (get product,              Sync Comm.
                     reduce stock)

┌──────────────────────────────────────────────────────┐
│           Infrastructure Layer                        │
│                                                       │
│   Eureka Server :8761   ← Pattern 3 (Registry)       │
│   Config Server :8888   ← Pattern 4 (Config Mgmt)    │
└──────────────────────────────────────────────────────┘
```

### Pattern → Spring Cloud Module Mapping

| Pattern | Spring Cloud Module | In Our Project |
|---|---|---|
| Centralised Config | Spring Cloud Config Server | `config-server` module |
| Service Discovery | Spring Cloud Netflix Eureka | `eureka-server` module |
| API Gateway | Spring Cloud Gateway | `api-gateway` module |
| Declarative HTTP Client | Spring Cloud OpenFeign | `order-service` → `product-service` |
| Client-side Load Balancing | Spring Cloud LoadBalancer | Auto via Eureka + `lb://` |
| Stateless Auth | Spring Security + JJWT | `user-service` (generate) + `api-gateway` (validate) |

---

## Part 6 — Key Vocabulary Reference

| Term | Definition |
|---|---|
| **Microservice** | A small, independently deployable service aligned with one business capability |
| **Monolith** | A single deployable artifact containing all application modules |
| **Bounded Context** | A domain concept with a clearly defined boundary — the natural size of a microservice |
| **Service Registry** | A database of running service instances (Eureka) |
| **API Gateway** | Single entry point for all client traffic; handles cross-cutting concerns |
| **Service Discovery** | Mechanism for services to find each other's addresses dynamically |
| **Feign Client** | Declarative HTTP client that generates implementation from an interface |
| **Load Balancer** | Distributes requests across multiple instances of the same service |
| **JWT** | JSON Web Token — a self-contained, signed token for stateless authentication |
| **Config Server** | A service that serves externalised configuration from a Git repo |
| **BOM** | Bill of Materials — a POM that manages version alignment for a set of libraries |
| **`lb://`** | URI scheme in Spring Cloud Gateway/Feign meaning "resolve via load balancer" |
| **GlobalFilter** | A filter in Spring Cloud Gateway that applies to every route |
| **WebFlux** | Spring's reactive (non-blocking) web framework — used by the API Gateway |
| **Heartbeat** | Periodic signal a service sends to Eureka to confirm it is alive |
| **Denormalisation** | Copying data (e.g. product name into order record) to avoid cross-service joins |

---

## Module 1 — Summary

| Concept | Key Takeaway |
|---|---|
| Why microservices? | To solve deployment coupling, scaling inefficiency, and team autonomy problems at scale |
| Core trade-off | More operational complexity in exchange for independent deployability and scalability |
| Database per Service | No shared databases — cross-service data access only via APIs |
| API Gateway | Single entry point; handles auth, routing, and cross-cutting concerns |
| Service Discovery | Services register with Eureka; callers look up addresses dynamically |
| Centralised Config | All environment-specific config lives in a Git repo, served by Config Server |
| OpenFeign | Declarative HTTP clients — clean, readable, Eureka-integrated |
| JWT Auth | Stateless tokens carry identity; gateway validates once; services trust headers |
| Spring Cloud | A family of projects that implements each of these patterns on top of Spring Boot |

---

## What's Next — Module 2

In the next module, we will:
1. Set up the Maven multi-module project structure from scratch
2. Write the parent `pom.xml` with Spring Cloud BOM
3. Create the `config-repo/` Git repository
4. Add the properties files for all three business services
5. Verify the project structure compiles cleanly

**No running services yet** — just the skeleton that everything will be built on.

---

*End of Module 1*
