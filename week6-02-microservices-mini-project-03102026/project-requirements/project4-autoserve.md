# Project 4 — AutoServe: Vehicle Service & Repair Platform

## Scenario

AutoServe connects vehicle owners with certified mechanics and garages for servicing, repairs, and inspections. Vehicle owners describe their problem, choose a garage, select a service type, and book a slot online. Mechanics manage their garage profile, list available services with pricing, accept bookings, record parts used, set a final bill amount, and update the job status. Vehicle owners pay the final bill online. Admins oversee the platform and verify garages.

**Roles:**
- **Admin** — manages the platform, verifies mechanics, views all data
- **Vehicle Owner (Customer)** — books services, pays bills, tracks job status
- **Mechanic (Supplier)** — manages garage profile, services, booking lifecycle, parts recording

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
    │  user-service   │  │  garage-service  │  │ payment-service  │
    │     :8081       │  │     :8082        │  │     :8085        │
    │                 │  │                  │  │                  │
    │  • Register     │  │  • Garage        │  │  • Process       │
    │  • Login (JWT)  │  │    profiles      │  │    payments      │
    │  • Admin CRUD   │  │  • Services      │  │  • Payment       │
    │  • Mechanic     │  │    offered       │  │    history       │
    │    verification │  │  • Pricing       │  │                  │
    └─────────────────┘  └──────────────────┘  └──────────────────┘
                                   ▲
                        ┌──────────┴───────────┐
                        │   booking-service     │
                        │        :8083          │
                        │                       │
                        │  • Create bookings    │
                        │  • Job card / status  │
                        │  • Record parts used  │
                        │  • Final billing      │
                        │  • Trigger payment    │
                        └───────────────────────┘
```

### Service Summary

| Service | Port | Database | Responsibility |
|---|---|---|---|
| config-server | 8888 | — | Stores all service configuration centrally |
| eureka-server | 8761 | — | Service discovery and registration |
| api-gateway | 8080 | — | JWT authentication, request routing |
| user-service | 8081 | `autoserve_users` | Registration, login, JWT, mechanic verification |
| garage-service | 8082 | `autoserve_garages` | Garage profiles, services offered, pricing |
| booking-service | 8083 | `autoserve_bookings` | Booking lifecycle, parts tracking, job billing |
| payment-service | 8085 | `autoserve_payments` | Mock payment processing and history |

---

## User Roles & Access

| Role | How Created | Default Credentials |
|---|---|---|
| ADMIN | Seeded by `DataInitializer` on first startup | `admin` / `Admin@123` |
| CUSTOMER (Vehicle Owner) | Self-register at `POST /api/auth/register` with `role: CUSTOMER` | — |
| SUPPLIER (Mechanic) | Self-register at `POST /api/auth/register` with `role: SUPPLIER` | — |

> **Note:** Mechanics must be verified by Admin before they can accept bookings. Their garage profile is created in garage-service during registration.

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
  ┌──────────────────┐ ┌───────────────┐ ┌──────────────┐
  │  vehicle_owners  │ │   mechanics   │ │    admins    │
  │   (CUSTOMER)     │ │  (SUPPLIER)   │ │   (ADMIN)    │
  │                  │ │               │ │              │
  │vehicle_number    │ │garage_name    │ │  (no extra   │
  │vehicle_make      │ │garage_address │ │   fields)    │
  │vehicle_model     │ │specializations│ └──────────────┘
  │vehicle_year      │ │certifications │
  │fuel_type         │ │experience_yrs │
  └──────────────────┘ │rating         │
                       │verified       │
                       └───────────────┘

garage-service database
─────────────────────────────────────────────────────────────
  ┌──────────────────────────────────────────────────────┐
  │                     garages                          │
  │  id | mechanic_id | mechanic_name | garage_name      │
  │  garage_address | specializations | certifications   │
  │  experience_years | rating | total_reviews          │
  │  open_from | open_to | working_days | active        │
  └────────────────────────┬─────────────────────────────┘
                           │ ONE garage offers MANY services
                           ▼
  ┌──────────────────────────────────────────────────────┐
  │                  service_offerings                   │
  │  id | garage_id (FK) | service_name | service_type  │
  │  estimated_duration_hours | base_price | description │
  └──────────────────────────────────────────────────────┘

  service_type values: MAINTENANCE | REPAIR | INSPECTION | CLEANING

booking-service database
─────────────────────────────────────────────────────────────
  ┌──────────────────────────────────────────────────────┐
  │                     bookings                         │
  │  id | customer_id | customer_name                   │
  │  vehicle_number | vehicle_make | vehicle_model       │
  │  garage_id | garage_name | mechanic_id               │
  │  service_offering_id | service_name                 │
  │  scheduled_date | scheduled_time                    │
  │  problem_description | status | payment_status      │
  │  estimated_amount | final_amount | completion_notes  │
  │  created_at                                          │
  └────────────────────────┬─────────────────────────────┘
                           │ ONE booking has MANY parts
                           ▼
  ┌──────────────────────────────────────────────────────┐
  │                  booking_parts                       │
  │  id | booking_id (FK) | part_name | part_number     │
  │  quantity | unit_cost | total_cost                  │
  └──────────────────────────────────────────────────────┘

  Booking status flow:
  PENDING ──confirm──► CONFIRMED ──start──► IN_PROGRESS ──complete──► COMPLETED
  PENDING ──cancel──►  CANCELLED
  CONFIRMED ──cancel──► CANCELLED

  Payment rule:
  • Payment amount = final_amount (if set after completion)
  • Payment amount = estimated_amount (if paying before completion)
  • payment_status: PENDING → PAID → REFUNDED (on cancel after payment)

payment-service database
─────────────────────────────────────────────────────────────
  ┌──────────────────────────────────────────────────────┐
  │                     payments                         │
  │  id | booking_id | customer_id | amount             │
  │  status | card_last4 | transaction_id | paid_at      │
  └──────────────────────────────────────────────────────┘

  status values: SUCCESS | DECLINED | INSUFFICIENT_FUNDS | EXPIRED
```

### Entity Field Reference

**VehicleOwner** (role = CUSTOMER)
| Field | Type | Notes |
|---|---|---|
| id | Long | Primary key |
| username | String | Unique |
| password | String | BCrypt hashed |
| firstName, lastName | String | |
| mobileNumber | String | |
| vehicleNumber | String | e.g. "KA01AB1234" |
| vehicleMake | String | e.g. "Honda" |
| vehicleModel | String | e.g. "City" |
| vehicleYear | Integer | |
| fuelType | Enum | PETROL, DIESEL, ELECTRIC, CNG |

**Mechanic** (role = SUPPLIER)
| Field | Type | Notes |
|---|---|---|
| garageName | String | |
| garageAddress | String | |
| specializations | String | e.g. "Engine,Brakes,AC" |
| certifications | String | e.g. "ASE,ISO9001" |
| experienceYears | Integer | |
| rating | Double | Default 0.0 |
| verified | Boolean | Default false — Admin must verify |

**Garage** (garage-service)
| Field | Type | Notes |
|---|---|---|
| id | Long | Primary key |
| mechanicId | Long | From user-service |
| mechanicName | String | |
| garageName | String | |
| garageAddress | String | |
| specializations | String | |
| certifications | String | |
| experienceYears | Integer | |
| rating | Double | Default 0.0 |
| totalReviews | Integer | Default 0 |
| openFrom | LocalTime | |
| openTo | LocalTime | |
| workingDays | String | e.g. "MON,TUE,WED,THU,FRI,SAT" |
| active | Boolean | Default true |

**ServiceOffering** (child of Garage)
| Field | Type | Notes |
|---|---|---|
| id | Long | Primary key |
| garageId | Long | FK to garages |
| serviceName | String | e.g. "Full Service", "Oil Change" |
| serviceType | Enum | MAINTENANCE, REPAIR, INSPECTION, CLEANING |
| estimatedDurationHours | Integer | |
| basePrice | BigDecimal | |
| description | String | |

**Booking** (booking-service)
| Field | Type | Notes |
|---|---|---|
| id | Long | Primary key |
| customerId | Long | From X-User-Id |
| customerName | String | |
| vehicleNumber | String | From customer profile |
| vehicleMake | String | |
| vehicleModel | String | |
| garageId | Long | |
| garageName | String | |
| mechanicId | Long | |
| serviceOfferingId | Long | |
| serviceName | String | |
| scheduledDate | LocalDate | |
| scheduledTime | LocalTime | |
| problemDescription | String | |
| status | Enum | PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED |
| paymentStatus | Enum | PENDING, PAID, REFUNDED |
| estimatedAmount | BigDecimal | From serviceOffering.basePrice |
| finalAmount | BigDecimal | Set by mechanic at completion |
| completionNotes | String | |

**BookingPart** (child of Booking)
| Field | Type | Notes |
|---|---|---|
| id | Long | Primary key |
| bookingId | Long | FK to bookings |
| partName | String | e.g. "Engine Oil 5W-30" |
| partNumber | String | |
| quantity | Integer | |
| unitCost | BigDecimal | |
| totalCost | BigDecimal | unitCost × quantity |

**Payment** (payment-service)
| Field | Type | Notes |
|---|---|---|
| id | Long | Primary key |
| bookingId | Long | |
| customerId | Long | |
| amount | BigDecimal | |
| status | Enum | SUCCESS, DECLINED, INSUFFICIENT_FUNDS, EXPIRED |
| cardLast4 | String | |
| transactionId | String | UUID |
| paidAt | LocalDateTime | |

---

## Inter-Service Communication

```
  SCENARIO 1: Customer Creates a Booking
  ────────────────────────────────────────
  Customer            api-gateway        booking-service        garage-service
    │                     │                    │                      │
    │── POST /api/bookings ──────────────────►│                      │
    │                     │  validate JWT      │                      │
    │                     │  inject headers    │── GET /api/garages/{id} ──────►│
    │                     │                   │◄── garage detail ─────────────│
    │                     │                   │── GET /api/garages/services/{id} ──►│
    │                     │                   │◄── service details, basePrice ─│
    │                     │                   │                      │
    │                     │                   │  validate: active garage?
    │                     │                   │  validate: verified mechanic?
    │                     │                   │  create booking (PENDING)
    │                     │                   │  estimatedAmount = basePrice
    │◄── 201 Booking Created ────────────────│                      │


  SCENARIO 2: Customer Pays the Bill
  ─────────────────────────────────────
  Customer         api-gateway      booking-service      payment-service
    │                  │                  │                    │
    │── POST /api/bookings/{id}/pay ─────►│                    │
    │                  │  validate JWT    │                    │
    │                  │  inject headers  │── POST /api/payments/process ──►│
    │                  │                  │◄── payment result ──────────────│
    │                  │                  │                    │
    │                  │                  │  if SUCCESS:       │
    │                  │                  │  paymentStatus → PAID           │
    │◄── 200 Payment Result ─────────────│                    │


  INTER-SERVICE RULES:
  • booking-service calls garage-service when a booking is created
    → to validate garageId exists and is active
    → to validate serviceOfferingId belongs to that garage
    → to fetch basePrice (stored as estimatedAmount)
  • booking-service calls payment-service when customer pays
    → amount = finalAmount if set, else estimatedAmount
  • All inter-service calls use service names (Eureka discovery),
    NOT hardcoded IP addresses or ports
```

---

## REST API Specification

### Auth Service (`/api/auth`) — No Authentication Required

#### POST /api/auth/register
**Vehicle Owner Request:**
```json
{
  "username": "owner_arjun",
  "password": "car@123",
  "firstName": "Arjun",
  "lastName": "Nair",
  "mobileNumber": "9876542222",
  "role": "CUSTOMER",
  "vehicleNumber": "KA01AB1234",
  "vehicleMake": "Honda",
  "vehicleModel": "City",
  "vehicleYear": 2021,
  "fuelType": "PETROL"
}
```
**Mechanic Request:**
```json
{
  "username": "mechanic_raju",
  "password": "garage@123",
  "firstName": "Raju",
  "lastName": "Sharma",
  "mobileNumber": "9876546666",
  "role": "SUPPLIER",
  "garageName": "Sharma Auto Works",
  "garageAddress": "12 Service Road, Pune",
  "specializations": "Engine,Brakes,AC",
  "certifications": "ASE,ISO9001",
  "experienceYears": 12
}
```
**Response 201:** `{ "token": "<jwt>", "userId": 4, "role": "CUSTOMER" }`

#### POST /api/auth/login
```json
{ "username": "admin", "password": "Admin@123" }
```
**Response 200:** `{ "token": "<jwt>", "userId": 1, "role": "ADMIN" }`

---

### Admin — User Management (`/api/admin`) — ADMIN role required

| Method | Endpoint | Description |
|---|---|---|
| GET | /api/admin/customers | List all vehicle owners |
| PUT | /api/admin/customers/{id}/disable | Disable vehicle owner |
| PUT | /api/admin/customers/{id}/enable | Enable vehicle owner |
| DELETE | /api/admin/customers/{id} | Delete vehicle owner |
| GET | /api/admin/mechanics | List all mechanics |
| PUT | /api/admin/mechanics/{id}/verify | Verify mechanic (allows accepting bookings) |
| PUT | /api/admin/mechanics/{id}/disable | Disable mechanic |
| DELETE | /api/admin/mechanics/{id} | Delete mechanic |
| POST | /api/admin/admins | Create a new admin account |
| GET | /api/admin/admins | List all admins |

---

### Garage Service (`/api/garages`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | /api/garages | Public | List all active garages with their services |
| GET | /api/garages/{id} | Public | Garage detail with full service list |
| GET | /api/garages/search?q= | Public | Search by garage name or location |
| GET | /api/garages/specialization/{spec} | Public | Filter by specialization |
| GET | /api/garages/my | SUPPLIER | View own garage profile |
| PUT | /api/garages/profile | SUPPLIER | Update garage profile details |
| PUT | /api/garages/hours | SUPPLIER | Update working hours and days |
| POST | /api/garages/services | SUPPLIER | Add a new service offering |
| PUT | /api/garages/services/{id} | SUPPLIER | Update a service offering |
| DELETE | /api/garages/services/{id} | SUPPLIER | Remove a service offering |
| GET | /api/garages/all | ADMIN | All garages including inactive |

**GET /api/garages — Sample Response:**
```json
[
  {
    "id": 1,
    "mechanicName": "Raju Sharma",
    "garageName": "Sharma Auto Works",
    "garageAddress": "12 Service Road, Pune",
    "specializations": "Engine,Brakes,AC",
    "certifications": "ASE",
    "rating": 4.5,
    "openFrom": "08:00",
    "openTo": "20:00",
    "workingDays": "MON,TUE,WED,THU,FRI,SAT",
    "services": [
      {
        "id": 1,
        "serviceName": "Full Service",
        "serviceType": "MAINTENANCE",
        "estimatedDurationHours": 3,
        "basePrice": 2500.00,
        "description": "Complete engine and fluid check with oil change"
      }
    ]
  }
]
```

**POST /api/garages/services — Request:**
```json
{
  "serviceName": "AC Gas Refill",
  "serviceType": "MAINTENANCE",
  "estimatedDurationHours": 1,
  "basePrice": 1200.00,
  "description": "Complete AC gas recharge with leak check"
}
```

**PUT /api/garages/hours — Request:**
```json
{
  "openFrom": "09:00",
  "openTo": "21:00",
  "workingDays": "MON,TUE,WED,THU,FRI,SAT,SUN"
}
```

---

### Booking Service (`/api/bookings`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | /api/bookings | CUSTOMER | Create a new service booking |
| GET | /api/bookings/my | CUSTOMER | List own bookings |
| GET | /api/bookings/{id} | CUSTOMER / SUPPLIER / ADMIN | Booking detail with parts |
| DELETE | /api/bookings/{id} | CUSTOMER | Cancel booking |
| POST | /api/bookings/{id}/pay | CUSTOMER | Pay the service bill |
| GET | /api/bookings/garage | SUPPLIER | All bookings for this garage |
| PUT | /api/bookings/{id}/confirm | SUPPLIER | Confirm a pending booking |
| PUT | /api/bookings/{id}/start | SUPPLIER | Mark as IN_PROGRESS (work started) |
| PUT | /api/bookings/{id}/complete | SUPPLIER | Mark as completed, set final amount |
| POST | /api/bookings/{id}/parts | SUPPLIER | Add a part used during service |
| DELETE | /api/bookings/{id}/parts/{partId} | SUPPLIER | Remove a part record |
| GET | /api/bookings/all | ADMIN | All bookings on platform |

**POST /api/bookings — Request:**
```json
{
  "garageId": 1,
  "serviceOfferingId": 1,
  "scheduledDate": "2026-03-26",
  "scheduledTime": "10:00",
  "problemDescription": "Car AC not cooling. Also oil change due."
}
```

**POST /api/bookings — Response 201:**
```json
{
  "id": 9,
  "garageName": "Sharma Auto Works",
  "serviceName": "Full Service",
  "scheduledDate": "2026-03-26",
  "scheduledTime": "10:00",
  "vehicleNumber": "KA01AB1234",
  "vehicleMake": "Honda",
  "vehicleModel": "City",
  "estimatedAmount": 2500.00,
  "status": "PENDING",
  "paymentStatus": "PENDING"
}
```

**PUT /api/bookings/{id}/complete — Request:**
```json
{
  "finalAmount": 3200.00,
  "completionNotes": "Full service done. AC gas recharged. Engine oil changed. Found minor brake pad wear."
}
```

**POST /api/bookings/{id}/parts — Request:**
```json
{
  "partName": "Engine Oil 5W-30",
  "partNumber": "EO5W30-4L",
  "quantity": 4,
  "unitCost": 450.00
}
```

**POST /api/bookings/{id}/pay — Request:**
```json
{
  "cardNumber": "4242424242424242",
  "cardExpiry": "12/26",
  "cardCvv": "321",
  "cardHolderName": "Arjun Nair"
}
```

**POST /api/bookings/{id}/pay — Response 200:**
```json
{
  "transactionId": "txn-uuid-here",
  "status": "SUCCESS",
  "amount": 3200.00,
  "bookingId": 9
}
```

---

### Payment Service (`/api/payments`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | /api/payments/my | CUSTOMER | View own payment history |
| GET | /api/payments/booking/{id} | CUSTOMER / ADMIN | Payment record for a booking |
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

1. A customer cannot book the same garage for the same date and time slot more than once.
2. Booking can only be cancelled when status is `PENDING` or `CONFIRMED`.
3. When a booking is cancelled after payment, the payment status must become `REFUNDED`.
4. Parts can only be added to a booking when its status is `IN_PROGRESS` or `COMPLETED`.
5. The final amount set at completion cannot be less than the total cost of all parts recorded.
6. Payment amount must be `finalAmount` if it has been set; otherwise `estimatedAmount`.
7. Only verified mechanics can have bookings confirmed — unverified garage bookings must be rejected.
8. Mechanics can only confirm, start, complete, and manage parts for bookings in their own garage.
9. Customers can only view and cancel their own bookings.
10. A garage automatically becomes inactive if the mechanic's account is disabled by admin.
11. Booking cannot be confirmed if the scheduled date is in the past.
12. Completed bookings cannot be cancelled.

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
| Registration | 3 | Vehicle owner and Mechanic can self-register; Admin-only endpoint to create Admin accounts |
| Login & JWT | 4 | Login returns valid JWT encoding userId, username, role; expiry enforced |
| Role-Based Access | 5 | ADMIN, CUSTOMER, SUPPLIER roles enforced; wrong-role returns 403 |
| Admin Management | 4 | Admin can list, enable, disable, delete customers and mechanics; can verify mechanics |
| Default Admin Seed | 2 | `DataInitializer` seeds `admin / Admin@123` on first startup; no duplicate on restart |
| Password Security | 2 | Passwords stored as BCrypt hashes; plaintext never returned |

### Section 3 — Garage Service (20 pts)

| Criteria | Points | Description |
|---|---|---|
| Entity & DB | 4 | Garage and ServiceOffering entities with all required fields; MySQL tables auto-created |
| CRUD Endpoints | 6 | All required endpoints functional with correct HTTP methods and status codes |
| Role Enforcement | 4 | Public listing vs SUPPLIER-only management; mechanics manage own garage only |
| Business Rules | 4 | Service offerings correctly linked to garage; working hours stored and retrievable |
| Error Handling | 2 | Structured error responses for not-found, forbidden, bad-request |

### Section 4 — Booking Service (20 pts)

| Criteria | Points | Description |
|---|---|---|
| Entity & DB | 4 | Booking and BookingPart entities with all required fields; MySQL tables auto-created |
| CRUD Endpoints | 6 | All required endpoints functional with correct HTTP methods and status codes |
| Role Enforcement | 4 | Customers see/cancel own; mechanics manage own garage bookings; admin sees all |
| Business Rules | 4 | Verified garage guard; status transitions enforced; final amount ≥ parts total; Feign calls work |
| Error Handling | 2 | Structured error responses for not-found, conflict, bad-request |

### Section 5 — Payment Service (10 pts)

| Criteria | Points | Description |
|---|---|---|
| Mock PSP Logic | 4 | Test card numbers route to correct outcomes |
| Payment Record | 3 | Payment entity persisted with status, amount, card last-4, timestamp |
| Query Endpoints | 3 | Customer views own history; Admin views all |

### Section 6 — Inter-Service Communication (10 pts)

| Criteria | Points | Description |
|---|---|---|
| booking → garage-service | 4 | Feign call to validate garage and service offering, fetch base price on booking creation |
| booking → payment-service | 4 | Feign call to process payment; result drives booking payment status |
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
