# Module 02 — REST Fundamentals

## What is REST?

**REST** = **RE**presentational **S**tate **T**ransfer

REST is an **architectural style** (not a protocol, not a standard) for designing networked APIs. Roy Fielding defined it in his PhD dissertation (2000).

A system that follows REST principles is called **RESTful**.

---

## REST Constraints (The 6 Principles)

| Constraint | Meaning |
|-----------|---------|
| **Client-Server** | UI and data storage are separated. Client doesn't care how the server stores data. |
| **Stateless** | Each request must contain all info needed to process it. Server stores NO session state. |
| **Cacheable** | Responses must declare whether they are cacheable. |
| **Uniform Interface** | Resources are identified by URIs. Standard methods (GET, POST, etc.) are used. |
| **Layered System** | Client doesn't know if it's talking directly to the server or via a proxy/load balancer. |
| **Code on Demand** *(optional)* | Server can send executable code (e.g., JavaScript) to clients. |

The most important for API design: **Stateless** and **Uniform Interface**.

### Stateless — What it means in practice

```
❌ Stateful (session-based):
Client logs in → Server stores session "user123 is logged in" → Next request uses that session

✅ Stateless (REST):
Every request includes authentication (JWT token / API key).
Server processes each request independently.
No server-side session state is stored.
```

---

## Resources and URIs

In REST, everything is a **resource**. A resource is a noun — a thing you want to expose.

```
Products    → /api/products
A product   → /api/products/5
Categories  → /api/categories
Orders      → /api/orders
Order items → /api/orders/3/items
```

### URI Design Rules

| Rule | Bad ❌ | Good ✅ |
|------|--------|---------|
| Use nouns, not verbs | `/getProducts` | `/products` |
| Use plural nouns | `/product/5` | `/products/5` |
| Use lowercase | `/API/Products` | `/api/products` |
| Use hyphens, not underscores | `/product_items` | `/product-items` |
| Hierarchical for related resources | `/getCategoryProducts?catId=2` | `/categories/2/products` |
| Don't include file extensions | `/products.json` | `/products` |
| Version your API | `/products` | `/v1/products` |

---

## HTTP Methods — The Verbs of REST

Each HTTP method has a specific, well-understood meaning:

| Method | CRUD Operation | SQL Equivalent | Safe? | Idempotent? |
|--------|---------------|----------------|-------|------------|
| `GET` | Read | `SELECT` | ✅ Yes | ✅ Yes |
| `POST` | Create | `INSERT` | ❌ No | ❌ No |
| `PUT` | Full Update | `UPDATE` | ❌ No | ✅ Yes |
| `PATCH` | Partial Update | `UPDATE` | ❌ No | ✅ Yes* |
| `DELETE` | Delete | `DELETE` | ❌ No | ✅ Yes |

**Safe**: Does not modify server state (GET is safe — calling it a million times changes nothing).
**Idempotent**: Calling it multiple times with the same data produces the same result.

### PUT vs PATCH — Important Distinction

```
Product in DB: { id: 1, name: "Laptop", price: 999.99, stock: 50, categoryId: 1 }

PUT /api/products/1
Body: { "name": "Gaming Laptop", "price": 1299.99, "stock": 50, "categoryId": 1 }
→ Replaces ENTIRE resource. You must send ALL fields.
→ If you omit "stock", it becomes null!

PATCH /api/products/1/stock?quantity=75
→ Updates ONLY the stock. Other fields remain unchanged.
→ More efficient when you only need to change one field.
```

### When to use POST vs PUT

```
POST /api/products         → Create a new product (server assigns the ID)
PUT  /api/products/5       → Replace product with ID 5 (you know the ID)
```

---

## HTTP Status Codes

Status codes tell the client what happened. They are grouped into ranges:

### 2xx — Success

| Code | Name | When to Use |
|------|------|-------------|
| `200 OK` | Success | GET, PUT, PATCH — returned data in body |
| `201 Created` | Resource created | POST — new resource was created |
| `204 No Content` | Success, no body | DELETE — success but nothing to return |

### 4xx — Client Errors (the client did something wrong)

| Code | Name | When to Use |
|------|------|-------------|
| `400 Bad Request` | Invalid request | Validation failed, malformed JSON |
| `401 Unauthorized` | Not authenticated | No/invalid credentials provided |
| `403 Forbidden` | Not authorized | Authenticated but not allowed to access this |
| `404 Not Found` | Resource not found | Product/Category with given ID doesn't exist |
| `405 Method Not Allowed` | Wrong HTTP method | POST on a read-only endpoint |
| `409 Conflict` | Conflict with current state | Duplicate email/name |
| `422 Unprocessable Entity` | Semantic validation failure | Business rule violation |

### 5xx — Server Errors (the server did something wrong)

| Code | Name | When to Use |
|------|------|-------------|
| `500 Internal Server Error` | Unexpected crash | NullPointerException, DB connection lost |
| `502 Bad Gateway` | Upstream server error | Microservice downstream failed |
| `503 Service Unavailable` | Server is down | Maintenance, overloaded |

### Status Code Decision Tree

```
Request received
    │
    ├─ Did the client send bad data?  ──► 400 Bad Request
    ├─ Not authenticated?             ──► 401 Unauthorized
    ├─ No permission?                 ──► 403 Forbidden
    ├─ Resource not found?            ──► 404 Not Found
    ├─ Duplicate resource?            ──► 409 Conflict
    ├─ Server crashed?                ──► 500 Internal Server Error
    │
    └─ Success!
        ├─ Creating something?         ──► 201 Created
        ├─ Returning data?             ──► 200 OK
        └─ Nothing to return?          ──► 204 No Content
```

---

## Request and Response Structure

### HTTP Request

```
POST /api/products HTTP/1.1
Host: localhost:8080
Content-Type: application/json        ← tells server: body is JSON
Authorization: Bearer eyJhbGciOi...   ← auth token (if secured)
Accept: application/json              ← tells server: I want JSON back

{
  "name": "MacBook Pro",
  "price": 1999.99,
  "categoryId": 1
}
```

### HTTP Response

```
HTTP/1.1 201 Created
Content-Type: application/json        ← confirms: body is JSON
Location: /api/products/5             ← where the new resource can be found

{
  "id": 5,
  "name": "MacBook Pro",
  "price": 1999.99,
  "categoryId": 1,
  "categoryName": "Electronics"
}
```

---

## REST vs SOAP

| Aspect | REST | SOAP |
|--------|------|------|
| Protocol | HTTP | HTTP, SMTP, TCP |
| Data Format | JSON (usually) | XML (always) |
| Complexity | Simple | Complex |
| Performance | Lightweight | Heavy (XML overhead) |
| Standard | Architectural style | W3C Standard |
| Use today | Modern APIs (99%) | Legacy enterprise systems |

---

## REST vs GraphQL

| Aspect | REST | GraphQL |
|--------|------|---------|
| Endpoints | Multiple (`/products`, `/categories`) | Single (`/graphql`) |
| Data fetching | Fixed response shape | Client specifies exact fields |
| Over-fetching | Common (get extra fields) | None (get exactly what you ask) |
| Under-fetching | Common (need multiple calls) | None (one query for nested data) |
| Learning curve | Low | Higher |
| When to use | Standard CRUD APIs | Complex querying, mobile apps |

---

## API Versioning Strategies

When you need to make breaking changes to your API, you version it so existing clients don't break.

```
Strategy 1 — URL versioning (most common, easiest to understand):
  /v1/api/products
  /v2/api/products

Strategy 2 — Header versioning:
  GET /api/products
  Accept: application/vnd.company.v2+json

Strategy 3 — Query parameter:
  GET /api/products?version=2
```

In Spring Boot, URL versioning is the simplest:
```java
@RequestMapping("/v1/api/products")  // or handle via @RequestMapping at class level
```
