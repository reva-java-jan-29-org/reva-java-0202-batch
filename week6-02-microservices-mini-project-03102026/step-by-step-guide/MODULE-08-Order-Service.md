# Module 8 — Order Service: Cart Management, OpenFeign & Order Placement

**Type:** Hands-on Implementation
**Duration:** ~3 hours
**Prerequisites:** All previous modules complete — Config Server, Eureka, User Service, Product Service, and API Gateway all running
**Goal:** Build the Order Service — the most complex service in the system — which manages shopping carts, communicates with Product Service via OpenFeign, and converts carts into orders with stock reduction.

---

## Learning Objectives

By the end of this module you will be able to:

1. Design a `Cart`/`CartItem` entity pair with a `@OneToMany` / `@ManyToOne` bidirectional relationship
2. Explain `CascadeType.ALL` and `orphanRemoval = true` for parent-child entity management
3. Implement **OpenFeign** — a declarative HTTP client for service-to-service calls
4. Read injected gateway headers (`X-User-Id`) in a controller using `@RequestHeader`
5. Implement a `@Transactional` order placement method that calls Feign, saves an Order, and clears the Cart
6. Validate the full shopping flow end-to-end using Postman: login → add to cart → place order → check order history

---

## Recap — Where We Are

```
✅ Config Server    :8888
✅ Eureka Server    :8761
✅ User Service     :8081
✅ Product Service  :8082
✅ API Gateway      :8080
⬜ Order Service    :8083  ← This module
```

---

## 8.1 Where Order Service Fits

```
Postman (authenticated — Bearer token)
      │
      ▼
API Gateway :8080
      │  validates JWT, injects X-User-Id header
      │
      ├─ /api/cart/**   → lb://order-service :8083
      └─ /api/orders/** → lb://order-service :8083
                │
                │  OpenFeign (internal, no JWT)
                ▼
         Product Service :8082  (GET /api/products/{id}, PUT .../reduce-stock)
                │
                ▼
           product_db (MySQL)

Order Service reads:
  order_db (MySQL) → carts, cart_items, orders, order_items tables
```

The Order Service:
- **Reads** product data from Product Service via Feign (price, name, stock)
- **Reduces** product stock via Feign when an order is placed
- **Owns** cart and order data in its own `order_db`
- **Identifies** the caller using the `X-User-Id` header injected by the gateway

---

## 8.2 Maven Dependencies — `order-service/pom.xml`

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

    <artifactId>order-service</artifactId>
    <name>Order Service</name>

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

        <!-- OpenFeign — declarative HTTP client for Product Service calls -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>

        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

**New dependency — `spring-cloud-starter-openfeign`:**
OpenFeign generates a full HTTP client from a Java interface. You declare the API contract, annotate it with `@FeignClient`, and Spring generates the implementation at startup.

---

## 8.3 Configuration

### `order-service/src/main/resources/application.yml`

```yaml
server:
  port: 8083

spring:
  application:
    name: order-service
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

### `config-repo/order-service.properties`

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/order_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=Root123
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

No `jwt.secret` here — the Order Service does not validate JWTs. It trusts the `X-User-Id` header injected by the gateway.

---

## 8.4 Main Application Class

```java
// order-service/src/main/java/com/ecommerce/order/OrderServiceApplication.java
package com.ecommerce.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients                    // ← enables OpenFeign client scanning
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

`@EnableFeignClients` tells Spring Boot to scan for interfaces annotated with `@FeignClient` and generate proxy implementations at startup.

---

## 8.5 Entity Layer

### 8.5.1 `Cart.java` + `CartItem.java`

```java
// order-service/src/main/java/com/ecommerce/order/entity/Cart.java
package com.ecommerce.order.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;                // from X-User-Id header (no FK to user_db)

    @OneToMany(mappedBy = "cart",
               cascade = CascadeType.ALL,
               fetch = FetchType.EAGER,
               orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and setters omitted for brevity
}
```

```java
// order-service/src/main/java/com/ecommerce/order/entity/CartItem.java
package com.ecommerce.order.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;                  // FK → carts.id

    @Column(nullable = false)
    private Long productId;             // snapshot — no FK to product_db

    @Column(nullable = false)
    private String productName;         // snapshot of product name at add time

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;           // snapshot of price at add time

    @Column(nullable = false)
    private Integer quantity;

    // Getters and setters omitted for brevity
}
```

**`@OneToMany` / `@ManyToOne` relationship:**

```
carts table:           cart_items table:
┌──────────┐           ┌───────────────────────┐
│ id  │ ...│    1:N    │ id │ cart_id │ price  │...
│  1  │    │ ◄────────►│ 1  │    1    │ 49.99  │
│  2  │    │           │ 2  │    1    │ 249.99 │
└──────────┘           │ 3  │    2    │ 89.99  │
                       └───────────────────────┘
```

**`CascadeType.ALL`** — any operation on `Cart` (save, delete) cascades to its `CartItem`s. Delete a cart → all its items are deleted.

**`orphanRemoval = true`** — if you remove a `CartItem` from the `cart.items` list (e.g. `cart.getItems().removeIf(...)`) and save the cart, the orphaned item row is automatically deleted. Without this, you would need an explicit `cartItemRepository.delete(item)`.

**`FetchType.EAGER`** on Cart's items — items are loaded immediately when the cart is loaded. This avoids `LazyInitializationException` when accessing items outside a transaction. For a cart service, this is acceptable because you always need the items.

**Why store `productName` and `price` as snapshots?**

`CartItem` stores a copy of the product's name and price at the time of adding. This is intentional:
- Product prices can change — your cart should show the price you saw
- Products could be deleted — your cart history should still be readable
- Cross-service data integrity: `order_db` has no foreign key to `product_db`

---

### 8.5.2 `Order.java` + `OrderItem.java`

```java
// order-service/src/main/java/com/ecommerce/order/entity/Order.java
package com.ecommerce.order.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    public enum Status { PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    private String shippingAddress;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and setters omitted for brevity
}
```

```java
// order-service/src/main/java/com/ecommerce/order/entity/OrderItem.java
// Same structure as CartItem but mapped to "order_items" with FK → orders.id
```

**Database tables created by Hibernate (`ddl-auto=update`):**

```
carts:       id, userId, created_at
cart_items:  id, cart_id (FK), productId, productName, price, quantity
orders:      id, userId, totalAmount, status, shippingAddress, created_at
order_items: id, order_id (FK), productId, productName, price, quantity
```

---

## 8.6 OpenFeign — `ProductServiceClient.java`

```java
// order-service/src/main/java/com/ecommerce/order/feign/ProductServiceClient.java
package com.ecommerce.order.feign;

import com.ecommerce.order.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service", url = "${product.service.url:}")
public interface ProductServiceClient {

    @GetMapping("/api/products/{id}")
    ProductDto getProductById(@PathVariable("id") Long id);

    @PutMapping("/api/products/{id}/reduce-stock")
    ProductDto reduceStock(@PathVariable("id") Long id, @RequestParam("quantity") int quantity);
}
```

**How OpenFeign works:**

```
Order Service code calls:
    productServiceClient.getProductById(1L)
            │
            ▼
Feign generates HTTP request:
    GET http://product-service/api/products/1
            │
            ▼
Eureka resolves "product-service" → 127.0.0.1:8082
            │
            ▼
    GET http://127.0.0.1:8082/api/products/1
            │
            ▼
ProductController returns JSON
            │
            ▼
Feign deserializes JSON → ProductDto
            │
            ▼
Your code receives: ProductDto { id=1, name="Laptop Pro 15", price=1299.99, ... }
```

**`@FeignClient` attributes:**
- `name = "product-service"` — the Eureka service name; used for Eureka-based load balancing
- `url = "${product.service.url:}"` — optional override; empty string means "use Eureka". Set this property to a direct URL (e.g. `http://localhost:8082`) to bypass Eureka in local testing.

**The local `ProductDto` in Order Service:**

```java
// order-service/src/main/java/com/ecommerce/order/dto/ProductDto.java
// A local copy — only the fields Order Service needs
public class ProductDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stock;
    // getters and setters
}
```

Each service defines its own DTOs for external service responses. You don't share the Product Service's entity classes across services — that would create compile-time coupling between services.

---

## 8.7 Repositories

```java
// CartRepository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);
}

// OrderRepository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
}
```

`findByUserIdOrderByCreatedAtDesc` is a derived query that Spring Data JPA parses as:
```sql
SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC
```

---

## 8.8 `CartService.java`

```java
// order-service/src/main/java/com/ecommerce/order/service/CartService.java
package com.ecommerce.order.service;

import com.ecommerce.order.dto.*;
import com.ecommerce.order.entity.Cart;
import com.ecommerce.order.entity.CartItem;
import com.ecommerce.order.feign.ProductServiceClient;
import com.ecommerce.order.repository.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductServiceClient productServiceClient;

    public CartDto getCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createNewCart(userId));
        return toDto(cart);
    }

    public CartDto addToCart(Long userId, CartItemRequest request) {
        // ① Fetch product info via OpenFeign (validates product exists)
        ProductDto product = productServiceClient.getProductById(request.getProductId());

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createNewCart(userId));

        // ② Check if product already in cart — increment if so
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(
                existingItem.get().getQuantity() + request.getQuantity()
            );
        } else {
            // ③ Add new CartItem with snapshotted product data
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductId(product.getId());
            newItem.setProductName(product.getName());
            newItem.setPrice(product.getPrice());
            newItem.setQuantity(request.getQuantity());
            cart.getItems().add(newItem);
        }

        return toDto(cartRepository.save(cart));
    }

    public CartDto removeFromCart(Long userId, Long itemId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));
        cart.getItems().removeIf(item -> item.getId().equals(itemId));
        return toDto(cartRepository.save(cart));   // orphanRemoval deletes the item row
    }

    public CartDto updateCartItem(Long userId, Long itemId, int quantity) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));
        cart.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .ifPresent(item -> item.setQuantity(quantity));
        return toDto(cartRepository.save(cart));
    }

    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart != null) {
            cart.getItems().clear();
            cartRepository.save(cart);
        }
    }

    private Cart createNewCart(Long userId) {
        Cart cart = new Cart();
        cart.setUserId(userId);
        return cartRepository.save(cart);
    }

    private CartDto toDto(Cart cart) {
        CartDto dto = new CartDto();
        dto.setId(cart.getId());
        dto.setUserId(cart.getUserId());
        List<CartItemDto> itemDtos = cart.getItems().stream()
                .map(this::toItemDto)
                .collect(Collectors.toList());
        dto.setItems(itemDtos);
        BigDecimal total = itemDtos.stream()
                .map(CartItemDto::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setTotalAmount(total);
        return dto;
    }

    private CartItemDto toItemDto(CartItem item) {
        CartItemDto dto = new CartItemDto();
        dto.setId(item.getId());
        dto.setProductId(item.getProductId());
        dto.setProductName(item.getProductName());
        dto.setPrice(item.getPrice());
        dto.setQuantity(item.getQuantity());
        dto.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        return dto;
    }
}
```

**`addToCart` flow:**

```
addToCart(userId=1, {productId=2, quantity=1})
    │
    ├─ Feign GET /api/products/2 → ProductDto {name="Wireless Headphones", price=249.99}
    │
    ├─ findByUserId(1) → existing cart OR create new cart
    │
    ├─ Is productId=2 already in cart?
    │   ├─ YES → increment quantity
    │   └─ NO  → create CartItem with snapshotted name + price
    │
    └─ save(cart) → INSERT/UPDATE cart_items
       return CartDto with updated items + recalculated total
```

**`reduce` via `BigDecimal.add()`:**

```java
BigDecimal total = itemDtos.stream()
        .map(CartItemDto::getSubtotal)       // item.price * item.quantity
        .reduce(BigDecimal.ZERO, BigDecimal::add);  // sum all subtotals
```

`BigDecimal::add` is a method reference for `BigDecimal.add(BigDecimal)`. The `reduce` operation starts from `BigDecimal.ZERO` and adds each subtotal — equivalent to summing a list.

---

## 8.9 `OrderService.java`

```java
// order-service/src/main/java/com/ecommerce/order/service/OrderService.java
package com.ecommerce.order.service;

import com.ecommerce.order.dto.*;
import com.ecommerce.order.entity.Cart;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.feign.ProductServiceClient;
import com.ecommerce.order.repository.CartRepository;
import com.ecommerce.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private CartService cartService;
    @Autowired private ProductServiceClient productServiceClient;

    @Transactional
    public OrderDto placeOrder(Long userId, PlaceOrderRequest request) {

        // ① Load cart — fail if empty
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cannot place order with empty cart");
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setShippingAddress(request.getShippingAddress());

        BigDecimal total = BigDecimal.ZERO;

        for (var cartItem : cart.getItems()) {

            // ② Reduce product stock via Feign (atomic check + decrement in Product Service)
            productServiceClient.reduceStock(cartItem.getProductId(), cartItem.getQuantity());

            // ③ Copy cart item to order item (snapshot the data)
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setProductName(cartItem.getProductName());
            orderItem.setPrice(cartItem.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            order.getItems().add(orderItem);

            total = total.add(
                cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()))
            );
        }

        order.setTotalAmount(total);

        // ④ Save order (cascades to order_items)
        Order savedOrder = orderRepository.save(order);

        // ⑤ Clear the cart after order placed
        cartService.clearCart(userId);

        return toDto(savedOrder);
    }

    public List<OrderDto> getUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public OrderDto getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }
        return toDto(order);
    }

    // toDto() method maps Order + OrderItem → OrderDto + CartItemDto
}
```

**`@Transactional` — why it matters for order placement:**

`@Transactional` wraps the entire `placeOrder()` method in a single database transaction. If any step fails (e.g. Feign call throws, or `orderRepository.save()` fails), the entire transaction rolls back — no partial orders, no stock reduced without an order record.

```
@Transactional placeOrder():
    ✓ reduceStock(product 2, qty 1) → Product Service stock: 100 → 99
    ✓ reduceStock(product 6, qty 2) → Product Service stock: 30 → 28
    ✗ orderRepository.save() fails (disk full)
    → Transaction ROLLS BACK
    → BUT: Feign calls already happened — Product Service already reduced stock!
```

> **Production consideration:** The Feign calls to reduce stock happen *outside* the Order Service's transaction boundary (they're remote HTTP calls, not database operations). A proper solution uses the **Saga pattern** (compensating transactions) or **outbox pattern**. For this training project, failures are treated as exceptional and not compensated.

---

## 8.10 Controllers

### 8.10.1 `CartController.java`

```java
// order-service/src/main/java/com/ecommerce/order/controller/CartController.java
package com.ecommerce.order.controller;

import com.ecommerce.order.dto.CartDto;
import com.ecommerce.order.dto.CartItemRequest;
import com.ecommerce.order.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping
    public ResponseEntity<CartDto> getCart(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody CartItemRequest request) {
        try {
            return ResponseEntity.ok(cartService.addToCart(userId, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartDto> removeFromCart(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeFromCart(userId, itemId));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartDto> updateCartItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long itemId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(cartService.updateCartItem(userId, itemId, quantity));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(@RequestHeader("X-User-Id") Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
```

**`@RequestHeader("X-User-Id")` — how the caller identity flows in:**

```
Postman → GET /api/cart (Authorization: Bearer eyJ...)
        ↓
    API Gateway
        - validates JWT
        - extracts userId=1 from token claims
        - adds header: X-User-Id: 1
        - forwards to order-service
        ↓
    CartController.getCart(@RequestHeader("X-User-Id") Long userId)
        userId = 1  ← Spring extracts from the injected header
```

The controller never sees the JWT token. It only sees the `X-User-Id` header that the gateway injected. This is the **gateway-first security pattern** in action.

---

### 8.10.2 `OrderController.java`

```java
// order-service/src/main/java/com/ecommerce/order/controller/OrderController.java
package com.ecommerce.order.controller;

import com.ecommerce.order.dto.OrderDto;
import com.ecommerce.order.dto.PlaceOrderRequest;
import com.ecommerce.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<?> placeOrder(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody PlaceOrderRequest request) {
        try {
            OrderDto order = orderService.placeOrder(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);  // 201
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> getMyOrders(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(orderService.getUserOrders(userId));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderById(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long orderId) {
        try {
            return ResponseEntity.ok(orderService.getOrderById(orderId, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                Map.of("error", e.getMessage())
            );
        }
    }
}
```

**REST design decisions:**

| Endpoint | Method | Status codes | Notes |
|---|---|---|---|
| Place order | POST `/api/orders` | 201 / 400 | 201 = resource created; 400 = empty cart / stock issue |
| My orders | GET `/api/orders` | 200 | Returns user's orders, newest first |
| Order detail | GET `/api/orders/{id}` | 200 / 403 | 403 if order belongs to a different user |

**Why 403 (Forbidden) for wrong-user order access, not 404?**

Returning 404 would leak information: "this order ID exists but you can't see it." 403 is more correct — you are authenticated but not authorized for this specific resource. In production, 404 is often used to prevent information disclosure, but 403 is technically more accurate.

---

## 8.11 Security Configuration

```java
// order-service/src/main/java/com/ecommerce/order/config/SecurityConfig.java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
        return http.build();
    }
}
```

Like Product Service, Order Service permits all requests internally — the Gateway has already authenticated the caller. The `X-User-Id` header is trusted because in production, the Order Service is only reachable from inside the cluster.

---

## 8.12 Package Structure

```
order-service/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── com/ecommerce/order/
        │       ├── OrderServiceApplication.java       ← @EnableFeignClients
        │       ├── config/
        │       │   └── SecurityConfig.java            ← permitAll
        │       ├── controller/
        │       │   ├── CartController.java            ← /api/cart
        │       │   └── OrderController.java           ← /api/orders
        │       ├── dto/
        │       │   ├── CartDto.java
        │       │   ├── CartItemDto.java
        │       │   ├── CartItemRequest.java
        │       │   ├── OrderDto.java
        │       │   ├── PlaceOrderRequest.java
        │       │   └── ProductDto.java               ← local copy for Feign response
        │       ├── entity/
        │       │   ├── Cart.java
        │       │   ├── CartItem.java
        │       │   ├── Order.java
        │       │   └── OrderItem.java
        │       ├── feign/
        │       │   └── ProductServiceClient.java     ← OpenFeign interface
        │       ├── repository/
        │       │   ├── CartRepository.java
        │       │   ├── CartItemRepository.java
        │       │   └── OrderRepository.java
        │       └── service/
        │           ├── CartService.java
        │           └── OrderService.java
        └── resources/
            └── application.yml
```

---

## 8.13 Startup & Validation

### Step 1 — Start Order Service

```bash
cd order-service
mvn spring-boot:run
```

Watch for:
```
Started OrderServiceApplication in X.XXX seconds
Registering application ORDER-SERVICE with eureka
```

### Step 2 — Verify Eureka

All 5 services should now appear in the Eureka dashboard at `http://localhost:8761`:
- CONFIG-SERVER
- USER-SERVICE
- PRODUCT-SERVICE
- API-GATEWAY
- ORDER-SERVICE

---

## 8.14 Postman Validation — Full Shopping Flow

> **Prerequisites:** Your Postman collection variable `{{baseUrl}}` = `http://localhost:8080` (API Gateway)

---

### Step 1: Login to get a token

**Request:** `POST {{baseUrl}}/api/auth/login`

**Body:**
```json
{
  "username": "alice",
  "password": "password123"
}
```

**Expected:** `200 OK` — token saved to `{{token}}` automatically by the test script.

---

### Step 2: View your cart (should be empty)

**Request:** `GET {{baseUrl}}/api/cart`

**Headers:** `Authorization: Bearer {{token}}`

**Expected response (`200 OK`):**
```json
{
  "id": 1,
  "userId": 1,
  "items": [],
  "totalAmount": 0
}
```

A new empty cart is created automatically on first access. Note the `userId` matches the `id` from the `customers` table.

---

### Step 3: Add a product to cart

**Request:** `POST {{baseUrl}}/api/cart/add`

**Headers:** `Authorization: Bearer {{token}}`

**Body:**
```json
{
  "productId": 1,
  "quantity": 2
}
```

**Expected response (`200 OK`):**
```json
{
  "id": 1,
  "userId": 1,
  "items": [
    {
      "id": 1,
      "productId": 1,
      "productName": "Laptop Pro 15",
      "price": 1299.99,
      "quantity": 2,
      "subtotal": 2599.98
    }
  ],
  "totalAmount": 2599.98
}
```

Note the item id (e.g. `1`) — you will need it for the remove/update requests. Save it to `{{cartItemId}}`.

---

### Step 4: Add another product to cart

**Request:** `POST {{baseUrl}}/api/cart/add`

**Body:**
```json
{
  "productId": 2,
  "quantity": 1
}
```

**Expected:** Cart now has 2 items. `totalAmount` = 2599.98 + 249.99 = 2849.97.

---

### Step 5: Update cart item quantity

**Request:** `PUT {{baseUrl}}/api/cart/items/{{cartItemId}}?quantity=1`

**Headers:** `Authorization: Bearer {{token}}`

**Expected:** First item quantity updated to 1. New total = 1299.99 + 249.99 = 1549.98.

---

### Step 6: Add same product again (should increment quantity)

**Request:** `POST {{baseUrl}}/api/cart/add` with `{"productId": 1, "quantity": 1}`

**Expected:** First item quantity is now 2 (1 + 1). Total = 2599.98 + 249.99 = 2849.97.

---

### Step 7: Check product stock before ordering

**Request:** `GET {{baseUrl}}/api/products/1`

Note the current `stock` value (should be 50 for Laptop Pro 15).

---

### Step 8: Place the order

**Request:** `POST {{baseUrl}}/api/orders`

**Headers:** `Authorization: Bearer {{token}}`

**Body:**
```json
{
  "shippingAddress": "42 Tech Street, Sydney NSW 2000"
}
```

**Expected response (`201 Created`):**
```json
{
  "id": 1,
  "userId": 1,
  "items": [
    {
      "productId": 1,
      "productName": "Laptop Pro 15",
      "price": 1299.99,
      "quantity": 2,
      "subtotal": 2599.98
    },
    {
      "productId": 2,
      "productName": "Wireless Headphones",
      "price": 249.99,
      "quantity": 1,
      "subtotal": 249.99
    }
  ],
  "totalAmount": 2849.97,
  "status": "PENDING",
  "shippingAddress": "42 Tech Street, Sydney NSW 2000",
  "createdAt": "2024-01-15T10:45:00"
}
```

The test script saves `{{orderId}}` from `response.json().id`.

---

### Step 9: Verify cart is cleared after order

**Request:** `GET {{baseUrl}}/api/cart`

**Expected:** `items` array is empty — `clearCart()` was called inside `placeOrder()`.

---

### Step 10: Verify product stock was reduced

**Request:** `GET {{baseUrl}}/api/products/1`

**Expected:** `stock` should be 48 (was 50, reduced by 2). This confirms the Feign call to `reduceStock` succeeded.

---

### Step 11: Get order history

**Request:** `GET {{baseUrl}}/api/orders`

**Headers:** `Authorization: Bearer {{token}}`

**Expected:** Array containing the order you just placed.

---

### Step 12: Get specific order

**Request:** `GET {{baseUrl}}/api/orders/{{orderId}}`

**Expected:** `200 OK` with the same order detail.

---

### Step 13: Try to access another user's order

**Request:** `GET {{baseUrl}}/api/orders/999`

**Expected:** `403 Forbidden` — the order doesn't belong to user `alice`.

---

### Step 14: Attempt to order with empty cart

**Request:** `POST {{baseUrl}}/api/orders` (cart is now empty)

**Expected:** `400 Bad Request` — `{"error": "Cart is empty"}`.

---

## 8.15 Complete System Architecture

At this point, all 5 services are running. Here is the full picture:

```
                    ┌──────────────────────────┐
                    │       Postman/Browser     │
                    └──────────┬───────────────┘
                               │ :8080
                    ┌──────────▼───────────────┐
                    │       API Gateway         │
                    │  - JWT validation          │
                    │  - Route matching          │
                    │  - Header injection        │
                    └──┬──────┬──────┬──────────┘
                       │      │      │
              /api/auth/**  /api/products/**  /api/cart/** + /api/orders/**
              /api/customers/**              │
                       │      │      │       │
           ┌───────────▼─┐ ┌──▼──────┴───┐ ┌▼──────────────────┐
           │ User Service │ │Product Svc  │ │  Order Service     │
           │  :8081       │ │  :8082      │ │  :8083             │
           │              │ │             │ │                    │
           │ - Register   │ │ - Catalogue │ │ - Cart management  │
           │ - Login      │ │ - Search    │ │ - Order placement  │
           │ - JWT issue  │ │ - Stock     │ │ - OpenFeign calls  │
           └──────┬───────┘ └─────┬───────┘ └────────┬──────────┘
                  │               │                   │
           ┌──────▼──────┐ ┌──────▼──────┐    ┌──────▼──────┐
           │   user_db   │ │ product_db  │    │  order_db   │
           │  (customers)│ │ (products)  │    │(carts/orders)│
           └─────────────┘ └─────────────┘    └─────────────┘
                                                    │ Feign ↑
                                             ← product-service
                                              (GET /api/products/{id})
                                              (PUT /api/products/{id}/reduce-stock)

Infrastructure (shared):
┌───────────────────────┐  ┌──────────────────────────────────┐
│   Config Server :8888 │  │       Eureka Server :8761        │
│   Serves *.properties │  │  Service registry + discovery    │
└───────────────────────┘  └──────────────────────────────────┘
```

---

## 8.16 Common Mistakes

### Mistake 1: Forgetting `@EnableFeignClients` on the main application class

```java
// WRONG — FeignClient interfaces will not be scanned
@SpringBootApplication
public class OrderServiceApplication { ... }

// CORRECT
@SpringBootApplication
@EnableFeignClients
public class OrderServiceApplication { ... }
```

Error: `No qualifying bean of type 'ProductServiceClient' available`

### Mistake 2: Missing `@RequestParam` name in Feign client method

```java
// WRONG — Feign cannot determine the query parameter name from the variable
ProductDto reduceStock(@PathVariable Long id, @RequestParam int quantity);

// CORRECT — always specify the name explicitly in Feign interfaces
ProductDto reduceStock(@PathVariable("id") Long id, @RequestParam("quantity") int quantity);
```

### Mistake 3: Bidirectional relationship infinite JSON loop

```java
// Cart has @OneToMany List<CartItem>
// CartItem has @ManyToOne Cart cart
// Jackson serializes Cart → items → CartItem → cart → items → ... (infinite loop)

// Fix — add @JsonIgnore on the back-reference in CartItem:
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "cart_id")
@JsonIgnore                    // ← prevents serialization of the parent reference
private Cart cart;
```

### Mistake 4: Using `FetchType.LAZY` on `Cart.items` without a transaction

```java
// If Cart.items is LAZY and you access items outside a transaction:
Cart cart = cartRepository.findByUserId(userId).get();
// Transaction ends here (outside @Transactional)
cart.getItems();  // → LazyInitializationException!

// Fix: Use FetchType.EAGER (done in this project) or ensure access within @Transactional
@OneToMany(fetch = FetchType.EAGER)   // always loads items with cart
```

### Mistake 5: Not handling Feign exceptions

```java
// If Product Service is down or product not found, Feign throws FeignException
// Without try-catch, a 404 from Product Service becomes a 500 in Order Service

// Wrap Feign calls:
try {
    ProductDto product = productServiceClient.getProductById(id);
} catch (FeignException.NotFound e) {
    throw new RuntimeException("Product not found: " + id);
}
```

---

## Module Checkpoint

Confirm all of these work end-to-end through the gateway:

- [ ] Order Service starts and `ORDER-SERVICE` appears in Eureka at `http://localhost:8761`
- [ ] `GET /api/cart` with token → empty cart returned
- [ ] `POST /api/cart/add` with productId=1, quantity=2 → cart shows 2x Laptop Pro 15
- [ ] `POST /api/cart/add` with productId=2, quantity=1 → cart shows 2 items
- [ ] `PUT /api/cart/items/{itemId}?quantity=1` → quantity updated
- [ ] `POST /api/orders` with shippingAddress → 201 with order detail
- [ ] After order: `GET /api/cart` → empty
- [ ] After order: `GET /api/products/1` → stock reduced by quantity ordered
- [ ] `GET /api/orders` → list contains the placed order
- [ ] `GET /api/orders/{orderId}` → order detail
- [ ] `POST /api/orders` with empty cart → 400 with error message

---

## Congratulations — System Complete!

You have built a complete microservices e-commerce platform:

| Module | Service | Port | Key Concepts |
|---|---|---|---|
| 3 | Config Server | 8888 | Centralized configuration, Git-backed |
| 4 | Eureka Server | 8761 | Service registry, heartbeat, dashboard |
| 5 | User Service | 8081 | JWT generation, Spring Security, @MappedSuperclass |
| 6 | Product Service | 8082 | JPA entities, DataInitializer, gateway-first auth |
| 7 | API Gateway | 8080 | JWT validation, reactive routing, header injection |
| 8 | Order Service | 8083 | OpenFeign, cart/order management, @Transactional |

**What you built:**
- A customer can register, login, and receive a JWT
- The gateway validates every token and injects user identity into downstream headers
- Downstream services trust the gateway and use the injected `X-User-Id`
- Products are publicly browsable — no authentication needed
- A customer can add products to their cart, place an order, and see their order history
- Order placement reduces product stock via OpenFeign (service-to-service communication)
- All services register with Eureka and are routed via load-balanced `lb://` URIs
- All configuration is centralized in Config Server and served from a local Git repository
