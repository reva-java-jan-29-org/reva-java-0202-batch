# Spring Interview Questions & Answers
### Week 4 — Revision (23 Feb)

**Topics:** Spring Core · IoC · Dependency Injection · Bean Lifecycle · Bean Scopes · Bean Definitions · Spring Boot · Lombok · DevTools · JPA · Spring Data JPA · Spring Web · REST APIs

---

## Spring Core & IoC

---

**Q1. What is the Spring Framework and why do we use it?**

Spring is an open-source Java framework that helps us build enterprise applications more easily. Before Spring, we had to manually create objects, manage their dependencies, and wire everything together by hand, which was a lot of boilerplate code.

Spring takes care of all that for us. The core idea is the IoC container — we just define our classes and tell Spring about their dependencies, and Spring creates and manages everything. It also provides modules for data access, web, security, testing, and more, so we don't have to reinvent the wheel.

---

**Q2. What is IoC (Inversion of Control)?**

IoC stands for Inversion of Control. Normally in a Java program, my class is responsible for creating the objects it needs. For example, if `OrderService` needs a `PaymentService`, I would write `PaymentService ps = new PaymentService()` inside `OrderService`.

With IoC, that control is inverted — instead of my class creating its dependencies, the Spring container creates them and injects them into my class. I just declare what I need, and Spring provides it. This makes the code loosely coupled and easier to test.

---

**Q3. What is the Spring IoC Container?**

The Spring IoC container is the core component of Spring. It's responsible for creating objects (called beans), wiring their dependencies together, and managing their entire lifecycle.

There are two types of IoC containers in Spring:
- `BeanFactory` — the basic container, lazy initialization, used in resource-constrained environments
- `ApplicationContext` — extends BeanFactory, adds features like event publishing, internationalization, and AOP. This is what we typically use in real projects.

When we start a Spring application, the container reads the configuration (XML, Java, or annotations), creates all the beans, injects dependencies, and keeps them ready for use.

---

**Q4. What is Dependency Injection?**

Dependency Injection (DI) is the implementation of IoC. When a class needs another object to work, instead of creating it inside, we declare it as a dependency and let the Spring container inject it from outside.

For example, if `OrderService` depends on `PaymentService`, I declare `PaymentService` as a field or constructor parameter in `OrderService`. Spring sees this and automatically injects the right `PaymentService` bean when creating `OrderService`. This makes classes more modular, reusable, and easy to test.

---

**Q5. What are the types of Dependency Injection in Spring?**

There are three types of DI in Spring:

**1. Constructor Injection** — dependencies are passed through the class constructor.
```java
@Service
public class OrderService {
    private final PaymentService paymentService;

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

**2. Setter Injection** — dependencies are set through setter methods.
```java
@Service
public class OrderService {
    private PaymentService paymentService;

    @Autowired
    public void setPaymentService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

**3. Field Injection** — Spring injects directly into the field using `@Autowired`.
```java
@Service
public class OrderService {
    @Autowired
    private PaymentService paymentService;
}
```

I personally prefer constructor injection because it makes dependencies explicit, supports `final` fields (immutability), and is easier to unit test without Spring.

---

**Q6. Which type of Dependency Injection is recommended and why?**

Constructor injection is the recommended approach and it's what the Spring team themselves recommends.

The reasons are:
- Dependencies declared in the constructor are **mandatory and visible** — you can't create the object without providing them
- It allows fields to be `final`, making the object immutable after creation
- It's easier to unit test — you can just `new` the class and pass mock objects without needing Spring
- With Lombok's `@RequiredArgsConstructor`, we don't even need to write the constructor manually

Field injection with `@Autowired` is the easiest to write but it hides dependencies, prevents using `final`, and makes testing harder.

---

**Q7. What is `@Autowired` and how does it work?**

`@Autowired` is a Spring annotation that tells the container to inject a dependency automatically. When Spring sees `@Autowired` on a field, constructor, or setter, it looks in its container for a bean of that type and injects it.

```java
@Autowired
private UserRepository userRepository;
```

Spring first tries to match by type. If there are multiple beans of the same type, it falls back to matching by name. If it still can't resolve, we use `@Qualifier` to specify which bean to inject.

Since Spring 4.3, if a class has only one constructor, `@Autowired` is not even needed — Spring automatically uses it.

---

**Q8. What is `@Qualifier` and when do we use it?**

`@Qualifier` is used when there are multiple beans of the same type and Spring doesn't know which one to inject. We use it to specify the exact bean name.

```java
public interface NotificationService { void send(String message); }

@Service("emailService")
public class EmailNotificationService implements NotificationService { ... }

@Service("smsService")
public class SmsNotificationService implements NotificationService { ... }

@Service
public class OrderService {
    @Autowired
    @Qualifier("emailService")
    private NotificationService notificationService;
}
```

Without `@Qualifier`, Spring would throw a `NoUniqueBeanDefinitionException` because it finds two beans implementing `NotificationService`.

---

**Q9. What is a Spring Bean?**

A Spring Bean is simply a Java object that is managed by the Spring IoC container. It's created, configured, and destroyed by Spring. Any POJO (Plain Old Java Object) that we register with the Spring container becomes a bean.

We can define beans using:
- `@Component`, `@Service`, `@Repository`, `@Controller` annotations
- `@Bean` methods inside `@Configuration` classes
- XML configuration (older style)

The container holds all these beans and manages their lifecycle and dependencies.

---

**Q10. What are Bean Scopes in Spring?**

Bean scope defines how many instances of a bean the Spring container creates and when.

The main scopes are:

| Scope | Description |
|-------|-------------|
| **Singleton** (default) | One instance per Spring container. All requests for this bean get the same object. |
| **Prototype** | A new instance is created every time the bean is requested. |
| **Request** | One instance per HTTP request. Only valid in a web application context. |
| **Session** | One instance per HTTP session. Only valid in web context. |
| **Application** | One instance per ServletContext. |

```java
@Component
@Scope("prototype")
public class ReportGenerator { ... }
```

Most of our service and repository beans are singleton (default), which is fine because they are stateless. We use prototype when we need a fresh instance each time, like for a non-thread-safe helper class.

---

**Q11. What is the Bean Lifecycle in Spring?**

The Spring bean lifecycle is the sequence of steps a bean goes through from creation to destruction.

The main steps are:

1. **Instantiation** — Spring creates the bean instance using the constructor
2. **Dependency Injection** — Spring injects all dependencies (`@Autowired`, constructor injection, etc.)
3. **Aware interfaces** — If the bean implements `BeanNameAware`, `ApplicationContextAware`, etc., Spring calls those methods
4. **`@PostConstruct`** — Spring calls the method annotated with `@PostConstruct` for initialization logic
5. **Bean is ready** — The bean is fully initialized and available for use
6. **`@PreDestroy`** — When the container shuts down, Spring calls this method for cleanup

```java
@Component
public class DatabaseConnection {

    @PostConstruct
    public void init() {
        System.out.println("Bean created — opening DB connection");
    }

    @PreDestroy
    public void destroy() {
        System.out.println("Container shutting down — closing DB connection");
    }
}
```

---

**Q12. What is the difference between `@Component`, `@Service`, `@Repository`, and `@Controller`?**

All four are specializations of `@Component` — they all register the class as a Spring bean. The difference is semantic (what they communicate about the role of the class) and in some cases functional:

- `@Component` — generic stereotype for any Spring-managed class
- `@Service` — marks the class as a service/business logic layer. No extra behavior, but it communicates intent clearly.
- `@Repository` — marks the class as a data access layer. Spring adds automatic exception translation (converts JDBC/JPA exceptions into Spring's `DataAccessException` hierarchy).
- `@Controller` — marks the class as a web controller for handling HTTP requests in Spring MVC.

I always use the most specific annotation because it makes the code more readable and self-documenting.

---

**Q13. How do we define beans using XML configuration?**

XML was the original way to configure Spring before annotations. We create an `applicationContext.xml` file:

```xml
<beans xmlns="http://www.springframework.org/schema/beans" ...>

    <!-- Define a bean -->
    <bean id="paymentService" class="com.example.PaymentService" />

    <!-- Constructor injection -->
    <bean id="orderService" class="com.example.OrderService">
        <constructor-arg ref="paymentService" />
    </bean>

    <!-- Setter injection -->
    <bean id="emailService" class="com.example.EmailService">
        <property name="smtpHost" value="smtp.gmail.com" />
    </bean>

</beans>
```

We load it with:
```java
ApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml");
OrderService service = ctx.getBean("orderService", OrderService.class);
```

XML configuration is verbose and error-prone (typos not caught at compile time), which is why we prefer Java-based or annotation-based configuration today.

---

**Q14. How do we define beans using Java configuration (`@Configuration`)?**

Java-based configuration uses `@Configuration` classes with `@Bean` methods:

```java
@Configuration
public class AppConfig {

    @Bean
    public PaymentService paymentService() {
        return new PaymentService();
    }

    @Bean
    public OrderService orderService() {
        // Spring injects paymentService() automatically
        return new OrderService(paymentService());
    }
}
```

This approach is type-safe (compile-time checking), easier to refactor, and works well for configuring third-party classes we can't annotate directly (like `DataSource`, `RestTemplate`).

---

**Q15. What is `@ComponentScan`?**

`@ComponentScan` tells Spring which packages to scan for annotated classes (`@Component`, `@Service`, etc.). Without it, Spring won't find our beans automatically.

```java
@Configuration
@ComponentScan(basePackages = "com.training")
public class AppConfig { }
```

In Spring Boot, `@SpringBootApplication` already includes `@ComponentScan`, so it automatically scans the package where the main class lives and all its sub-packages. That's why we always keep our main class in the root package.

---

## Spring Boot

---

**Q16. What is Spring Boot and how is it different from Spring Framework?**

Spring Framework is a comprehensive framework for building Java enterprise applications, but setting it up requires a lot of configuration — defining beans, configuring the web server, setting up data sources, etc.

Spring Boot is built on top of Spring Framework to eliminate that boilerplate setup. It follows a "convention over configuration" approach — it makes sensible assumptions and auto-configures things for us.

Key differences:
- Spring Boot has **embedded servers** (Tomcat, Jetty) — no need to deploy a WAR file separately
- **Auto-configuration** — automatically configures beans based on what's on the classpath
- **Starter dependencies** — one dependency pulls in everything needed for a feature
- **No XML configuration** needed by default
- `spring-boot-starter-parent` manages all dependency versions for us

---

**Q17. What is Spring Boot Auto-Configuration?**

Auto-configuration is one of the most powerful features of Spring Boot. It automatically configures Spring beans based on the dependencies present in the classpath and the properties we've set.

For example, if `spring-boot-starter-data-jpa` is in our `pom.xml`, Spring Boot automatically:
- Creates a `DataSource` bean using properties from `application.properties`
- Sets up a `LocalContainerEntityManagerFactoryBean`
- Configures a `TransactionManager`

We don't write any of that configuration ourselves.

Auto-configuration classes are annotated with `@AutoConfiguration` and use `@ConditionalOnClass`, `@ConditionalOnMissingBean`, etc. to decide whether to apply. This means if we define our own `DataSource` bean, Spring Boot's auto-configuration steps aside.

We can see what's being auto-configured by adding `--debug` flag or checking the `CONDITIONS EVALUATION REPORT` in logs.

---

**Q18. What are Starter Dependencies in Spring Boot?**

Starter dependencies are convenience POMs that bundle all the related dependencies for a specific feature. Instead of figuring out which 5-6 libraries we need and which versions are compatible, we add one starter.

Common starters:

| Starter | What it provides |
|---------|-----------------|
| `spring-boot-starter-web` | Spring MVC, embedded Tomcat, Jackson |
| `spring-boot-starter-data-jpa` | Spring Data JPA, Hibernate, JDBC |
| `spring-boot-starter-security` | Spring Security |
| `spring-boot-starter-test` | JUnit 5, Mockito, MockMvc |
| `spring-boot-starter-validation` | Hibernate Validator, Bean Validation |

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

This one line pulls in Spring MVC, embedded Tomcat, and Jackson — everything I need to build a REST API.

---

**Q19. What is `application.properties` / `application.yml` in Spring Boot?**

`application.properties` is the central configuration file in Spring Boot, located at `src/main/resources/`. We use it to configure server port, database credentials, JPA settings, logging levels, and our own custom properties.

```properties
server.port=8080
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=Root123
spring.jpa.hibernate.ddl-auto=update
```

Spring Boot reads this file at startup and binds the values to the appropriate auto-configuration beans. We can also use `application.yml` for the same purpose with YAML syntax, which is more readable for nested properties.

We can also have profile-specific files like `application-dev.properties`, `application-prod.properties` and activate them with `spring.profiles.active=dev`.

---

**Q20. What is `@SpringBootApplication`?**

`@SpringBootApplication` is a convenience annotation that combines three annotations:

1. `@Configuration` — marks the class as a source of bean definitions
2. `@EnableAutoConfiguration` — enables Spring Boot's auto-configuration mechanism
3. `@ComponentScan` — scans the current package and sub-packages for Spring components

```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

This is the entry point of every Spring Boot application. `SpringApplication.run()` bootstraps the application, creates the ApplicationContext, and starts the embedded server.

---

**Q21. What is Spring Boot DevTools and what does it do?**

Spring Boot DevTools is a developer productivity tool. When we add it as a dependency, it provides:

1. **Automatic restart** — whenever we change a Java class and save it, the application restarts automatically. It's faster than a full restart because it uses two classloaders — one for our code (reloaded on change) and one for libraries (not reloaded).

2. **LiveReload** — automatically refreshes the browser when static resources (HTML, CSS, JS) change.

3. **Disabled caching** — in development, template caches (Thymeleaf, etc.) are disabled so we see changes immediately.

4. **H2 Console** — enables the H2 in-memory database console automatically.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

The `optional=true` ensures DevTools is not included in the final production JAR.

---

## Lombok

---

**Q22. What is Lombok and why do we use it?**

Lombok is a Java library that eliminates boilerplate code by generating common methods at compile time using annotations. Java classes often need constructors, getters, setters, `toString()`, `equals()`, and `hashCode()` — and writing all of that manually is tedious.

With Lombok, we just add an annotation and it generates the code for us:

```java
@Data                    // generates getters, setters, equals, hashCode, toString
@NoArgsConstructor       // generates no-arg constructor
@AllArgsConstructor      // generates constructor with all fields
@Builder                 // generates builder pattern
public class Product {
    private Long id;
    private String name;
    private BigDecimal price;
}
```

Without Lombok, this would be 50+ lines of boilerplate. The generated code is invisible in the source file but is present in the compiled `.class` file.

---

**Q23. What are the most commonly used Lombok annotations?**

| Annotation | What it generates |
|------------|-------------------|
| `@Getter` | Getters for all fields (or specific field) |
| `@Setter` | Setters for all fields |
| `@Data` | `@Getter` + `@Setter` + `@ToString` + `@EqualsAndHashCode` + required args constructor |
| `@NoArgsConstructor` | Constructor with no parameters |
| `@AllArgsConstructor` | Constructor with all fields as parameters |
| `@RequiredArgsConstructor` | Constructor for all `final` and `@NonNull` fields |
| `@Builder` | Builder design pattern |
| `@Slf4j` | Creates a `log` field backed by SLF4J Logger |
| `@ToString` | `toString()` method |
| `@EqualsAndHashCode` | `equals()` and `hashCode()` |

I use `@RequiredArgsConstructor` a lot with Spring because it generates a constructor for all `final` fields, which works perfectly with constructor injection:

```java
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;  // injected via constructor
}
```

---

**Q24. Why do we use `@Getter @Setter` instead of `@Data` for JPA entities with bidirectional relationships?**

`@Data` includes `@EqualsAndHashCode` and `@ToString`, which by default include all fields. In JPA bidirectional relationships, the parent references the child and the child references the parent. If both sides are included in `toString()` or `equals()`, it causes an infinite loop and a `StackOverflowError`.

For example, `Category` has a list of `Product` objects, and each `Product` has a reference back to `Category`. Calling `category.toString()` → calls `product.toString()` → calls `category.toString()` → infinite loop.

That's why for entities with relationships, I use `@Getter @Setter` separately and exclude related collections from `@ToString` and `@EqualsAndHashCode`:

```java
@Entity
@Getter @Setter
@ToString(exclude = "products")
@EqualsAndHashCode(exclude = "products")
public class Category { ... }
```

---

## JPA

---

**Q25. What is JPA?**

JPA stands for Java Persistence API. It is a specification (a set of interfaces and rules) for how Java objects should be persisted to a relational database. JPA itself is not an implementation — it's just the standard.

Before JPA, every database framework had its own way of doing things. JPA standardized object-relational mapping (ORM) in Java so that we can write code against the JPA API and switch the underlying implementation (Hibernate, EclipseLink, etc.) without changing our code.

JPA lets us work with Java objects directly instead of writing SQL. We define entity classes that map to database tables, and JPA handles the SQL generation.

---

**Q26. What are JPA Providers and give examples?**

A JPA Provider is the actual implementation of the JPA specification. Since JPA is just an interface, we need a provider to do the actual work of connecting to the database, generating SQL, and managing the object-relational mapping.

Common JPA providers:
- **Hibernate** — the most popular, default provider in Spring Boot
- **EclipseLink** — the reference implementation of JPA
- **OpenJPA** — Apache's implementation

In Spring Boot with `spring-boot-starter-data-jpa`, Hibernate is automatically included and used as the JPA provider. When we annotate a class with `@Entity`, Hibernate is the one that reads that annotation and maps it to a database table.

---

**Q27. What is an Entity in JPA?**

An entity is a Java class that is mapped to a database table. We mark it with `@Entity`, and JPA uses this to know that this class should be persisted.

```java
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private BigDecimal price;
}
```

Each instance of the `Product` class corresponds to one row in the `products` table. Each field maps to a column. JPA manages the mapping between the object and the relational world.

Rules for an entity class:
- Must be annotated with `@Entity`
- Must have a no-arg constructor (public or protected)
- Must have a field annotated with `@Id`

---

**Q28. What is `EntityManager` in JPA?**

`EntityManager` is the main interface we use to interact with the persistence context. It's like a gateway to the database for JPA operations.

Through `EntityManager` we can:
- `persist(entity)` — save a new entity to the database (INSERT)
- `find(Entity.class, id)` — find an entity by primary key (SELECT)
- `merge(entity)` — update a detached entity (UPDATE)
- `remove(entity)` — delete an entity (DELETE)
- `createQuery(jpql)` — run JPQL queries

```java
@PersistenceContext
private EntityManager entityManager;

public Product findProduct(Long id) {
    return entityManager.find(Product.class, id);
}
```

In modern Spring Data JPA, we rarely use `EntityManager` directly — the repository layer handles it for us. But it's important to understand because Spring Data JPA uses `EntityManager` internally.

---

**Q29. What is `EntityManagerFactory`?**

`EntityManagerFactory` is a factory class that creates `EntityManager` instances. It's a heavyweight object — creating it is expensive because it reads all entity mappings, connects to the database, and prepares Hibernate. So there is **one** `EntityManagerFactory` per application (or per persistence unit).

`EntityManager` is a lightweight object created per request/transaction. We create and close many `EntityManager` instances during the application's lifetime.

```
Application starts → One EntityManagerFactory created
                           │
          ┌────────────────┼───────────────────┐
          ▼                ▼                   ▼
   EntityManager1   EntityManager2      EntityManager3
   (per request)    (per request)       (per request)
```

Spring manages `EntityManagerFactory` for us through `LocalContainerEntityManagerFactoryBean` and injects `EntityManager` per request using `@PersistenceContext`.

---

**Q30. What is the Persistence Context?**

The Persistence Context is like a first-level cache that `EntityManager` maintains. It tracks all entities that have been loaded or saved during the current transaction.

When we load an entity, it goes into the persistence context. Any changes we make to it are tracked automatically (this is called "dirty checking"). When the transaction commits, Hibernate compares the current state of the entity with what it loaded, and if anything changed, it automatically generates an UPDATE SQL — without us calling `save()` explicitly.

```java
@Transactional
public void updateProductName(Long id, String newName) {
    Product product = entityManager.find(Product.class, id);  // loaded into context
    product.setName(newName);  // just modifying the object
    // No save() needed! Hibernate detects the change and runs UPDATE on commit
}
```

The persistence context is scoped to a transaction by default (transaction-scoped persistence context).

---

**Q31. What is a Persistence Unit?**

A persistence unit is a named configuration unit that defines:
- Which entity classes are managed
- The database connection settings (DataSource)
- The JPA provider settings

Traditionally, a persistence unit is defined in `META-INF/persistence.xml`. In Spring Boot, this is replaced by `application.properties` — Spring Boot creates the persistence unit automatically from our configuration.

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=Root123
spring.jpa.hibernate.ddl-auto=update
```

In most Spring Boot projects, we have one persistence unit that handles all our entities.

---

## Spring Data JPA

---

**Q32. What is Spring Data JPA?**

Spring Data JPA is a Spring module that sits on top of JPA and makes data access even easier. Without Spring Data JPA, we'd have to write a lot of `EntityManager` code even for basic CRUD operations.

Spring Data JPA provides the **Repository pattern** — we just define an interface and Spring automatically generates the implementation at runtime. We get all standard CRUD methods for free, plus the ability to define custom queries just by following a naming convention.

```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    // Spring generates the implementation — no code needed!
}
```

This single interface gives us `save()`, `findById()`, `findAll()`, `deleteById()`, and much more without writing a single line of implementation code.

---

**Q33. What is the Repository Pattern?**

The Repository Pattern is a design pattern that abstracts the data access layer. Instead of writing database code directly in service classes, we create a "repository" layer that acts as an in-memory collection of objects.

The benefits are:
- Service classes don't know or care about how data is stored
- We can swap the database or ORM without changing business logic
- Easier to test (we can mock the repository in unit tests)

In Spring Data JPA, `JpaRepository` is the repository interface. Our custom interfaces extend it, and Spring generates the implementation.

```
Service Layer
    │
    │ calls
    ▼
Repository Interface  ←─── Spring generates implementation
    │
    │ delegates to
    ▼
EntityManager / Hibernate / MySQL
```

---

**Q34. What are the Spring Data JPA repository interfaces and their hierarchy?**

Spring Data JPA has a hierarchy of repository interfaces:

```
Repository (marker interface — no methods)
    └── CrudRepository (basic CRUD: save, findById, findAll, delete)
            └── PagingAndSortingRepository (adds pagination and sorting)
                    └── JpaRepository (adds flush, batch operations, JPA-specific methods)
```

We almost always extend `JpaRepository<Entity, IdType>` because it provides the most features:
- `save(entity)` — INSERT or UPDATE
- `findById(id)` — returns `Optional<Entity>`
- `findAll()` — returns all records
- `deleteById(id)` — DELETE by primary key
- `count()` — row count
- `existsById(id)` — check existence
- `findAll(Pageable)` — paginated results

---

**Q35. How do derived query methods work in Spring Data JPA?**

Spring Data JPA can generate SQL queries automatically by parsing method names. This is called derived query methods or query derivation.

```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByName(String name);                          // WHERE name = ?
    List<Product> findByPriceLessThan(BigDecimal price);            // WHERE price < ?
    List<Product> findByNameContainingIgnoreCase(String keyword);   // WHERE LOWER(name) LIKE %keyword%
    List<Product> findByCategoryId(Long categoryId);                // WHERE category_id = ?
    boolean existsByName(String name);                              // SELECT COUNT(*) > 0 WHERE name = ?
    Optional<Product> findByNameAndCategoryId(String name, Long id);
}
```

Spring reads the method name, splits it by keywords (`find`, `By`, `And`, `Or`, `LessThan`, `Containing`, etc.), and generates the corresponding JPQL/SQL. No SQL writing needed!

---

**Q36. What is `@Query` in Spring Data JPA?**

When derived query method names become too long or complex, we use `@Query` to write the query explicitly.

```java
// JPQL (object-oriented query language — uses class and field names, not table/column names)
@Query("SELECT p FROM Product p WHERE p.price BETWEEN :min AND :max")
List<Product> findByPriceRange(@Param("min") BigDecimal min, @Param("max") BigDecimal max);

// Native SQL (actual SQL with table/column names)
@Query(value = "SELECT * FROM products WHERE price < :price", nativeQuery = true)
List<Product> findCheaperThan(@Param("price") BigDecimal price);
```

JPQL is preferred because it's database-independent. Native SQL is used when we need database-specific features or complex queries that are hard to express in JPQL.

---

**Q37. What is `@Transactional` and when do we use it?**

`@Transactional` marks a method (or class) as needing a database transaction. A transaction ensures that a series of database operations either all succeed (commit) or all fail together (rollback), maintaining data consistency.

```java
@Service
@Transactional(readOnly = true)  // class-level: all methods are read-only by default
public class OrderService {

    public List<Order> getAllOrders() { ... }  // uses readOnly transaction

    @Transactional  // overrides class-level: this method needs a write transaction
    public Order createOrder(OrderRequest request) {
        // if this throws an exception, the whole transaction rolls back
        Order order = orderRepository.save(...);
        paymentService.processPayment(...);
        inventoryService.reduceStock(...);
        return order;
    }
}
```

`readOnly = true` is an optimization hint — it tells Hibernate to skip dirty checking (no need to track changes for updates), which improves performance for read operations.

Transactions roll back automatically on unchecked exceptions (`RuntimeException` and its subclasses).

---

**Q38. What are Entity Relationships in JPA?**

JPA supports four types of relationships between entities:

**`@OneToOne`** — One entity is associated with exactly one other entity.
```java
@OneToOne
@JoinColumn(name = "profile_id")
private UserProfile profile;
```

**`@OneToMany` / `@ManyToOne`** — One entity has multiple related entities.
```java
// In Category (the "one" side)
@OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
private List<Product> products;

// In Product (the "many" side — owns the foreign key)
@ManyToOne
@JoinColumn(name = "category_id")
private Category category;
```

**`@ManyToMany`** — Multiple entities can be associated with multiple other entities.
```java
@ManyToMany
@JoinTable(name = "student_courses",
    joinColumns = @JoinColumn(name = "student_id"),
    inverseJoinColumns = @JoinColumn(name = "course_id"))
private List<Course> courses;
```

The `mappedBy` attribute indicates the non-owning side. The owning side (with `@JoinColumn`) holds the foreign key in the database.

---

**Q39. What is the N+1 problem in JPA and how do you solve it?**

The N+1 problem happens when loading a list of entities with a related collection. JPA first runs 1 query to get all parent entities, then runs N more queries (one per parent) to load each child collection — hence "N+1".

Example: Loading 10 categories, each with products → 1 query for categories + 10 queries for each category's products = 11 queries total.

```java
// N+1 problem: for each category, Hibernate runs a separate query for products
List<Category> categories = categoryRepository.findAll();
categories.forEach(c -> System.out.println(c.getProducts().size())); // 10 extra queries!
```

**Solution: JOIN FETCH in JPQL**
```java
@Query("SELECT c FROM Category c JOIN FETCH c.products")
List<Category> findAllWithProducts();
```

This generates a single SQL JOIN query that loads everything at once.

---

## Spring Web & REST APIs

---

**Q40. What is the DispatcherServlet in Spring MVC?**

The `DispatcherServlet` is the heart of Spring MVC. It's a **Front Controller** — a single entry point that receives all incoming HTTP requests and routes them to the appropriate controller.

```
HTTP Request
     │
     ▼
DispatcherServlet  ← single entry point for all requests
     │
     │  1. Consults HandlerMapping → finds the right controller method
     │  2. Calls the controller method → gets the return value
     │  3. Passes return value to Jackson → serializes to JSON
     │
     ▼
HTTP Response (JSON)
```

In Spring Boot, the `DispatcherServlet` is auto-configured by `spring-boot-starter-web`. It's mapped to the root path `/` by default, meaning it handles all incoming requests.

---

**Q41. What is the difference between `@Controller` and `@RestController`?**

Both are used to handle HTTP requests, but they differ in how they handle the return value:

`@Controller` — the return value is treated as a **view name**. Spring looks for a template (like a Thymeleaf `.html` file) with that name and renders it.

`@RestController` — the return value is automatically serialized to **JSON** (or XML) and written directly to the HTTP response body. It's a combination of `@Controller` + `@ResponseBody`.

```java
@Controller
public class PageController {
    @GetMapping("/home")
    public String homePage() {
        return "home";  // looks for templates/home.html
    }
}

@RestController
public class ProductController {
    @GetMapping("/api/products")
    public List<Product> getProducts() {
        return productService.findAll();  // serialized to JSON automatically
    }
}
```

For REST APIs, we always use `@RestController`.

---

**Q42. What is `@RequestMapping` and the HTTP method-specific shorthand annotations?**

`@RequestMapping` is the base annotation for mapping HTTP requests to controller methods. It can specify the URL path, HTTP method, content type, and more.

```java
@RequestMapping(value = "/api/products", method = RequestMethod.GET)
public List<Product> getProducts() { ... }
```

Because specifying `method = RequestMethod.GET` every time is verbose, Spring provides shorthand annotations:

| Annotation | Equivalent to |
|------------|--------------|
| `@GetMapping("/path")` | `@RequestMapping(value="/path", method=GET)` |
| `@PostMapping("/path")` | `@RequestMapping(value="/path", method=POST)` |
| `@PutMapping("/path")` | `@RequestMapping(value="/path", method=PUT)` |
| `@PatchMapping("/path")` | `@RequestMapping(value="/path", method=PATCH)` |
| `@DeleteMapping("/path")` | `@RequestMapping(value="/path", method=DELETE)` |

---

**Q43. What is `@PathVariable` and when do we use it?**

`@PathVariable` is used to extract values from the URL path itself. When part of the URL represents a resource identifier (like an ID), we embed it in the path and extract it with `@PathVariable`.

```java
@GetMapping("/api/products/{id}")
public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
    return ResponseEntity.ok(productService.findById(id));
}
```

A request to `/api/products/42` will have `id = 42` extracted automatically.

We use `@PathVariable` when the value is part of the resource's identity. For example, `/api/orders/123` — the `123` is the order's ID.

---

**Q44. What is `@RequestParam` and when do we use it?**

`@RequestParam` is used to extract query parameters from the URL (the `?key=value` part). We use it for filtering, searching, pagination, and optional criteria.

```java
@GetMapping("/api/products")
public List<ProductResponse> getProducts(
    @RequestParam(required = false) String name,
    @RequestParam(required = false) Long categoryId,
    @RequestParam(defaultValue = "0") int page
) {
    // filter based on provided params
}
```

Request: `/api/products?name=mac&categoryId=1` → `name = "mac"`, `categoryId = 1`

**`@PathVariable` vs `@RequestParam`:**
- `@PathVariable` — for identifying *which* resource (`/products/42`)
- `@RequestParam` — for filtering/modifying *how* to retrieve resources (`/products?name=mac`)

---

**Q45. What is `@RequestBody` and how does it work?**

`@RequestBody` tells Spring to read the HTTP request body and deserialize it from JSON into a Java object. Jackson handles the deserialization automatically.

```java
@PostMapping("/api/products")
public ResponseEntity<ProductResponse> createProduct(@RequestBody @Valid ProductRequest request) {
    ProductResponse created = productService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
}
```

When a client sends:
```json
{ "name": "MacBook Pro", "price": 1999.99, "categoryId": 1 }
```

Jackson reads the JSON and maps it to the `ProductRequest` object. If any field names don't match, they're ignored (unless `@JsonProperty` is used).

We combine `@RequestBody` with `@Valid` to trigger Bean Validation on the incoming data.

---

**Q46. What is `ResponseEntity` and why do we use it?**

`ResponseEntity<T>` is a Spring class that represents the entire HTTP response — status code, headers, and body — giving us complete control over what we send back to the client.

```java
// Just returning an object — Spring defaults to 200 OK
public ProductResponse getProduct() { return product; }

// Using ResponseEntity — we control everything
public ResponseEntity<ProductResponse> getProduct(Long id) {
    ProductResponse product = productService.findById(id);
    return ResponseEntity.ok(product);                    // 200 OK
}

public ResponseEntity<ProductResponse> createProduct(...) {
    ProductResponse created = productService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);  // 201 Created
}

public ResponseEntity<Void> deleteProduct(Long id) {
    productService.delete(id);
    return ResponseEntity.noContent().build();           // 204 No Content
}
```

I always use `ResponseEntity` because returning the correct HTTP status code is important for REST API clients to understand the outcome of their request.

---

**Q47. What is the DTO (Data Transfer Object) pattern?**

A DTO is a simple object used to transfer data between layers of an application or between client and server. Instead of exposing our entity classes directly in the API, we create separate classes for input (Request) and output (Response).

**Why not expose entities directly?**
- Entities may have fields we don't want to expose (passwords, internal IDs)
- Adding Jackson annotations to entities mixes persistence and presentation concerns
- Bidirectional JPA relationships can cause infinite loops during JSON serialization
- API contract should be stable even if the database schema changes

```java
// Request DTO — what the client sends
public class ProductRequest {
    @NotBlank
    private String name;
    @Positive
    private BigDecimal price;
    private Long categoryId;
}

// Response DTO — what we send back
public class ProductResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private String categoryName;  // flattened from the Category entity

    public static ProductResponse from(Product product) {
        // static factory method for clean conversion
    }
}
```

---

**Q48. What are the common HTTP status codes used in REST APIs?**

| Code | Name | When to use |
|------|------|-------------|
| `200 OK` | Success | Successful GET, PUT, PATCH |
| `201 Created` | Resource created | Successful POST |
| `204 No Content` | Success, no body | Successful DELETE |
| `400 Bad Request` | Client error | Validation failure, malformed JSON |
| `401 Unauthorized` | Not authenticated | No token or invalid token |
| `403 Forbidden` | Not authorized | Valid token but insufficient role |
| `404 Not Found` | Resource missing | Entity with given ID doesn't exist |
| `409 Conflict` | Duplicate | Trying to create a resource that already exists |
| `500 Internal Server Error` | Server error | Unhandled exception on the server |

---

**Q49. What is `@RestControllerAdvice` and how does it help with exception handling?**

`@RestControllerAdvice` is used to create a global exception handler — a single class that handles exceptions thrown by any controller in the application.

Without it, if our service throws a `ResourceNotFoundException`, Spring would return a generic 500 error with a stack trace. That's not useful or secure.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex,
                                                         HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
            LocalDateTime.now(), 404, "Not Found", ex.getMessage(), request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
```

Now every `ResourceNotFoundException` thrown anywhere in the app returns a clean, consistent JSON error response with the right status code.

---

**Q50. What is Bean Validation and how do we use it in Spring Boot?**

Bean Validation (JSR-380) is a Java standard for declaring validation rules on object fields using annotations. Hibernate Validator is the implementation.

We add `spring-boot-starter-validation` as a dependency, annotate our DTO fields, and use `@Valid` in the controller to trigger validation:

```java
public class ProductRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;

    @Positive(message = "Price must be positive")
    private BigDecimal price;

    @PositiveOrZero
    private int stockQuantity;
}

@PostMapping
public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
    ...
}
```

If validation fails, Spring throws `MethodArgumentNotValidException`, which our `@RestControllerAdvice` catches and returns a `400 Bad Request` with field-level error messages.

---

## Mixed / Advanced

---

**Q51. What is the difference between `@Component` scan and explicit bean definition?**

`@ComponentScan` + stereotype annotations (`@Service`, `@Repository`, etc.) is an automatic approach — Spring scans packages and auto-detects classes to register as beans. This is concise and works great for classes we own.

Explicit bean definition with `@Bean` inside `@Configuration` is a manual approach where we write a method that creates and returns the bean. This is necessary for:
- Third-party classes we can't annotate (e.g., `DataSource`, `RestTemplate`)
- Beans that need complex construction logic
- When we need to control which implementation of an interface is registered

Both approaches can coexist in the same application.

---

**Q52. What is `@Value` in Spring and how do we use it?**

`@Value` injects values from `application.properties` or environment variables directly into fields.

```java
@Service
public class EmailService {
    @Value("${app.smtp.host}")
    private String smtpHost;

    @Value("${app.smtp.port:587}")  // 587 is the default if the property is missing
    private int smtpPort;
}
```

`application.properties`:
```properties
app.smtp.host=smtp.gmail.com
app.smtp.port=465
```

For more complex configuration objects, we prefer `@ConfigurationProperties` which binds an entire group of properties to a POJO. But for simple single-value injections, `@Value` is clean and convenient.

---

**Q53. What is the difference between `spring.jpa.hibernate.ddl-auto` values?**

This property controls how Hibernate manages the database schema at startup:

| Value | Behavior |
|-------|----------|
| `none` | Don't touch the schema |
| `validate` | Validate schema matches entities; throw error if not |
| `update` | Add missing tables/columns; never drop anything |
| `create` | Drop and recreate schema on every start (lose all data) |
| `create-drop` | Create on start, drop on shutdown |

For development I use `update` — it adds new tables/columns as I add entities. For production I use `none` or `validate` and manage schema changes with a migration tool like Flyway or Liquibase.

---

**Q54. What is lazy loading vs eager loading in JPA?**

These control *when* related entities are loaded from the database.

**Lazy loading** (default for `@OneToMany`, `@ManyToMany`) — the related collection is NOT loaded when the parent is loaded. It's loaded only when you actually access it.

**Eager loading** (default for `@ManyToOne`, `@OneToOne`) — the related entity IS loaded immediately when the parent is loaded, always with a JOIN.

```java
@OneToMany(fetch = FetchType.LAZY)   // products loaded only when accessed
private List<Product> products;

@ManyToOne(fetch = FetchType.EAGER)  // category always loaded with product
private Category category;
```

Lazy loading is generally preferred for collections because loading everything upfront can be expensive. However, accessing a lazy collection outside a transaction (e.g., in a controller after the transaction closed) causes a `LazyInitializationException`.

---

**Q55. What is the difference between `save()` and `saveAndFlush()` in Spring Data JPA?**

`save()` — saves the entity to the persistence context (memory). The actual SQL INSERT/UPDATE is executed when the transaction commits (or when Hibernate decides to flush).

`saveAndFlush()` — saves to the persistence context AND immediately flushes to the database (executes the SQL right now), within the same transaction.

We use `saveAndFlush()` when we need the database to reflect the changes immediately — for example, if a subsequent query in the same transaction needs to see the saved data.

For most cases, `save()` is sufficient and more efficient because Hibernate can batch SQL statements together.

---

**Q56. How does Spring Boot know which auto-configurations to apply?**

Spring Boot uses `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (in Spring Boot 3.x) to list all available auto-configuration classes. Each class has conditions:

- `@ConditionalOnClass` — only apply if a specific class is on the classpath
- `@ConditionalOnMissingBean` — only apply if we haven't defined our own bean
- `@ConditionalOnProperty` — only apply if a specific property is set

For example, `DataSourceAutoConfiguration`:
```java
@AutoConfiguration
@ConditionalOnClass({ DataSource.class, EmbeddedDatabaseType.class })
@ConditionalOnMissingBean(type = { "io.r2dbc.spi.ConnectionFactory" })
public class DataSourceAutoConfiguration { ... }
```

This means: "Auto-configure a DataSource only if DataSource class is on the classpath AND we haven't defined our own." Since we add `spring-boot-starter-data-jpa`, `DataSource` is on the classpath, so auto-configuration kicks in and creates a `DataSource` from our `application.properties`.

---

**Q57. What is `@GeneratedValue` and what are the generation strategies?**

`@GeneratedValue` tells JPA to automatically generate the primary key value. There are four strategies:

| Strategy | Behavior |
|----------|----------|
| `IDENTITY` | Database auto-increment (MySQL `AUTO_INCREMENT`). Most common for MySQL. |
| `SEQUENCE` | Uses a database sequence object. Common for PostgreSQL, Oracle. |
| `TABLE` | Uses a special table to simulate sequences. Portable but slow. |
| `AUTO` | JPA picks the best strategy based on the database. |

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

For MySQL, I always use `IDENTITY` because MySQL natively supports `AUTO_INCREMENT` columns.

---

**Q58. What happens when we add `spring-boot-starter-web` to the project?**

When we add `spring-boot-starter-web`, Spring Boot auto-configuration does all of this automatically:

1. Registers the `DispatcherServlet` mapped to `/`
2. Starts an **embedded Tomcat** server on port 8080 (configurable via `server.port`)
3. Configures **Jackson** for JSON serialization/deserialization
4. Sets up `HandlerMapping` for routing requests to `@Controller` methods
5. Configures `ContentNegotiationManager` for content type handling
6. Sets up `ExceptionHandlerExceptionResolver` for `@ExceptionHandler` support

Before Spring Boot, all of this required dozens of lines of XML or Java configuration. Now it's automatic as soon as we add the dependency.

---

**Q59. What is JPQL and how is it different from SQL?**

JPQL (Java Persistence Query Language) is an object-oriented query language that works with entity objects and their fields, not database tables and columns.

```sql
-- SQL (works with tables/columns)
SELECT p.product_name, c.category_name
FROM products p
JOIN categories c ON p.category_id = c.id
WHERE p.price > 100

-- JPQL (works with classes/fields)
SELECT p FROM Product p JOIN p.category c WHERE p.price > 100
```

Key differences:
- JPQL uses **entity class names** (not table names)
- JPQL uses **field names** (not column names)
- JPQL is **database-independent** — works with any database
- No `SELECT *` in JPQL; you select entities or specific fields

Hibernate translates JPQL to the appropriate SQL dialect for the database being used.

---

**Q60. What is the purpose of `@Column`, `@Table`, and `@JoinColumn` annotations?**

These annotations give us fine-grained control over how the entity maps to the database schema.

**`@Table`** — customizes the database table name and can add unique constraints:
```java
@Entity
@Table(name = "products", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class Product { ... }
```

**`@Column`** — customizes the column mapping:
```java
@Column(name = "product_name", nullable = false, length = 100)
private String name;

@Column(precision = 10, scale = 2)  // for BigDecimal: 10 total digits, 2 decimal places
private BigDecimal price;
```

**`@JoinColumn`** — customizes the foreign key column in a relationship:
```java
@ManyToOne
@JoinColumn(name = "category_id", nullable = false)  // FK column name in products table
private Category category;
```

Without these annotations, JPA uses defaults — field name as column name, class name as table name. These annotations are used when we need to match an existing schema or follow specific naming conventions.

---

*End of 60 Questions — Good luck with your interview!*
