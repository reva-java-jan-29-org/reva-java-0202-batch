# Concept Check — Interview Questions
### Week 6 Revision · 09 Mar 2026

> 10 questions per topic, focused on **understanding and reasoning** — not just definitions.
> These questions test *why* and *how*, not just *what*.

---

## TABLE OF CONTENTS

1. [Spring Core](#1-spring-core)
2. [Spring Web (MVC)](#2-spring-web-mvc)
3. [REST API Development](#3-rest-api-development)
4. [Spring Boot](#4-spring-boot)
5. [JPA (Core Concepts)](#5-jpa-core-concepts)
6. [Spring Security](#6-spring-security)
7. [Spring Data JPA](#7-spring-data-jpa)
8. [Microservices — Config Server, API Gateway, Eureka](#8-microservices--config-server-api-gateway-eureka)
9. [Docker](#9-docker)

---

## 1. Spring Core

---

**Q1. You have two beans of the same type in the container. Your field uses `@Autowired`. What happens, and how do you fix it?**

Spring throws a `NoUniqueBeanDefinitionException` at startup because it cannot determine which bean to inject.

Fix it with one of:
- `@Qualifier("beanName")` on the injection point to name the specific bean you want.
- `@Primary` on one bean to make it the default choice when no qualifier is specified.
- Use the field name matching the bean name — Spring falls back to name matching if type is ambiguous.

---

**Q2. What is the difference between `@Component`, `@Service`, `@Repository`, and `@Controller`? Are they interchangeable?**

All four are specialisations of `@Component` — they are all scanned and registered as beans. The differences are **semantic** and **functional**:

| Annotation | Semantic role | Extra behaviour |
|---|---|---|
| `@Component` | Generic bean | None |
| `@Service` | Business logic | None (pure marker) |
| `@Repository` | Data access layer | Translates `SQLException` → Spring `DataAccessException` |
| `@Controller` | MVC controller | Handled by `DispatcherServlet` for view resolution |

Technically `@Service` and `@Component` are interchangeable, but you lose semantics. `@Repository` and `@Controller` have real runtime behaviour — don't swap them arbitrarily.

---

**Q3. Explain the Spring bean lifecycle from instantiation to destruction.**

1. **Instantiate** — container creates the object (calls constructor)
2. **Populate properties** — injects dependencies (`@Autowired`, setters, constructor args)
3. **Aware callbacks** — `BeanNameAware`, `ApplicationContextAware` etc. are called
4. **`@PostConstruct`** — custom init logic runs after all dependencies are injected
5. **Bean is ready** — used by the application
6. **`@PreDestroy`** — cleanup logic runs before the bean is removed from the container
7. **Destroy** — bean is garbage collected

`@PostConstruct` / `@PreDestroy` are the practical hooks you write in day-to-day code.

---

**Q4. What is the difference between `@Bean` and `@Component`?**

| | `@Component` | `@Bean` |
|---|---|---|
| Where | On the class | On a method inside a `@Configuration` class |
| Who controls instantiation | Spring (via classpath scan) | You write the `new` call inside the method |
| Use when | You own the class source | Third-party class or need custom construction logic |

Example: you cannot put `@Component` on `ObjectMapper` from Jackson — you don't own its source. So you write a `@Bean` method that creates and configures it.

---

**Q5. What does `@Configuration` do? How is it different from just using `@Component` on a class with `@Bean` methods?**

`@Configuration` tells Spring to proxy the class using CGLIB. This means when one `@Bean` method calls another inside the same class, it goes through the proxy and returns the **same singleton bean** instead of creating a new object.

With `@Component`, the class is not proxied — calling another `@Bean` method directly creates a new instance each time, breaking singleton semantics.

---

**Q6. What is the default bean scope in Spring? When would you use `prototype` scope?**

Default scope is **singleton** — one instance per Spring container, shared by everyone who injects it.

Use `prototype` when the bean holds **state** that should not be shared:
- A bean that builds a request-specific object
- A bean used in multithreaded code where each thread needs its own instance

Singleton beans with a prototype dependency need `@Lookup` or `ApplicationContext.getBean()` — Spring will not re-inject the prototype on every method call otherwise.

---

**Q7. What problem does Dependency Injection solve that the `new` keyword does not?**

Using `new` creates **tight coupling** — `OrderService` directly depends on a concrete `EmailService`. To test `OrderService` in isolation, you can't swap in a fake `EmailService`.

With DI, `OrderService` depends on the `NotificationService` **interface**. Spring injects the real `EmailService` at runtime. In tests, you inject a mock. The classes are loosely coupled and independently testable.

---

**Q8. What is `ApplicationContext`? How is it different from `BeanFactory`?**

`BeanFactory` is the base container — it lazily creates beans on first request.

`ApplicationContext` extends `BeanFactory` and adds:
- **Eager initialisation** of singleton beans at startup
- **Event publishing** (`ApplicationEvent`)
- **i18n** (message sources)
- **AOP integration**

In practice, always use `ApplicationContext` (`AnnotationConfigApplicationContext`, `SpringApplication` etc.) — `BeanFactory` is rarely used directly.

---

**Q9. What is AOP? Give a real use case where you would apply it in a Spring project.**

AOP (Aspect-Oriented Programming) lets you separate **cross-cutting concerns** — logic that applies to many classes — from business logic.

Real use cases:
- **Logging** — log every service method call and its duration without polluting every class
- **Transaction management** — `@Transactional` is itself implemented as an AOP aspect
- **Security checks** — `@PreAuthorize` intercepts method calls via AOP
- **Auditing** — record who called what and when

Key terms: **Aspect** (the cross-cutting class), **Advice** (`@Before`, `@After`, `@Around`), **Pointcut** (which methods to intercept), **Join point** (the actual method execution).

---

**Q10. What happens if you inject a `prototype`-scoped bean into a `singleton`-scoped bean using `@Autowired`?**

The prototype bean is injected **once** at the time the singleton is created. After that, the same prototype instance is reused for every call — defeating the purpose of prototype scope.

To get a new prototype instance on every use, either:
- Use `ApplicationContext.getBean(MyPrototype.class)` inside the singleton
- Annotate the getter method with `@Lookup` — Spring overrides it to call `getBean` each time

---

## 2. Spring Web (MVC)

---

**Q1. What is `DispatcherServlet` and what role does it play in Spring MVC?**

`DispatcherServlet` is the **front controller** — the single entry point for all HTTP requests in a Spring MVC application. Every request hits it first.

It delegates to:
- `HandlerMapping` — finds which controller method handles this URL
- `HandlerAdapter` — invokes the controller method
- `ViewResolver` — (for non-REST responses) resolves the logical view name to a template

In a REST API (`@RestController`), the `ViewResolver` step is skipped — the return value is written directly to the response body via `HttpMessageConverter`.

---

**Q2. What is the difference between `@GetMapping("/products/{id}")` and `@GetMapping("/products")`? When do you use each?**

`/products/{id}` — identifies a **specific resource** by its unique identifier. Used for single-resource operations: GET, PUT, DELETE one product.

`/products` — refers to the **collection**. Used for: list all (GET), create new (POST).

The `{id}` segment is a **path variable** extracted with `@PathVariable`. The collection endpoint often accepts filter/sort parameters as `@RequestParam`.

---

**Q3. What is `@RequestMapping`? How do `@GetMapping`, `@PostMapping` etc. relate to it?**

`@RequestMapping` is the generic mapping annotation — it accepts a `method` attribute to specify the HTTP method.

`@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping` are **composed annotations** — shorthand for `@RequestMapping(method = RequestMethod.GET)` etc.

You typically see `@RequestMapping` at class level (to set the base path) and method-level shortcuts on handler methods.

---

**Q4. What does `@ResponseBody` do? Why do you not need it when using `@RestController`?**

`@ResponseBody` tells Spring to write the return value of the method **directly to the HTTP response body** using a `HttpMessageConverter` (Jackson for JSON), instead of treating the return value as a view name.

`@RestController` is a composed annotation: `@Controller` + `@ResponseBody`. The `@ResponseBody` is applied to every method in the class automatically — you don't need to repeat it.

---

**Q5. What is `HttpMessageConverter`? How does Spring decide which one to use?**

`HttpMessageConverter` is responsible for converting Java objects to/from HTTP request/response bodies.

Spring picks the converter based on:
1. The **`Content-Type`** header of the request (for deserialization — `@RequestBody`)
2. The **`Accept`** header from the client (for serialization — response)
3. The return type of the method

`Jackson2HttpMessageConverter` handles `application/json`. `StringHttpMessageConverter` handles `text/plain`. You can register custom converters via `WebMvcConfigurer.configureMessageConverters()`.

---

**Q6. What is the role of `@Valid` and where must you place it?**

`@Valid` triggers Bean Validation (JSR-380) on the annotated method parameter.

Place it on the controller method parameter before `@RequestBody`:
```java
public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest req) { ... }
```

If validation fails, Spring automatically returns a `400 Bad Request` and throws `MethodArgumentNotValidException`, which you can handle in `@RestControllerAdvice`.

Without `@Valid`, the validation annotations on the DTO class (`@NotBlank`, `@Min` etc.) are silently ignored.

---

**Q7. What is content negotiation in Spring MVC?**

Content negotiation is the process of Spring deciding **in what format** to return the response, based on what the client says it can accept (via the `Accept` header).

Example:
- `Accept: application/json` → Jackson serialises to JSON
- `Accept: application/xml` → JAXB serialises to XML (if `jackson-dataformat-xml` is on classpath)

If Spring cannot produce the requested format, it returns `406 Not Acceptable`.

---

**Q8. What is the difference between `@ModelAttribute` and `@RequestBody`?**

| | `@ModelAttribute` | `@RequestBody` |
|---|---|---|
| Data source | HTML form fields (URL-encoded form) | HTTP request body (JSON, XML) |
| Use case | Traditional MVC form submission | REST API JSON input |
| Binding | Binds individual form fields to object | Deserialises entire body at once |

`@ModelAttribute` is used with Thymeleaf/JSP form submissions. `@RequestBody` is used in REST APIs where the client sends JSON.

---

**Q9. What happens when a controller method throws an exception that has no handler?**

Spring will look for:
1. An `@ExceptionHandler` in the same controller for that exception type
2. An `@ExceptionHandler` in a `@ControllerAdvice` / `@RestControllerAdvice` class
3. If none found, it delegates to `DefaultHandlerExceptionResolver` for known Spring exceptions
4. Finally falls back to the container's default error response (usually a plain 500 page)

Best practice: always have a `@RestControllerAdvice` with a catch-all handler for `Exception.class` returning a standardised JSON error body.

---

**Q10. How does Spring MVC handle path variable type conversion? What happens if the conversion fails?**

Spring uses `ConversionService` to automatically convert path variable strings to the declared Java type. `@PathVariable Long id` — Spring converts `"42"` → `Long` for you.

If the string cannot be converted (e.g., `@PathVariable Long id` for URL `/products/abc`), Spring throws `MethodArgumentTypeMismatchException`, which results in a `400 Bad Request`. You can handle this in `@RestControllerAdvice`.

---

## 3. REST API Development

---

**Q1. What is the difference between authentication and authorisation? Give an example of each.**

**Authentication** — verifying *who you are*. Example: logging in with username and password, or presenting a JWT token.

**Authorisation** — verifying *what you are allowed to do*. Example: a logged-in user with the `USER` role trying to access an admin-only endpoint gets `403 Forbidden`.

In Spring Security: authentication is handled by `AuthenticationManager`; authorisation is handled by `AccessDecisionManager` or `@PreAuthorize`.

---

**Q2. Why should a REST API be stateless? What breaks if it isn't?**

Stateless means the server holds **no session state** between requests — every request carries all the information needed (e.g., a JWT token in the `Authorization` header).

Benefits:
- **Scalability** — any server instance can handle any request; no sticky sessions needed
- **Simplicity** — no server-side session storage to manage

If the API uses server-side sessions, load balancers must route the same user to the same server (sticky sessions) — this limits horizontal scaling and creates a single point of failure.

---

**Q3. What is the difference between `404 Not Found` and `400 Bad Request`? When do you return each?**

`400 Bad Request` — the **client's input is wrong**: missing required fields, invalid format, validation failure. The client must fix the request before retrying.

`404 Not Found` — the **resource does not exist**: `GET /products/999` where product 999 is not in the database. The input is structurally correct, but the referenced resource is missing.

Common mistake: returning 500 for a missing resource — always return 404 for "not found" scenarios.

---

**Q4. Why do we use DTOs instead of returning JPA entities directly from controllers?**

Five reasons:
1. **Security** — entities may have sensitive fields (`password`, internal codes) that would be serialised
2. **Serialisation errors** — LAZY-loaded associations cause `LazyInitializationException` after the Hibernate session closes
3. **Infinite loops** — bidirectional relationships (e.g., `Order` ↔ `OrderItem`) cause `StackOverflowError` during JSON serialisation
4. **Decoupling** — DB schema changes should not immediately break the API contract
5. **Shape mismatch** — what you receive (`ProductRequest`) and what you return (`ProductResponse`) are often different from what's in the DB

---

**Q5. What is HATEOAS? Do you need it for every REST API?**

HATEOAS (Hypermedia As The Engine Of Application State) — the idea that API responses include **links** to related actions, so clients can discover what they can do next without hardcoding URLs.

```json
{
  "id": 1,
  "name": "Laptop",
  "_links": {
    "self": { "href": "/api/products/1" },
    "delete": { "href": "/api/products/1" }
  }
}
```

In practice: most real-world APIs (level 2 Richardson Maturity) skip HATEOAS. It's more important to know the concept for interviews than to implement it on every project.

---

**Q6. What is the Richardson Maturity Model?**

A scale that measures how "RESTful" an API is:

| Level | Description | Example |
|---|---|---|
| 0 | Single URI, all actions via POST (RPC-style) | `POST /service` with action in body |
| 1 | Multiple URIs (resources), still all POST | `POST /products`, `POST /orders` |
| 2 | Resources + correct HTTP verbs | `GET /products`, `POST /products`, `DELETE /products/1` |
| 3 | Level 2 + HATEOAS | Responses include links to next actions |

Most production APIs are at Level 2. Level 3 is the "true" REST but rare in practice.

---

**Q7. How do you handle versioning in a REST API? What are the trade-offs?**

Three common approaches:

| Strategy | Example | Trade-off |
|---|---|---|
| URI versioning | `/api/v1/products` | Simple, easy to test. Pollutes the URL space |
| Header versioning | `Accept: application/vnd.app.v1+json` | Clean URLs. Harder to test in a browser |
| Query param | `/api/products?version=1` | Easy. Not RESTful (query params should filter, not version) |

URI versioning is most widely adopted and the easiest to understand.

---

**Q8. What is the purpose of `ETag` and `If-None-Match` headers? How do they relate to caching?**

`ETag` is a hash/version of a resource's content sent by the server:
```
GET /api/products/1
Response: ETag: "abc123"
```

Client stores it and sends on next request:
```
GET /api/products/1
Request: If-None-Match: "abc123"
```

If the resource hasn't changed, server returns `304 Not Modified` with no body — saving bandwidth. If it changed, server returns `200 OK` with the new content and a new ETag.

Useful for read-heavy APIs where resources change infrequently.

---

**Q9. What is the difference between a REST API and an RPC-style API? Why does it matter?**

**REST** — resource-oriented. URLs represent *things* (nouns). HTTP verbs describe the action.
```
DELETE /api/orders/5
```

**RPC-style** — action-oriented. URLs represent *operations* (verbs).
```
POST /api/cancelOrder?id=5
```

RPC-style ignores HTTP semantics — everything becomes POST, status codes lose meaning, and caching becomes impossible. REST APIs are easier to reason about, cache, and consume.

---

**Q10. How would you design an endpoint that needs to return large amounts of data? What strategies can you use?**

Options:

1. **Pagination** — `?page=0&size=20` — return one page at a time (most common)
2. **Cursor-based pagination** — use a cursor (last seen ID) instead of page number — more stable for large, frequently updated datasets
3. **Filtering** — `?category=laptops&minPrice=500` — reduce result set server-side
4. **Projection / field selection** — only return the fields the client asks for
5. **Streaming** — return `application/x-ndjson` (newline-delimited JSON) using `ResponseBodyEmitter` or Spring WebFlux for truly large datasets

Always avoid returning unbounded lists — always set a max page size.

---

## 4. Spring Boot

---

**Q1. What does `@SpringBootApplication` actually do? What three annotations does it combine?**

`@SpringBootApplication` is a composed annotation combining:
1. `@Configuration` — marks this as a configuration class (source of `@Bean` definitions)
2. `@EnableAutoConfiguration` — tells Spring Boot to auto-configure beans based on classpath dependencies
3. `@ComponentScan` — scans the current package and all sub-packages for `@Component`, `@Service`, `@Repository`, `@Controller` etc.

This is why the main class must be in the **root package** — `@ComponentScan` scans downward from there.

---

**Q2. How does Spring Boot auto-configuration work internally?**

Spring Boot scans `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (or `spring.factories` in older versions) inside each starter JAR.

Each entry is an auto-configuration class annotated with `@ConditionalOn*` annotations:
```java
@ConditionalOnClass(DataSource.class)       // only if DataSource is on classpath
@ConditionalOnMissingBean(DataSource.class) // only if you haven't defined your own
public class DataSourceAutoConfiguration { ... }
```

So Spring Boot only configures what you haven't already configured yourself. Adding a `@Bean` of the same type overrides the auto-configured one.

---

**Q3. What is the purpose of `application.properties` vs `application.yml`? Are they interchangeable?**

Both configure the same Spring environment — they are functionally interchangeable. `application.yml` uses YAML indentation which is cleaner for nested properties:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
```

vs

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
```

You can have both in the same project, but it can be confusing — pick one convention and stick to it.

---

**Q4. What is the purpose of Spring Boot profiles? How do you activate one?**

Profiles allow you to have **environment-specific configuration** — different DB credentials for dev vs prod, different logging levels etc.

Activate a profile:
- `application.properties`: `spring.profiles.active=dev`
- JVM arg: `-Dspring.profiles.active=prod`
- Environment variable: `SPRING_PROFILES_ACTIVE=prod`

Profile-specific files: `application-dev.properties`, `application-prod.properties` — values in these override the base `application.properties` when that profile is active.

---

**Q5. What is an embedded server in Spring Boot? What is the default? How do you switch to Jetty?**

Spring Boot includes an embedded Tomcat by default — the application runs as a plain Java process (`java -jar app.jar`), no external Tomcat installation needed.

To switch to Jetty:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jetty</artifactId>
</dependency>
```

---

**Q6. What is `CommandLineRunner`? When would you use it?**

`CommandLineRunner` is a functional interface with a single `run(String... args)` method. A bean implementing it is invoked **once, after the ApplicationContext is fully loaded**, before the application starts accepting requests.

Use cases:
- Seed/load initial data into the database at startup
- Pre-warm a cache
- Run a one-off data migration
- Print startup diagnostics

```java
@Component
public class DataSeeder implements CommandLineRunner {
    @Override
    public void run(String... args) {
        // runs after all beans are ready
    }
}
```

---

**Q7. What is Spring Boot DevTools and what does it give you?**

`spring-boot-devtools` is a development-time dependency (should never go to production) that provides:

- **Automatic restart** — detects class changes and restarts the Spring context quickly (much faster than a full restart)
- **LiveReload** — triggers a browser refresh when static resources change (works with browser LiveReload extension)
- **Relaxed caching** — disables template caching so Thymeleaf/Freemarker changes are reflected immediately
- **H2 console** — auto-enables the H2 console when H2 is on the classpath

Add `<optional>true</optional>` in pom.xml so it is never included in production JARs.

---

**Q8. What is `@ConfigurationProperties`? How is it different from `@Value`?**

`@Value("${property.key}")` — injects a single property value into a field.

`@ConfigurationProperties(prefix = "app")` — binds an entire group of properties to a POJO:

```yaml
app:
  name: My App
  max-connections: 50
  timeout: 30s
```

```java
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String name;
    private int maxConnections;
    private Duration timeout;
}
```

`@ConfigurationProperties` is better for grouped config — type-safe, supports complex types, and works with IDE autocompletion (with `spring-boot-configuration-processor`).

---

**Q9. How does Spring Boot's `@Conditional` family of annotations work? Name three and explain their conditions.**

`@Conditional*` annotations control whether a `@Bean` or auto-configuration class is registered based on runtime conditions:

- `@ConditionalOnClass(Foo.class)` — only if `Foo` is on the classpath
- `@ConditionalOnMissingBean(Foo.class)` — only if no bean of type `Foo` is already registered
- `@ConditionalOnProperty(name = "feature.enabled", havingValue = "true")` — only if the property is set to `true`
- `@ConditionalOnWebApplication` — only in a web application context
- `@ConditionalOnExpression("${app.mode} == 'dev'")` — evaluates a SpEL expression

These are what make auto-configuration smart — it backs off when you provide your own beans.

---

**Q10. What is the Spring Boot Fat JAR? What does it contain? How is it different from a standard JAR?**

A standard JAR contains only **your** compiled classes and resources — you need all dependency JARs separately on the classpath.

A Spring Boot Fat JAR (also called an Uber JAR or executable JAR) contains:
- Your compiled classes
- All dependency JARs **nested inside** `BOOT-INF/lib/`
- Spring Boot's custom launcher (`JarLauncher`) in the manifest

The launcher knows how to load classes from nested JARs, so you can run the app with simply:
```
java -jar myapp.jar
```

Built by the `spring-boot-maven-plugin` with the `repackage` goal.

---

## 5. JPA (Core Concepts)

---

**Q1. What is JPA? What is Hibernate? How do they relate?**

**JPA** (Jakarta Persistence API) is a **specification** — a set of interfaces and annotations that defines how Java objects should be mapped to relational databases. It is not an implementation.

**Hibernate** is the most popular **implementation** of JPA. It provides the actual code that talks to the database.

Spring Boot auto-configures Hibernate as the JPA provider when you add `spring-boot-starter-data-jpa`. You write JPA annotations (`@Entity`, `@OneToMany` etc.) and Hibernate executes the SQL.

---

**Q2. What is the difference between `FetchType.LAZY` and `FetchType.EAGER`?**

`EAGER` — related data is loaded **immediately** in the same query (or a join). Convenient but can load too much data.

`LAZY` — a proxy is returned. The related data is only fetched from the DB **when you first access it** (e.g., call `getOrders()`). More efficient, but the session must be open when you access it — if not, `LazyInitializationException`.

Default behaviour:
- `@ManyToOne`, `@OneToOne` → EAGER by default
- `@OneToMany`, `@ManyToMany` → LAZY by default

**Best practice**: always use LAZY and fetch eagerly only when needed via `JOIN FETCH`.

---

**Q3. What is the `@Entity` annotation? What are the minimum requirements for a JPA entity class?**

`@Entity` marks a class as a JPA entity — Hibernate maps it to a DB table.

Minimum requirements:
1. `@Entity` annotation on the class
2. A **no-argument constructor** (can be `protected`)
3. A field annotated with `@Id` (the primary key)

Optional but common:
- `@Table(name = "...")` if the table name differs from the class name
- `@GeneratedValue` to auto-generate the primary key

---

**Q4. Explain the JPA entity states: Transient, Persistent, Detached, Removed.**

| State | Description | What it means |
|---|---|---|
| Transient | Object exists in Java but not known to the persistence context | `new Product()` — Hibernate doesn't track it |
| Persistent (Managed) | Object is associated with an active persistence context | Hibernate tracks changes; changes are auto-synced to DB on flush |
| Detached | Was persistent, but the session/persistence context has closed | Changes are not tracked; must `merge()` to re-attach |
| Removed | Marked for deletion | Will be `DELETE`d from DB on next flush/commit |

---

**Q5. What is the persistence context? How does dirty checking work?**

The persistence context is Hibernate's **first-level cache** — it holds all managed entities for the current session/transaction.

**Dirty checking**: at the end of a transaction (on `flush`), Hibernate compares the current state of all managed entities against a snapshot taken when they were loaded. If any field changed, Hibernate automatically generates an `UPDATE` SQL — **without you calling `save()`**.

This is why modifying an entity inside a `@Transactional` method is enough — you don't need to explicitly call `save()` on it.

---

**Q6. What is the difference between `@OneToOne`, `@OneToMany`, `@ManyToOne`, and `@ManyToMany`?**

| Annotation | Example | Who holds the FK? |
|---|---|---|
| `@OneToOne` | User ↔ UserProfile (1:1) | Either side; typically the child |
| `@ManyToOne` | Many Orders → One Customer | The `@ManyToOne` side (Orders table has `customer_id`) |
| `@OneToMany` | One Customer → Many Orders | The other side (mapped by `@ManyToOne` in Orders) |
| `@ManyToMany` | Students ↔ Courses | Join table (e.g., `student_courses`) |

For bidirectional relationships, one side is the **owner** (has the FK / join column), the other uses `mappedBy`. Only the owner side controls the actual database join.

---

**Q7. What is `CascadeType` in JPA? What does `CascadeType.ALL` do?**

Cascade tells Hibernate to **propagate operations** from parent to child automatically.

| CascadeType | Effect |
|---|---|
| `PERSIST` | Saving parent also saves children |
| `MERGE` | Merging parent also merges children |
| `REMOVE` | Deleting parent also deletes children |
| `REFRESH` | Refreshing parent also refreshes children |
| `DETACH` | Detaching parent also detaches children |
| `ALL` | All of the above |

**Warning**: `CascadeType.REMOVE` (or `ALL`) on a `@ManyToMany` relationship can accidentally delete shared entities. Use carefully.

---

**Q8. What is `orphanRemoval = true`? How is it different from `CascadeType.REMOVE`?**

`CascadeType.REMOVE` — when the parent is deleted, children are also deleted.

`orphanRemoval = true` — when a child is **removed from the parent's collection** (without deleting the parent), the child is automatically deleted from the DB.

```java
// With orphanRemoval = true:
order.getItems().remove(item);  // triggers DELETE on item — no need to call itemRepo.delete(item)
```

Use `orphanRemoval = true` on `@OneToMany` where children have no meaning without the parent (e.g., `Order` → `OrderItem`).

---

**Q9. What is the difference between `@Column(nullable = false)` and `@NotNull`?**

| | `@Column(nullable = false)` | `@NotNull` |
|---|---|---|
| Layer | Database schema (DDL) | Java / Bean Validation |
| Effect | Adds `NOT NULL` constraint to the column in the DB | Validates the value in Java before sending to DB |
| When checked | At DB level on INSERT/UPDATE | At application level (with `@Valid`) |

Best practice: use **both** — `@NotNull` catches issues early at the API layer, `@Column(nullable = false)` provides a DB-level safety net.

---

**Q10. What is `@GeneratedValue`? What strategies are available?**

`@GeneratedValue` tells Hibernate how to generate primary key values automatically.

| Strategy | Description |
|---|---|
| `AUTO` | Hibernate chooses the strategy based on the DB dialect (default) |
| `IDENTITY` | Uses DB auto-increment column (`AUTO_INCREMENT` in MySQL) |
| `SEQUENCE` | Uses a DB sequence (PostgreSQL, Oracle) — more efficient for batch inserts |
| `TABLE` | Uses a separate table to simulate a sequence — portable but slow |

For MySQL: use `IDENTITY`. For PostgreSQL: use `SEQUENCE`. `AUTO` usually maps to `IDENTITY` on MySQL.

---

## 6. Spring Security

---

**Q1. What does Spring Security do by default when you add `spring-boot-starter-security` to a project?**

With no extra configuration, Spring Security:
- Enables HTTP Basic authentication on all endpoints
- Generates a random password printed to the console at startup
- Requires authentication for every URL
- Creates an in-memory `UserDetailsService` with username `user`
- Enables CSRF protection
- Enables session management

To override the defaults, create a `@Configuration` class that provides a `SecurityFilterChain` bean.

---

**Q2. What is the Spring Security Filter Chain? How does a request flow through it?**

Spring Security is implemented as a chain of `Filter` objects (a `FilterChainProxy`) that intercepts every HTTP request before it reaches the `DispatcherServlet`.

Key filters in the chain:
1. `SecurityContextPersistenceFilter` — loads the `SecurityContext` from session/token
2. `UsernamePasswordAuthenticationFilter` — handles form login
3. `BearerTokenAuthenticationFilter` — validates JWT/Bearer tokens
4. `ExceptionTranslationFilter` — translates security exceptions to 401/403 responses
5. `FilterSecurityInterceptor` — makes the final authorisation decision (allow or deny)

Each filter can either handle the request, pass it along, or reject it.

---

**Q3. What is `SecurityFilterChain`? How do you configure it to permit some endpoints and require auth on others?**

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())  // disable for REST APIs (stateless)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()   // public endpoints
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated()                  // all others need auth
        )
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // no sessions for REST
        );
    return http.build();
}
```

---

**Q4. What is JWT? What are its three parts?**

JWT (JSON Web Token) is a compact, self-contained token for transmitting identity and claims between parties.

Three base64-encoded parts separated by `.`:
1. **Header** — algorithm and token type: `{"alg": "HS256", "typ": "JWT"}`
2. **Payload** — claims (user data): `{"sub": "user1", "roles": ["USER"], "exp": 1710000000}`
3. **Signature** — `HMACSHA256(base64(header) + "." + base64(payload), secretKey)` — ensures the token hasn't been tampered with

The server verifies the signature — if valid, it trusts the payload without hitting the database.

---

**Q5. What is the difference between `401 Unauthorized` and `403 Forbidden`?**

`401 Unauthorized` — **not authenticated**. The request has no credentials or invalid credentials. The client should log in and retry. (Despite the name, it means "unauthenticated".)

`403 Forbidden` — **authenticated but not authorised**. The server knows who you are, but you don't have permission to access this resource. Logging in again won't help — you need a different role.

Common mistake: returning 401 when you mean 403 (user is logged in but lacks a role).

---

**Q6. What is CSRF? Why do you disable it in REST APIs?**

CSRF (Cross-Site Request Forgery) — an attack where a malicious website tricks a logged-in user's browser into making an unwanted request to your site (using the stored session cookie).

Spring Security enables CSRF protection by default by requiring a CSRF token on state-changing requests (POST, PUT, DELETE).

For REST APIs: CSRF is not a threat because:
- REST APIs are stateless — no session cookies are used
- The client sends a JWT in the `Authorization` header, which a malicious site cannot access (it's not a cookie)
- Only cookies are automatically sent cross-site by browsers

So `csrf.disable()` is the standard setting for REST APIs.

---

**Q7. What is `UserDetailsService`? How does Spring Security use it during authentication?**

`UserDetailsService` is an interface with one method:
```java
UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
```

During authentication, Spring Security calls this method to load the user's details from your data source (DB, LDAP etc.). It returns a `UserDetails` object (username, password hash, roles, account status).

Spring Security then compares the provided password (using `PasswordEncoder`) with the stored hash. If they match, authentication succeeds and the user is stored in the `SecurityContext`.

You implement this interface to connect Spring Security to your own User table.

---

**Q8. What is `PasswordEncoder`? Why must you never store passwords in plain text?**

`PasswordEncoder` is a Spring Security interface for hashing passwords before storage and comparing them during login.

The recommended implementation is `BCryptPasswordEncoder`:
- Uses the bcrypt adaptive hashing algorithm with a salt
- Computationally slow by design — makes brute-force attacks impractical
- Produces a different hash each time even for the same password (due to random salt)

Plain text passwords in a DB breach expose every user's credentials immediately. Even if your DB is compromised, bcrypt-hashed passwords are practically uncrackable within a reasonable time frame.

---

**Q9. What is method-level security? What annotations does Spring Security provide for it?**

Method-level security lets you apply authorisation checks directly on service/controller methods instead of (or in addition to) URL-based rules.

Enable it with `@EnableMethodSecurity` on a config class.

| Annotation | Description |
|---|---|
| `@PreAuthorize("hasRole('ADMIN')")` | Checks authority before the method runs |
| `@PostAuthorize("returnObject.owner == authentication.name")` | Checks after the method returns |
| `@Secured({"ROLE_USER", "ROLE_ADMIN"})` | Simple role check (less expressive) |
| `@PreFilter` / `@PostFilter` | Filters collections passed in or returned |

`@PreAuthorize` with SpEL expressions is the most flexible and commonly used.

---

**Q10. Where is the `SecurityContext` stored? What is `SecurityContextHolder`?**

`SecurityContextHolder` is a static class that holds the current user's `SecurityContext` in a `ThreadLocal` variable — one per thread.

After successful authentication, the `SecurityContext` (containing the `Authentication` object with roles and identity) is stored there. You access it anywhere in your code:
```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String username = auth.getName();
```

For stateless REST APIs, the security filter reads the JWT on each request, rebuilds the `Authentication`, stores it in the `SecurityContextHolder`, and clears it after the request completes.

---

## 7. Spring Data JPA

---

**Q1. What does Spring Data JPA give you on top of plain JPA/Hibernate?**

Spring Data JPA eliminates boilerplate repository code. Instead of writing `EntityManager` calls manually, you:
1. Create an interface extending `JpaRepository<Entity, IdType>`
2. Get CRUD methods (`findAll`, `findById`, `save`, `delete`) for free
3. Write derived query methods by naming conventions (`findByName`, `findByPriceGreaterThan`)
4. Use `@Query` for complex JPQL/native queries
5. Get automatic pagination and sorting support via `PagingAndSortingRepository`

Spring Data generates the implementation at runtime using dynamic proxies.

---

**Q2. How do derived query methods work? Give three examples.**

Spring Data JPA parses the method name and generates a JPQL query at startup.

```java
// findBy + field name + condition keyword
List<Product> findByName(String name);
// → SELECT p FROM Product p WHERE p.name = ?1

List<Product> findByPriceGreaterThan(BigDecimal price);
// → SELECT p FROM Product p WHERE p.price > ?1

List<Product> findByNameContainingAndActiveTrue(String keyword);
// → SELECT p FROM Product p WHERE p.name LIKE %?1% AND p.active = true
```

Keywords: `And`, `Or`, `Between`, `LessThan`, `GreaterThan`, `Like`, `Containing`, `StartingWith`, `EndingWith`, `IsNull`, `IsNotNull`, `OrderBy`, `Top`, `First`, `Distinct`

---

**Q3. When would you use `@Query` instead of a derived query method?**

Use `@Query` when:
- The derived method name becomes unreadably long
- You need a `JOIN FETCH` to solve N+1 queries
- You need aggregation (`COUNT`, `SUM`, `AVG`)
- You need a native SQL query (`nativeQuery = true`)
- You need a `GROUP BY` or `HAVING` clause

```java
@Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.price > :minPrice")
List<Product> findExpensiveProductsWithCategory(@Param("minPrice") BigDecimal minPrice);
```

---

**Q4. What is the difference between `save()` returning a new object and the original? Why should you always use the returned value?**

`save()` calls either `persist` (new entity) or `merge` (existing entity) on the `EntityManager`.

For `merge`: Hibernate copies the state of your object into a **new managed instance** and returns it. Your original object becomes detached.

```java
Product detached = new Product();
detached.setId(5L);
Product managed = productRepository.save(detached); // always use `managed`, not `detached`
```

Using the original `detached` object after `save()` means you're working with a detached entity — further Hibernate operations on it may fail or not track changes.

---

**Q5. What is the difference between `findById()` returning `Optional` vs throwing an exception?**

`findById(id)` returns `Optional<Entity>` — the entity may or may not exist.

You have two choices:
```java
// Option 1: throw a meaningful exception
Product p = productRepository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));

// Option 2: use if present
productRepository.findById(id).ifPresent(p -> { ... });
```

**Never** call `.get()` on `Optional` without checking — it throws `NoSuchElementException` if empty. Always use `orElseThrow()` or `orElse()`.

---

**Q6. What is `@Modifying`? When do you need it?**

`@Modifying` is required on `@Query` methods that perform `UPDATE` or `DELETE` operations (not just `SELECT`).

```java
@Modifying
@Transactional
@Query("UPDATE Product p SET p.active = false WHERE p.category.id = :categoryId")
int deactivateByCategory(@Param("categoryId") Long categoryId);
```

Without `@Modifying`, Spring Data will try to execute the query as a `SELECT` and throw an exception.

Also add `@Transactional` on the method — `@Modifying` queries require an active transaction.

---

**Q7. What is the difference between `JpaRepository`, `CrudRepository`, and `PagingAndSortingRepository`?**

They form an inheritance hierarchy:

```
CrudRepository
  └── PagingAndSortingRepository
        └── JpaRepository
```

| Interface | Adds |
|---|---|
| `CrudRepository` | Basic CRUD: `save`, `findById`, `findAll`, `delete`, `count` |
| `PagingAndSortingRepository` | `findAll(Pageable)`, `findAll(Sort)` |
| `JpaRepository` | `saveAll`, `flush`, `saveAndFlush`, `deleteInBatch`, JPA-specific operations |

In practice, always extend `JpaRepository` — it includes everything from the parents plus JPA-specific methods.

---

**Q8. What is a `Pageable` and how do you create one?**

`Pageable` is a Spring Data interface representing pagination and sorting instructions:
- Which page (0-indexed)
- How many items per page
- Sort order

Create it with `PageRequest`:
```java
Pageable pageable = PageRequest.of(0, 10, Sort.by("price").descending());
Page<Product> page = productRepository.findAll(pageable);
```

The returned `Page<T>` contains:
- `getContent()` — the list of entities
- `getTotalElements()` — total count in DB
- `getTotalPages()` — total pages
- `isFirst()`, `isLast()`, `hasNext()`, `hasPrevious()`

---

**Q9. What is the `Specification` pattern in Spring Data JPA? When would you use it?**

`Specification<T>` allows you to build dynamic, composable queries at runtime.

Used when the filter criteria are unknown at compile time (e.g., search forms with multiple optional filters).

```java
public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> { }

// Build a spec dynamically:
Specification<Product> spec = Specification
    .where(hasCategory(categoryId))
    .and(hasPriceBelow(maxPrice))
    .and(isActive());

List<Product> results = productRepository.findAll(spec);
```

Each `Specification` is a small predicate that can be composed with `.and()` / `.or()`.

---

**Q10. What is `@EntityGraph`? How does it help solve the N+1 problem?**

`@EntityGraph` tells Hibernate which associations to eagerly load for a specific query, without changing the default `FetchType` on the entity.

```java
@EntityGraph(attributePaths = {"category", "tags"})
List<Product> findAll();
```

This generates a single `JOIN FETCH` query instead of N+1 separate queries — but only for this specific repository method. The entity's default `FetchType.LAZY` is preserved for all other queries.

Alternative to `@Query("SELECT p FROM Product p JOIN FETCH p.category")` when you want to keep the query implicit.

---

## 8. Microservices — Config Server, API Gateway, Eureka

---

**Q1. What problem does a Config Server solve? Why not just use `application.properties` in each service?**

In a microservices system with 10+ services, each with dev/staging/prod environments, managing config in each service's `application.properties` means:
- Config changes require code changes and redeployment
- No single place to audit what config each service is using
- Secrets (DB passwords, API keys) are scattered across repos

**Config Server** centralises all configuration in one place (e.g., a Git repo). Services fetch their config at startup via HTTP. To change a DB URL, you update one file — no redeployment needed (with `@RefreshScope`).

---

**Q2. How does a service know where to find the Config Server?**

The service must have `spring-cloud-starter-config` on its classpath and a `bootstrap.properties` (or `application.properties` with spring config import) that points to the Config Server **before** the main context loads:

```properties
spring.application.name=order-service
spring.config.import=optional:configserver:http://localhost:8888
```

The Config Server then serves the file matching the service name: `order-service.properties` (or `order-service-dev.properties` for the dev profile).

---

**Q3. What is Eureka? What roles do Eureka Server and Eureka Client play?**

Eureka is a **service discovery** server from Netflix, integrated into Spring Cloud.

**Eureka Server** — a registry. Runs as a standalone Spring Boot app (`@EnableEurekaServer`). All services register themselves here.

**Eureka Client** — any microservice with `@EnableEurekaClient` (or `spring-cloud-starter-netflix-eureka-client`). On startup it registers with the server (name, host, port). It also polls the server periodically to get an up-to-date list of all registered services.

Why it matters: services don't need to hardcode each other's IP/port. They look up each other by service name.

---

**Q4. How does a service call another service using Eureka + OpenFeign?**

1. Both services register with Eureka
2. The calling service has `spring-cloud-starter-openfeign` and `@EnableFeignClients` on the main class
3. Declare a Feign client interface:

```java
@FeignClient(name = "product-service")   // name must match spring.application.name of target
public interface ProductClient {
    @GetMapping("/api/products/{id}")
    ProductResponse getProductById(@PathVariable Long id);
}
```

4. Inject and call it like any Spring bean — Feign handles the HTTP call, Eureka resolves `product-service` to an actual host:port, and the load balancer picks one instance.

---

**Q5. What is an API Gateway? What problems does it solve?**

An API Gateway is the **single entry point** for all client requests to the microservices backend.

Problems it solves:
- **Routing** — routes `/api/products/**` to product-service, `/api/orders/**` to order-service
- **Security** — centralises JWT validation; individual services don't need to re-implement auth
- **Rate limiting** — throttles requests at one place
- **Load balancing** — distributes traffic across service instances
- **Cross-cutting concerns** — logging, request tracing, CORS in one place
- **API aggregation** — can combine responses from multiple services

Without a gateway, clients must know the network address of every service.

---

**Q6. How do you configure routing in Spring Cloud Gateway?**

In `application.yml`:
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: product-service
          uri: lb://product-service       # lb:// = Eureka load-balanced
          predicates:
            - Path=/api/products/**
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
          filters:
            - StripPrefix=1
```

`lb://` tells the gateway to resolve the service name via Eureka. `predicates` define URL matching rules. `filters` transform requests/responses.

---

**Q7. What is service registration and heartbeat in Eureka?**

**Registration** — when a Eureka client starts, it sends a `POST` to the Eureka Server with its metadata (name, IP, port, health check URL). The server adds it to the registry.

**Heartbeat (Lease Renewal)** — every 30 seconds (by default) the client sends a `PUT` to the server saying "I'm still alive." If the server doesn't receive a heartbeat for 90 seconds (3 missed intervals), it marks the instance as `DOWN` and eventually removes it from the registry.

This mechanism ensures that crashed services are automatically deregistered and clients don't route traffic to dead instances.

---

**Q8. What is the difference between client-side load balancing and server-side load balancing?**

**Server-side load balancing** — a dedicated load balancer (Nginx, AWS ALB) sits between the client and servers. The client always talks to the load balancer, which picks a server. Simple for the client, but adds a network hop.

**Client-side load balancing** (Spring Cloud LoadBalancer / Ribbon) — the client fetches the list of service instances from Eureka and picks one itself using a load-balancing algorithm (round-robin by default). No extra network hop; the client talks directly to the chosen instance.

Feign + Eureka uses client-side load balancing by default.

---

**Q9. What is `@EnableEurekaServer` vs `@EnableDiscoveryClient`?**

`@EnableEurekaServer` — marks a Spring Boot application as the **Eureka registry server**. Used only on the service discovery server app.

`@EnableDiscoveryClient` — marks a Spring Boot application as a **client** that registers with and discovers services from a discovery server. It's a generic annotation that works with Eureka, Consul, Zookeeper etc.

`@EnableEurekaClient` is Eureka-specific; `@EnableDiscoveryClient` is the more portable option. In modern Spring Cloud, simply having the Eureka client dependency on the classpath is enough — no annotation required.

---

**Q10. What happens if the Config Server or Eureka Server is down when a service starts?**

**Config Server down at startup** — by default the service fails to start if it can't fetch config. You can make it optional with `optional:configserver:` prefix — the service falls back to its local `application.properties`.

**Eureka Server down at startup** — with `eureka.client.fetch-registry=true`, the client caches the registry. If Eureka is down:
- Already-started services continue using their **cached registry** — they can still call each other
- New services fail to register (but may start depending on config)
- After Eureka comes back, services re-register automatically

For resilience, run Eureka in a **cluster** (at least 2 replicas) in production.

---

## 9. Docker

---

**Q1. What is Docker? What problem does it solve?**

Docker is a **containerisation platform**. It packages your application along with all its dependencies (JRE, config, libraries) into a **container image** — a single portable unit.

Problem it solves: "works on my machine" — different developer machines, CI servers, and production environments have different OS versions, JDK versions, installed software. Docker eliminates these differences. The container runs the same way everywhere: developer laptop, CI, staging, production.

---

**Q2. What is the difference between a Docker Image and a Docker Container?**

| | Image | Container |
|---|---|---|
| What is it | A read-only snapshot (blueprint) | A running instance of an image |
| Analogy | Class in Java | Object (instance) of that class |
| Stored | On disk / Docker Hub | In memory (running process) |
| Lifecycle | Built once, shared | Created, started, stopped, deleted |

You `build` an image and `run` it to create a container. Multiple containers can run from the same image simultaneously.

---

**Q3. What is a `Dockerfile`? Walk through the key instructions.**

A `Dockerfile` is a text file with step-by-step instructions for building a Docker image.

```dockerfile
FROM eclipse-temurin:17-jre          # Base image (JRE 17)
WORKDIR /app                          # Set working directory inside container
COPY target/myapp.jar app.jar         # Copy the fat JAR into the image
EXPOSE 8080                           # Document that the app uses port 8080
ENTRYPOINT ["java", "-jar", "app.jar"] # Command to run when container starts
```

| Instruction | Purpose |
|---|---|
| `FROM` | Base image to start from |
| `WORKDIR` | Sets the working directory |
| `COPY` / `ADD` | Copy files from host into image |
| `RUN` | Execute a command during image build |
| `EXPOSE` | Documents the port (doesn't actually open it) |
| `ENTRYPOINT` / `CMD` | Command to run when container starts |

---

**Q4. What is the difference between `ENTRYPOINT` and `CMD`?**

`CMD` — provides default arguments. Can be overridden by passing arguments to `docker run`.

`ENTRYPOINT` — defines the main command. Not easily overridden.

When used together:
```dockerfile
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--spring.profiles.active=dev"]   # default arg, override with: docker run myapp --spring.profiles.active=prod
```

Best practice for Spring Boot apps: use `ENTRYPOINT` for the `java -jar` command and `CMD` for overridable Spring arguments.

---

**Q5. What is Docker Compose? Why is it useful for a microservices project?**

Docker Compose lets you define and run **multi-container applications** using a single `docker-compose.yml` file.

Instead of running 5 separate `docker run` commands for API gateway, order-service, product-service, MySQL, and Eureka, you define them all in one file and start everything with `docker compose up`.

Benefits:
- Single command to start the entire system
- Automatic networking — services find each other by service name (no hardcoded IPs)
- Environment variables, volume mounts, port mappings in one place
- Great for local development and integration testing

---

**Q6. What is a Docker volume? Why do you need it for a MySQL container?**

By default, data written inside a container is **ephemeral** — if the container is removed, all data is lost.

A **volume** mounts a directory from the host filesystem (or Docker-managed storage) into the container, so data persists across container restarts and removals.

For MySQL:
```yaml
services:
  mysql:
    image: mysql:8.0
    volumes:
      - mysql_data:/var/lib/mysql   # DB files stored in named volume, not inside container

volumes:
  mysql_data:
```

Without a volume, your database is wiped every time you recreate the container.

---

**Q7. What is the difference between `docker build`, `docker run`, and `docker compose up`?**

| Command | What it does |
|---|---|
| `docker build -t myapp .` | Reads the `Dockerfile` in the current directory and creates an image named `myapp` |
| `docker run -p 8080:8080 myapp` | Creates and starts a container from the `myapp` image, mapping host port 8080 to container port 8080 |
| `docker compose up` | Reads `docker-compose.yml`, builds images if needed, creates and starts all defined services |

`docker compose up --build` forces a rebuild of images before starting.

---

**Q8. What is the `-p` flag in `docker run`? What does `8080:8080` mean?**

`-p host_port:container_port` — maps (publishes) a port from the container to the host.

`-p 8080:8080`:
- Left `8080` — the port on your **host machine** (your laptop or server)
- Right `8080` — the port **inside the container** where the app listens

After this, `http://localhost:8080` on your machine routes to port 8080 inside the container.

If you run two containers of the same image, they both use port 8080 internally, but you map them to different host ports: `-p 8081:8080` and `-p 8082:8080`.

---

**Q9. How do services in a `docker-compose.yml` communicate with each other?**

Docker Compose creates a private virtual network for all services in the same compose file. Each service is reachable by its **service name** as a hostname — Docker handles the DNS resolution internally.

```yaml
services:
  order-service:
    environment:
      PRODUCT_SERVICE_URL: http://product-service:8080   # 'product-service' is the service name
  product-service:
    ports:
      - "8081:8080"
```

`order-service` calls `http://product-service:8080` — Docker resolves `product-service` to the correct container IP. Services do not need to expose ports to communicate with each other — only to the host.

---

**Q10. What is a multi-stage Docker build? Why would you use it for a Spring Boot application?**

A multi-stage build uses multiple `FROM` statements in one `Dockerfile`. Each stage can be based on a different image, and you copy only the artifacts you need from one stage to the next.

For Spring Boot:
```dockerfile
# Stage 1: Build — needs full JDK + Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run — only needs JRE (much smaller)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/myapp.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Benefits:
- Final image is much smaller (JRE ~200MB vs JDK ~400MB, no Maven)
- Maven, source code, and build files are not in the final image — smaller attack surface
- Clean separation between build environment and runtime environment
