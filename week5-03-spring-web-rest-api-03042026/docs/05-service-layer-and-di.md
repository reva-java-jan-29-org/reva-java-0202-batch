# Module 05 — Service Layer & Dependency Injection

## Why a Service Layer?

A common question beginners ask: "Why can't the controller just call the repository directly?"

```java
// ❌ Controller calling repository directly — anti-pattern
@PostMapping
public ResponseEntity<Product> create(@RequestBody Product product) {
    Product saved = productRepository.save(product);  // ← repository in controller!
    return ResponseEntity.status(201).body(saved);
}
```

Problems with this approach:

| Problem | Explanation |
|---------|-------------|
| **No separation of concerns** | Controller (HTTP layer) is now doing business logic |
| **Hard to test** | You need to mock the repository AND test HTTP routing at the same time |
| **No reusability** | If another part of the app needs the same business logic, you'd duplicate code |
| **No transaction management** | Where does `@Transactional` go? |
| **Business logic scattered** | Price calculations, validation rules, etc. live in the controller |

### The Three-Layer Architecture

```
┌──────────────────────────────────┐
│  Controller Layer (@RestController) │
│  - Handles HTTP routing             │
│  - Extracts path/query params       │
│  - Calls service                    │
│  - Returns ResponseEntity           │
└────────────────┬─────────────────┘
                 │ calls
┌────────────────▼─────────────────┐
│  Service Layer (@Service)         │
│  - Business logic                 │
│  - Transaction management         │
│  - Calls multiple repositories    │
│  - Maps entities ↔ DTOs          │
└────────────────┬─────────────────┘
                 │ calls
┌────────────────▼─────────────────┐
│  Repository Layer (@Repository)   │
│  - Data access                    │
│  - SQL queries / JPA operations   │
│  - No business logic              │
└────────────────┬─────────────────┘
                 │ reads/writes
┌────────────────▼─────────────────┐
│  Database (MySQL)                 │
└──────────────────────────────────┘
```

---

## @Service Annotation

`@Service` is a **stereotype annotation** — it marks a class as a service layer bean.

Technically, `@Service` is just `@Component` with a different name. Spring scans for it and registers the class as a bean in the application context.

```java
@Service          // Spring detects this and creates a singleton bean
public class ProductService {
    ...
}
```

Other stereotype annotations:
- `@Component` — generic Spring bean
- `@Service` — business logic
- `@Repository` — data access
- `@Controller` / `@RestController` — web layer

They all behave the same way — the names are for **documentation clarity**, not technical difference.

---

## Dependency Injection

**Dependency Injection (DI)** means Spring creates your objects and wires their dependencies for you.

Without DI (manual wiring — bad):
```java
public class ProductController {
    private ProductService productService = new ProductService(
        new ProductRepository(),   // ← you create every dependency yourself
        new CategoryRepository()
    );
}
```

With DI (Spring handles it):
```java
@RestController
public class ProductController {
    private final ProductService productService;

    // Spring sees this constructor and injects the ProductService bean automatically
    public ProductController(ProductService productService) {
        this.productService = productService;
    }
}
```

### Three Ways to Inject Dependencies

#### 1. Constructor Injection (Recommended ✅)

```java
@Service
@RequiredArgsConstructor  // Lombok generates this constructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
}
```

With Lombok's `@RequiredArgsConstructor`:
- Generates a constructor for all `final` fields
- Spring sees the constructor and injects the dependencies

**Why constructor injection is preferred:**
- Dependencies are `final` (immutable, always set)
- Easy to unit test (pass mocks in the constructor)
- Makes dependencies explicit (you can see them at a glance)
- No "dependency not injected yet" surprises

#### 2. Field Injection (Common but not recommended ❌)

```java
@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;
}
```

Problems:
- Fields can't be `final` (not immutable)
- Harder to test (need Spring's `@MockBean` or reflection)
- Hidden dependencies (no visible constructor)

#### 3. Setter Injection (Rarely used)

```java
@Service
public class ProductService {
    private ProductRepository productRepository;

    @Autowired
    public void setProductRepository(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
}
```

Use only when the dependency is truly optional.

---

## @Transactional — Transaction Management

A **transaction** is a unit of work that either completes entirely or not at all.

```
Transfer money:
  1. Debit account A   ← if this succeeds but step 2 fails...
  2. Credit account B  ← ...money disappears!

Solution: wrap in a transaction:
  Either BOTH steps succeed (COMMIT)
  Or     BOTH steps are rolled back (ROLLBACK)
```

### @Transactional in Spring

```java
@Service
@Transactional(readOnly = true)  // default for all methods in this class
public class ProductService {

    public List<ProductResponse> findAll() {  // uses readOnly transaction
        return productRepository.findAll()...
    }

    @Transactional  // overrides class-level — uses full read-write transaction
    public ProductResponse create(ProductRequest request) {
        // If anything here throws an exception → transaction ROLLS BACK
        // If all succeeds → transaction COMMITS (data saved to DB)
        Category category = categoryRepository.findById(request.getCategoryId())...
        Product product = ...
        Product saved = productRepository.save(product);
        return ProductResponse.from(saved);
    }
}
```

### readOnly = true

```java
@Transactional(readOnly = true)
public List<ProductResponse> findAll() { ... }
```

Benefits of `readOnly = true`:
- Tells Hibernate: don't track changes to entities (no dirty-checking)
- Some databases/drivers optimize read-only transactions (e.g., routing to read replicas)
- Performance improvement for query-heavy operations

### When transactions roll back

By default, Spring rolls back only on **unchecked exceptions** (RuntimeException and its subclasses).

```java
// This WILL cause rollback (RuntimeException):
throw new ResourceNotFoundException("Product not found");
throw new IllegalArgumentException("Invalid input");

// This will NOT cause rollback by default (checked exception):
throw new IOException("File not found");  // ← won't rollback!

// To rollback on checked exceptions too:
@Transactional(rollbackFor = Exception.class)
```

---

## Cross-Entity Operations in Services

Services can interact with multiple repositories. This is one of the key reasons the service layer exists.

```java
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;     // ← two repositories
    private final CategoryRepository categoryRepository;   // ← in one service

    @Transactional
    public ProductResponse create(ProductRequest request) {
        // Step 1: Verify the category exists
        Category category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        // Step 2: Build the product entity
        Product product = Product.builder()
            .name(request.getName())
            .price(request.getPrice())
            .category(category)     // ← set the FK relationship
            .build();

        // Step 3: Save the product
        Product saved = productRepository.save(product);

        // Step 4: Map to DTO and return
        return ProductResponse.from(saved);
    }
}
```

If `categoryRepository.findById()` throws, the product is never saved. If `productRepository.save()` throws, the whole transaction rolls back. This is the power of `@Transactional`.

---

## Unit Testing the Service Layer

Because services take their dependencies through the constructor, you can easily unit-test them with Mockito:

```java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void findById_shouldReturnProduct_whenExists() {
        // Arrange
        Category cat = Category.builder().id(1L).name("Electronics").build();
        Product product = Product.builder().id(1L).name("Laptop").category(cat).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // Act
        ProductResponse response = productService.findById(1L);

        // Assert
        assertThat(response.getName()).isEqualTo("Laptop");
        assertThat(response.getCategoryName()).isEqualTo("Electronics");
    }

    @Test
    void findById_shouldThrowException_whenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }
}
```

Notice: no HTTP, no Spring context, no database — pure unit test running in milliseconds.
