# Project 3 — AgriMarket: Farm Produce Marketplace

## Scenario

AgriMarket is a digital marketplace that directly connects farmers with buyers — individuals, restaurants, and retailers — eliminating middlemen. Farmers list their fresh produce with quantities, prices, and harvest details. Buyers browse the catalog, place multi-item orders, and pay online. Admins oversee the platform and verify farmer accounts. Stock is managed automatically: quantities reduce only after successful payment, and restore on cancellation.

**Roles:**
- **Admin** — manages the platform, verifies farmers, resolves disputes
- **Buyer (Customer)** — browses produce, places orders, pays online
- **Farmer (Supplier)** — lists produce, manages stock, updates order delivery status

---

## System Architecture

```
                        ┌──────────────────────┐
                        │    config-server      │
                        │       :8888           │
                        │  (Central Config)     │
                        └──────────┬───────────┘
                                   │  all services fetch config on startup
                        ┌──────────▼───────────┐
                        │    eureka-server      │
                        │       :8761           │
                        │  (Service Registry)   │
                        └──────────┬───────────┘
                                   │  all services register here
                        ┌──────────▼───────────┐
                        │     api-gateway       │
                        │       :8080           │
                        │  ┌─────────────────┐  │
                        │  │  JWT Filter     │  │
                        │  │  • Validates    │  │
                        │  │    token        │  │
                        │  │  • Injects      │  │
                        │  │    X-User-Id    │  │
                        │  │    X-User-Role  │  │
                        │  │    X-Username   │  │
                        │  └─────────────────┘  │
                        └──┬────┬────┬───────┬──┘
                           │    │    │       │
               ┌───────────┘    │    │       └──────────────┐
               │                │    │                      │
    ┌──────────▼──────┐  ┌──────▼────▼──────┐  ┌──────────▼──────┐
    │  user-service   │  │ catalog-service  │  │ payment-service  │
    │     :8081       │  │     :8082        │  │     :8085        │
    │                 │  │                  │  │                  │
    │  • Register     │  │  • Produce       │  │  • Process       │
    │  • Login (JWT)  │  │    listings      │  │    payments      │
    │  • Admin CRUD   │  │  • Categories    │  │  • Payment       │
    │  • Farmer       │  │  • Stock mgmt    │  │    history       │
    │    verification │  │                  │  │                  │
    └─────────────────┘  └──────────────────┘  └──────────────────┘
                                   ▲
                        ┌──────────┴───────────┐
                        │    order-service      │
                        │        :8083          │
                        │                       │
                        │  • Multi-item orders  │
                        │  • Delivery tracking  │
                        │  • Trigger payment    │
                        │  • Stock decrement    │
                        └───────────────────────┘
```

### Service Summary

| Service | Port | Database | Responsibility |
|---|---|---|---|
| config-server | 8888 | — | Stores all service configuration centrally |
| eureka-server | 8761 | — | Service discovery and registration |
| api-gateway | 8080 | — | JWT authentication, request routing |
| user-service | 8081 | `agrimarket_users` | Registration, login, JWT, admin + farmer verification |
| catalog-service | 8082 | `agrimarket_catalog` | Produce listings, categories, stock levels |
| order-service | 8083 | `agrimarket_orders` | Multi-item orders, delivery status tracking |
| payment-service | 8085 | `agrimarket_payments` | Mock payment processing and history |

---

## User Roles & Access

| Role | How Created | Default Credentials |
|---|---|---|
| ADMIN | Seeded by `DataInitializer` on first startup | `admin` / `Admin@123` |
| CUSTOMER (Buyer) | Self-register at `POST /api/auth/register` with `role: CUSTOMER` | — |
| SUPPLIER (Farmer) | Self-register at `POST /api/auth/register` with `role: SUPPLIER` | — |

> **Note:** Farmers must be verified by Admin before they can create produce listings. Unverified farmers can register and log in but cannot list produce.

---

## Data Model

### Entity Relationship Diagram

```
user-service database
─────────────────────────────────────────────────────────────
  ┌─────────────────────────────────────────────────────┐
  │                      users                          │
  │  id | username | password | first_name | last_name  │
  │  mobile_number | role | account_enabled | created_at │
  └──────────────────────┬──────────────────────────────┘
                         │ role discriminates subtype
           ┌─────────────┼───────────────┐
           ▼             ▼               ▼
  ┌──────────────┐ ┌───────────────┐ ┌──────────────┐
  │    buyers    │ │    farmers    │ │    admins    │
  │  (CUSTOMER)  │ │  (SUPPLIER)   │ │   (ADMIN)    │
  │              │ │               │ │              │
  │delivery_addr │ │farm_name      │ │  (no extra   │
  │buyer_type    │ │farm_location  │ │   fields)    │
  └──────────────┘ │farm_size_acres│ └──────────────┘
                   │certifications │
                   │bank_account   │
                   │bank_ifsc      │
                   │rating         │
                   │verified       │
                   └───────────────┘

catalog-service database
─────────────────────────────────────────────────────────────
  ┌──────────────────────────────────────────────────────┐
  │                      produce                         │
  │  id | farmer_id | farmer_name | farm_name           │
  │  name | description | category | unit               │
  │  price_per_unit | available_quantity                 │
  │  min_order_quantity | harvest_date | expiry_date     │
  │  organic | image_url | active | created_at          │
  └──────────────────────────────────────────────────────┘

  category values: FRUITS | VEGETABLES | GRAINS | DAIRY | SPICES | HERBS

  Stock state:
  available_quantity > 0  →  active=true  (visible in listings)
  available_quantity = 0  →  active=false (auto-hidden from listings)
  past expiry_date        →  excluded from public listings

order-service database
─────────────────────────────────────────────────────────────
  ┌──────────────────────────────────────────────────────┐
  │                      orders                          │
  │  id | buyer_id | buyer_name | delivery_address      │
  │  status | payment_status | total_amount             │
  │  notes | created_at | updated_at                    │
  └────────────────────────┬─────────────────────────────┘
                           │ ONE order has MANY items
                           ▼
  ┌──────────────────────────────────────────────────────┐
  │                   order_items                        │
  │  id | order_id (FK) | produce_id | produce_name     │
  │  farmer_id | farmer_name | unit                     │
  │  price_per_unit | quantity | subtotal                │
  └──────────────────────────────────────────────────────┘

  Order status flow:
  PENDING ──confirm──► CONFIRMED ──pack──► PACKED ──ship──► SHIPPED ──deliver──► DELIVERED
  PENDING ──cancel──►  CANCELLED
  CONFIRMED ──cancel──► CANCELLED (with refund if paid)

  payment_status:  PENDING → PAID → REFUNDED (on cancellation after payment)

payment-service database
─────────────────────────────────────────────────────────────
  ┌──────────────────────────────────────────────────────┐
  │                     payments                         │
  │  id | order_id | buyer_id | amount                  │
  │  status | card_last4 | transaction_id | paid_at      │
  └──────────────────────────────────────────────────────┘

  status values: SUCCESS | DECLINED | INSUFFICIENT_FUNDS | EXPIRED
```

### Entity Field Reference

**Buyer** (role = CUSTOMER)
| Field | Type | Notes |
|---|---|---|
| id | Long | Primary key |
| username | String | Unique |
| password | String | BCrypt hashed |
| firstName, lastName | String | |
| mobileNumber | String | |
| deliveryAddress | String | Default delivery address |
| buyerType | Enum | INDIVIDUAL, RESTAURANT, RETAILER |

**Farmer** (role = SUPPLIER)
| Field | Type | Notes |
|---|---|---|
| farmName | String | |
| farmLocation | String | |
| farmSizeAcres | Double | |
| certifications | String | e.g. "Organic,GAP" |
| bankAccountNumber | String | |
| bankIfscCode | String | |
| rating | Double | Default 0.0 |
| verified | Boolean | Default false — Admin must verify |

**Produce** (catalog-service)
| Field | Type | Notes |
|---|---|---|
| id | Long | Primary key |
| farmerId | Long | From user-service |
| farmerName | String | |
| farmName | String | |
| name | String | e.g. "Alphonso Mango" |
| description | String | |
| category | Enum | FRUITS, VEGETABLES, GRAINS, DAIRY, SPICES, HERBS |
| unit | String | e.g. "kg", "dozen", "litre" |
| pricePerUnit | BigDecimal | |
| availableQuantity | Integer | |
| minOrderQuantity | Integer | Default 1 |
| harvestDate | LocalDate | |
| expiryDate | LocalDate | |
| organic | Boolean | Default false |
| imageUrl | String | |
| active | Boolean | Default true |

**Order** (order-service)
| Field | Type | Notes |
|---|---|---|
| id | Long | Primary key |
| buyerId | Long | From X-User-Id |
| buyerName | String | |
| deliveryAddress | String | |
| status | Enum | PENDING, CONFIRMED, PACKED, SHIPPED, DELIVERED, CANCELLED |
| paymentStatus | Enum | PENDING, PAID, REFUNDED |
| totalAmount | BigDecimal | Sum of all item subtotals |
| notes | String | Special instructions from buyer |

**OrderItem** (child of Order)
| Field | Type | Notes |
|---|---|---|
| id | Long | Primary key |
| orderId | Long | FK to orders |
| produceId | Long | |
| produceName | String | |
| farmerId | Long | |
| farmerName | String | |
| unit | String | |
| pricePerUnit | BigDecimal | Snapshot at time of order |
| quantity | Integer | |
| subtotal | BigDecimal | pricePerUnit × quantity |

**Payment** (payment-service)
| Field | Type | Notes |
|---|---|---|
| id | Long | Primary key |
| orderId | Long | |
| buyerId | Long | |
| amount | BigDecimal | |
| status | Enum | SUCCESS, DECLINED, INSUFFICIENT_FUNDS, EXPIRED |
| cardLast4 | String | |
| transactionId | String | UUID |
| paidAt | LocalDateTime | |

---

## Inter-Service Communication

```
  SCENARIO 1: Buyer Places an Order
  ────────────────────────────────────
  Buyer               api-gateway        order-service         catalog-service
    │                     │                   │                      │
    │── POST /api/orders ────────────────────►│                      │
    │                     │  validate JWT     │                      │
    │                     │  inject headers   │                      │
    │                     │                   │  for each item:      │
    │                     │                   │── GET /api/produce/{id} ──►│
    │                     │                   │◄── produce details ──│
    │                     │                   │  check: active?      │
    │                     │                   │  check: qty available?
    │                     │                   │  check: minOrderQty? │
    │                     │                   │  snapshot: price     │
    │                     │                   │  compute total       │
    │                     │                   │  create order (PENDING)
    │◄── 201 Order Created ──────────────────│                      │


  SCENARIO 2: Buyer Pays for Order
  ─────────────────────────────────
  Buyer          api-gateway     order-service    payment-service   catalog-service
    │                │                │                 │                 │
    │── POST /api/orders/{id}/pay ───►│                 │                 │
    │                │  validate JWT  │                 │                 │
    │                │  inject headers│── POST /api/payments/process ────►│
    │                │                │◄── payment result ────────────────│
    │                │                │                 │                 │
    │                │                │  if SUCCESS:    │                 │
    │                │                │  paymentStatus → PAID             │
    │                │                │── PUT /api/produce/{id}/decrement (per item) ──►│
    │                │                │   (reduce availableQuantity)      │
    │◄── 200 Payment Result ─────────│                 │                 │


  SCENARIO 3: Order Cancellation After Payment
  ─────────────────────────────────────────────
  Buyer        api-gateway    order-service    catalog-service
    │               │               │                │
    │── DELETE /api/orders/{id} ───►│                │
    │               │  validate JWT │                │
    │               │  inject headers               │
    │               │               │  check status: must be PENDING/CONFIRMED
    │               │               │── PUT /api/produce/{id}/increment (per item) ──►│
    │               │               │   (restore availableQuantity)      │
    │               │               │  paymentStatus → REFUNDED          │
    │◄── 200 Order Cancelled ───────│                │


  INTER-SERVICE RULES:
  • order-service calls catalog-service for each item on order creation
    → to validate produceId is active and not expired
    → to check availableQuantity >= requested quantity
    → to snapshot pricePerUnit at the time of ordering
  • order-service calls payment-service when buyer pays
  • On SUCCESS, order-service calls catalog-service to decrement stock per item
  • On cancellation after payment, order-service calls catalog-service to restore stock
  • All inter-service calls use service names (Eureka discovery)
```

---

## REST API Specification

### Auth Service (`/api/auth`) — No Authentication Required

#### POST /api/auth/register
**Buyer Request:**
```json
{
  "username": "buyer_meena",
  "password": "buy@123",
  "firstName": "Meena",
  "lastName": "Reddy",
  "mobileNumber": "9876540001",
  "role": "CUSTOMER",
  "deliveryAddress": "45 Koramangala, Bangalore 560095",
  "buyerType": "INDIVIDUAL"
}
```
**Farmer Request:**
```json
{
  "username": "farmer_suresh",
  "password": "farm@123",
  "firstName": "Suresh",
  "lastName": "Patil",
  "mobileNumber": "9876547777",
  "role": "SUPPLIER",
  "farmName": "Patil Organic Farms",
  "farmLocation": "Kolar, Karnataka",
  "farmSizeAcres": 12.5,
  "certifications": "Organic,GAP",
  "bankAccountNumber": "1234567890",
  "bankIfscCode": "SBIN0001234"
}
```
**Response 201:** `{ "token": "<jwt>", "userId": 3, "role": "SUPPLIER" }`

#### POST /api/auth/login
```json
{ "username": "admin", "password": "Admin@123" }
```
**Response 200:** `{ "token": "<jwt>", "userId": 1, "role": "ADMIN" }`

---

### Admin — User Management (`/api/admin`) — ADMIN role required

| Method | Endpoint | Description |
|---|---|---|
| GET | /api/admin/buyers | List all buyers |
| PUT | /api/admin/buyers/{id}/disable | Disable buyer |
| PUT | /api/admin/buyers/{id}/enable | Enable buyer |
| DELETE | /api/admin/buyers/{id} | Delete buyer |
| GET | /api/admin/farmers | List all farmers |
| PUT | /api/admin/farmers/{id}/verify | Verify farmer (allows creating listings) |
| PUT | /api/admin/farmers/{id}/disable | Disable farmer |
| DELETE | /api/admin/farmers/{id} | Delete farmer |
| POST | /api/admin/admins | Create a new admin account |
| GET | /api/admin/admins | List all admins |

---

### Catalog Service (`/api/produce`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | /api/produce | Public | All active, non-expired produce |
| GET | /api/produce/{id} | Public | Produce detail |
| GET | /api/produce/category/{cat} | Public | Filter by category |
| GET | /api/produce/search?q= | Public | Search by name or farm |
| GET | /api/produce/organic | Public | Organic produce only |
| GET | /api/produce/my | SUPPLIER | Farmer's own listings |
| POST | /api/produce | SUPPLIER | Create a produce listing (farmer must be verified) |
| PUT | /api/produce/{id} | SUPPLIER | Update listing (own only) |
| DELETE | /api/produce/{id} | SUPPLIER | Delete listing (own only) |
| PUT | /api/produce/{id}/stock | SUPPLIER | Update available quantity |
| GET | /api/produce/all | ADMIN | All listings including inactive |

**POST /api/produce — Request:**
```json
{
  "name": "Alphonso Mango",
  "description": "Premium Ratnagiri Alphonso, naturally ripened",
  "category": "FRUITS",
  "unit": "dozen",
  "pricePerUnit": 450.00,
  "availableQuantity": 200,
  "minOrderQuantity": 1,
  "harvestDate": "2026-03-18",
  "expiryDate": "2026-03-28",
  "organic": true,
  "imageUrl": "https://example.com/mango.jpg"
}
```

**POST /api/produce — Response 201:**
```json
{
  "id": 4,
  "farmerName": "Suresh Patil",
  "farmName": "Patil Organic Farms",
  "name": "Alphonso Mango",
  "category": "FRUITS",
  "unit": "dozen",
  "pricePerUnit": 450.00,
  "availableQuantity": 200,
  "minOrderQuantity": 1,
  "organic": true,
  "active": true
}
```

**GET /api/produce — Sample Response:**
```json
[
  {
    "id": 1,
    "farmerName": "Suresh Patil",
    "farmName": "Patil Organic Farms",
    "name": "Tomatoes",
    "category": "VEGETABLES",
    "unit": "kg",
    "pricePerUnit": 35.00,
    "availableQuantity": 500,
    "organic": false,
    "harvestDate": "2026-03-17",
    "expiryDate": "2026-03-24"
  }
]
```

**PUT /api/produce/{id}/stock — Request:**
```json
{ "availableQuantity": 350 }
```

---

### Order Service (`/api/orders`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | /api/orders | CUSTOMER | Place a new order with multiple items |
| GET | /api/orders/my | CUSTOMER | List own orders |
| GET | /api/orders/{id} | CUSTOMER / SUPPLIER / ADMIN | Order detail with items |
| DELETE | /api/orders/{id} | CUSTOMER | Cancel order |
| POST | /api/orders/{id}/pay | CUSTOMER | Pay for the order |
| GET | /api/orders/farmer | SUPPLIER | Orders containing this farmer's produce |
| PUT | /api/orders/{id}/confirm | SUPPLIER | Confirm order |
| PUT | /api/orders/{id}/pack | SUPPLIER | Mark as packed |
| PUT | /api/orders/{id}/ship | SUPPLIER | Mark as shipped |
| PUT | /api/orders/{id}/deliver | SUPPLIER | Mark as delivered |
| GET | /api/orders/all | ADMIN | All orders on platform |

**POST /api/orders — Request:**
```json
{
  "deliveryAddress": "45 Koramangala, Bangalore 560095",
  "notes": "Please deliver before 10am",
  "items": [
    { "produceId": 1, "quantity": 5 },
    { "produceId": 4, "quantity": 2 }
  ]
}
```

**POST /api/orders — Response 201:**
```json
{
  "id": 8,
  "status": "PENDING",
  "paymentStatus": "PENDING",
  "totalAmount": 1075.00,
  "items": [
    {
      "produceName": "Tomatoes",
      "unit": "kg",
      "quantity": 5,
      "pricePerUnit": 35.00,
      "subtotal": 175.00
    },
    {
      "produceName": "Alphonso Mango",
      "unit": "dozen",
      "quantity": 2,
      "pricePerUnit": 450.00,
      "subtotal": 900.00
    }
  ]
}
```

**POST /api/orders/{id}/pay — Request:**
```json
{
  "cardNumber": "4242424242424242",
  "cardExpiry": "12/26",
  "cardCvv": "789",
  "cardHolderName": "Meena Reddy"
}
```

**POST /api/orders/{id}/pay — Response 200:**
```json
{
  "transactionId": "txn-uuid-here",
  "status": "SUCCESS",
  "amount": 1075.00,
  "orderId": 8
}
```

---

### Payment Service (`/api/payments`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | /api/payments/my | CUSTOMER | View own payment history |
| GET | /api/payments/order/{id} | CUSTOMER / ADMIN | Payment record for an order |
| GET | /api/payments/all | ADMIN | All payments on platform |

**Test Card Numbers for Payment Simulation:**

| Card Number | Result |
|---|---|
| 4242 4242 4242 4242 | SUCCESS |
| 4000 0000 0000 0002 | DECLINED |
| 4000 0000 0000 9995 | INSUFFICIENT_FUNDS |
| 4000 0000 0000 0069 | EXPIRED |

---

## Business Rules

1. A buyer cannot order more than the `availableQuantity` of any produce item.
2. Each item in the order must meet the `minOrderQuantity` threshold.
3. Produce with `expiryDate` in the past must not appear in public listings.
4. Produce automatically becomes inactive (`active=false`) when `availableQuantity` reaches 0.
5. Only verified farmers can create produce listings.
6. Stock is only decremented after a successful payment, not when the order is placed.
7. Order can only be cancelled when status is `PENDING` or `CONFIRMED`.
8. When an order is cancelled after payment, the payment status becomes `REFUNDED` and all item stocks are restored.
9. Farmers can only update the delivery status (confirm, pack, ship, deliver) of orders containing their own produce.
10. Farmers can only edit or delete their own listings.
11. The price per unit is snapshotted at the time of ordering — price changes after the order is placed do not affect existing orders.
12. An order must contain at least 1 item.

---

## Evaluation Rubric (100 Points)

### Section 1 — Infrastructure & Configuration (15 pts)

| Criteria | Points | Description |
|---|---|---|
| Config Server | 3 | Spring Cloud Config Server on port 8888; all services fetch config at startup |
| Eureka Discovery | 3 | Eureka Server on port 8761; all services register and visible in dashboard |
| API Gateway | 5 | Gateway on port 8080; JWT filter validates token, rejects unauthenticated with 401, injects `X-User-Id`, `X-User-Role`, `X-Username` headers |
| Docker Compose | 4 | `docker-compose up -d` starts all services + databases; healthchecks and `depends_on` ordering correct |

### Section 2 — User Service & Authentication (20 pts)

| Criteria | Points | Description |
|---|---|---|
| Registration | 3 | Buyer and Farmer can self-register; Admin-only endpoint to create Admin accounts |
| Login & JWT | 4 | Login returns valid JWT encoding userId, username, role; expiry enforced |
| Role-Based Access | 5 | ADMIN, CUSTOMER, SUPPLIER roles enforced; wrong-role returns 403 |
| Admin Management | 4 | Admin can list, enable, disable, delete buyers and farmers; can verify farmers |
| Default Admin Seed | 2 | `DataInitializer` seeds `admin / Admin@123` on first startup; no duplicate on restart |
| Password Security | 2 | Passwords stored as BCrypt hashes; plaintext never returned |

### Section 3 — Catalog Service (20 pts)

| Criteria | Points | Description |
|---|---|---|
| Entity & DB | 4 | Produce entity with all required fields; MySQL table auto-created |
| CRUD Endpoints | 6 | All required endpoints functional; public listing excludes expired/inactive |
| Role Enforcement | 4 | Public browse vs SUPPLIER-only listing management; farmers manage own only |
| Business Rules | 4 | Verified-farmer guard; auto-inactive on zero stock; expiry filter |
| Error Handling | 2 | Structured error responses for not-found, forbidden, bad-request |

### Section 4 — Order Service (20 pts)

| Criteria | Points | Description |
|---|---|---|
| Entity & DB | 4 | Order and OrderItem entities with all required fields; MySQL tables auto-created |
| CRUD Endpoints | 6 | All required endpoints functional with correct HTTP methods and status codes |
| Role Enforcement | 4 | Buyers see/cancel own; farmers see orders with their produce; admin sees all |
| Business Rules | 4 | Stock check on order creation; price snapshot; stock decrement post-payment; restore on cancel |
| Error Handling | 2 | Structured error responses for not-found, conflict, insufficient-stock |

### Section 5 — Payment Service (10 pts)

| Criteria | Points | Description |
|---|---|---|
| Mock PSP Logic | 4 | Test card numbers route to correct outcomes |
| Payment Record | 3 | Payment entity persisted with status, amount, card last-4, timestamp |
| Query Endpoints | 3 | Buyer views own history; Admin views all |

### Section 6 — Inter-Service Communication (10 pts)

| Criteria | Points | Description |
|---|---|---|
| order → catalog-service | 4 | Feign calls to validate produce, check stock, snapshot price on order creation; decrement/restore stock |
| order → payment-service | 4 | Feign call to process payment; result drives order payment status |
| Fallback Handling | 2 | Graceful error when downstream service is unavailable |

### Section 7 — Code Quality & Design (5 pts)

| Criteria | Points | Description |
|---|---|---|
| DTO Pattern | 2 | Separate Request and Response classes; entities not returned from controllers |
| Lombok | 1 | Appropriate use of Lombok annotations |
| Clean Code | 2 | No dead code; no hardcoded secrets; sensible naming |

### Grading Scale

| Score | Grade |
|---|---|
| 90–100 | O (Outstanding) |
| 80–89 | A+ |
| 70–79 | A |
| 60–69 | B |
| 50–59 | C |
| < 50 | F |

### Deductions

| Violation | Penalty |
|---|---|
| Any service fails to start | −5 per service |
| Admin endpoint accessible without ADMIN role | −5 |
| Plaintext passwords in DB or API response | −5 |
| Hardcoded secrets in source code | −3 |
