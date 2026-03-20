# Project 5 — StayEase: Hotel & Property Booking Platform

## Scenario

StayEase is an online accommodation booking platform where hotel managers and property owners (Hosts) list their properties with room types and nightly rates. Guests browse properties by city, check availability for their dates, make reservations, and pay online. Room availability is managed automatically — rooms are held only after successful payment, and released on cancellation. Admins manage the platform and verify host accounts before they can publish listings.

**Roles:**
- **Admin** — manages the platform, verifies hosts, approves listings
- **Guest (Customer)** — searches properties, makes reservations, pays online
- **Host (Supplier)** — lists properties, manages room types, handles check-in/check-out

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
    │  user-service   │  │property-service  │  │ payment-service  │
    │     :8081       │  │     :8082        │  │     :8085        │
    │                 │  │                  │  │                  │
    │  • Register     │  │  • Property      │  │  • Process       │
    │  • Login (JWT)  │  │    listings      │  │    payments      │
    │  • Admin CRUD   │  │  • Room types    │  │  • Payment       │
    │  • Host         │  │  • Availability  │  │    history       │
    │    verification │  │  • City search   │  │                  │
    └─────────────────┘  └──────────────────┘  └──────────────────┘
                                   ▲
                        ┌──────────┴───────────┐
                        │  reservation-service  │
                        │        :8083          │
                        │                       │
                        │  • Make reservations  │
                        │  • Availability check │
                        │  • Trigger payment    │
                        │  • Check-in/Check-out │
                        │  • Room mgmt          │
                        └───────────────────────┘
```

### Service Summary

| Service | Port | Database | Responsibility |
|---|---|---|---|
| config-server | 8888 | — | Stores all service configuration centrally |
| eureka-server | 8761 | — | Service discovery and registration |
| api-gateway | 8080 | — | JWT authentication, request routing |
| user-service | 8081 | `stayease_users` | Registration, login, JWT, host verification |
| property-service | 8082 | `stayease_properties` | Property listings, room types, availability counts |
| reservation-service | 8083 | `stayease_reservations` | Reservation lifecycle, check-in/out, payment trigger |
| payment-service | 8085 | `stayease_payments` | Mock payment processing and history |

---

## User Roles & Access

| Role | How Created | Default Credentials |
|---|---|---|
| ADMIN | Seeded by `DataInitializer` on first startup | `admin` / `Admin@123` |
| CUSTOMER (Guest) | Self-register at `POST /api/auth/register` with `role: CUSTOMER` | — |
| SUPPLIER (Host) | Self-register at `POST /api/auth/register` with `role: SUPPLIER` | — |

> **Note:** Hosts must be verified by Admin before they can publish properties. A property must also have at least 1 room type before it can be published.

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
  │    guests    │ │     hosts     │ │    admins    │
  │  (CUSTOMER)  │ │  (SUPPLIER)   │ │   (ADMIN)    │
  │              │ │               │ │              │
  │nationality   │ │business_name  │ │  (no extra   │
  │passport_no   │ │business_type  │ │   fields)    │
  │pref_language │ │business_addr  │ └──────────────┘
  │loyalty_points│ │pan_number     │
  └──────────────┘ │gst_number     │
                   │rating         │
                   │verified       │
                   └───────────────┘

property-service database
─────────────────────────────────────────────────────────────
  ┌──────────────────────────────────────────────────────┐
  │                    properties                        │
  │  id | host_id | host_name | business_name           │
  │  name | description | property_type                 │
  │  address | city | state | pincode                   │
  │  star_rating | check_in_time | check_out_time        │
  │  amenities | image_url | published                  │
  │  rating | total_reviews | created_at                │
  └────────────────────────┬─────────────────────────────┘
                           │ ONE property has MANY room types
                           ▼
  ┌──────────────────────────────────────────────────────┐
  │                    room_types                        │
  │  id | property_id (FK) | type_name | description    │
  │  max_occupancy | total_rooms | available_rooms       │
  │  price_per_night | amenities | image_url            │
  └──────────────────────────────────────────────────────┘

  property_type values: HOTEL | RESORT | GUESTHOUSE | APARTMENT | HOSTEL

  Publish rules:
  • Host must be verified by Admin
  • Property must have at least 1 room type
  • published=false  →  not visible in public search
  • published=true   →  visible, searchable by city and dates

  Room availability:
  available_rooms decrements on successful payment
  available_rooms increments on reservation cancellation

reservation-service database
─────────────────────────────────────────────────────────────
  ┌──────────────────────────────────────────────────────┐
  │                   reservations                       │
  │  id | guest_id | guest_name                         │
  │  property_id | property_name | city                 │
  │  room_type_id | room_type_name                      │
  │  price_per_night | check_in_date | check_out_date   │
  │  number_of_nights | number_of_guests | total_amount │
  │  status | payment_status | special_requests         │
  │  created_at                                          │
  └──────────────────────────────────────────────────────┘

  total_amount = price_per_night × number_of_nights
  number_of_nights = check_out_date - check_in_date (days)

  Status flow:
  PENDING ──confirm──► CONFIRMED ──check-in──► CHECKED_IN ──check-out──► CHECKED_OUT
  PENDING ──cancel──►  CANCELLED
  CONFIRMED ──cancel──► CANCELLED (refund if already paid)

  payment_status: PENDING → PAID → REFUNDED (on cancel after payment)

payment-service database
─────────────────────────────────────────────────────────────
  ┌──────────────────────────────────────────────────────┐
  │                     payments                         │
  │  id | reservation_id | guest_id | amount            │
  │  status | card_last4 | transaction_id | paid_at      │
  └──────────────────────────────────────────────────────┘

  status values: SUCCESS | DECLINED | INSUFFICIENT_FUNDS | EXPIRED
```

### Entity Field Reference

**Guest** (role = CUSTOMER)
| Field | Type | Notes |
|---|---|---|
| id | Long | Primary key |
| username | String | Unique |
| password | String | BCrypt hashed |
| firstName, lastName | String | |
| mobileNumber | String | |
| nationality | String | |
| passportNumber | String | |
| preferredLanguage | String | Default "English" |
| loyaltyPoints | Integer | Default 0 |

**Host** (role = SUPPLIER)
| Field | Type | Notes |
|---|---|---|
| businessName | String | Hotel or property company name |
| businessType | Enum | HOTEL, RESORT, GUESTHOUSE, APARTMENT, HOSTEL |
| businessAddress | String | |
| panNumber | String | Tax identification |
| gstNumber | String | |
| rating | Double | Default 0.0 |
| verified | Boolean | Default false — Admin must verify |

**Property** (property-service)
| Field | Type | Notes |
|---|---|---|
| id | Long | Primary key |
| hostId | Long | From user-service |
| hostName | String | |
| businessName | String | |
| name | String | e.g. "Grand Palace Hotel" |
| description | String | |
| propertyType | Enum | HOTEL, RESORT, GUESTHOUSE, APARTMENT, HOSTEL |
| address | String | |
| city | String | |
| state | String | |
| pincode | String | |
| starRating | Integer | 1–5 |
| checkInTime | LocalTime | e.g. 14:00 |
| checkOutTime | LocalTime | e.g. 11:00 |
| amenities | String | Comma-separated e.g. "WiFi,Pool,Gym,Parking" |
| imageUrl | String | |
| published | Boolean | Default false |
| rating | Double | Default 0.0 |
| totalReviews | Integer | Default 0 |

**RoomType** (child of Property)
| Field | Type | Notes |
|---|---|---|
| id | Long | Primary key |
| propertyId | Long | FK to properties |
| typeName | String | e.g. "Deluxe King", "Standard Twin" |
| description | String | |
| maxOccupancy | Integer | Max guests this room fits |
| totalRooms | Integer | Total physical rooms of this type |
| availableRooms | Integer | Currently available (decrements/increments dynamically) |
| pricePerNight | BigDecimal | |
| amenities | String | Room-specific e.g. "AC,TV,Minibar,Balcony" |
| imageUrl | String | |

**Reservation** (reservation-service)
| Field | Type | Notes |
|---|---|---|
| id | Long | Primary key |
| guestId | Long | From X-User-Id |
| guestName | String | |
| propertyId | Long | |
| propertyName | String | |
| city | String | |
| roomTypeId | Long | |
| roomTypeName | String | |
| pricePerNight | BigDecimal | Snapshot at booking time |
| checkInDate | LocalDate | |
| checkOutDate | LocalDate | |
| numberOfNights | Integer | Computed from dates |
| numberOfGuests | Integer | |
| totalAmount | BigDecimal | pricePerNight × numberOfNights |
| status | Enum | PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED |
| paymentStatus | Enum | PENDING, PAID, REFUNDED |
| specialRequests | String | Guest instructions |

**Payment** (payment-service)
| Field | Type | Notes |
|---|---|---|
| id | Long | Primary key |
| reservationId | Long | |
| guestId | Long | |
| amount | BigDecimal | |
| status | Enum | SUCCESS, DECLINED, INSUFFICIENT_FUNDS, EXPIRED |
| cardLast4 | String | |
| transactionId | String | UUID |
| paidAt | LocalDateTime | |

---

## Inter-Service Communication

```
  SCENARIO 1: Guest Makes a Reservation
  ──────────────────────────────────────
  Guest              api-gateway      reservation-service     property-service
    │                    │                    │                     │
    │── POST /api/reservations ─────────────►│                     │
    │                    │  validate JWT      │                     │
    │                    │  inject headers    │── GET /api/properties/{id} ──►│
    │                    │                   │◄── property detail ───────────│
    │                    │                   │── GET /api/properties/{id}/rooms/{rId} ──►│
    │                    │                   │◄── room type, price, availability ────────│
    │                    │                   │                     │
    │                    │                   │  validate: published?
    │                    │                   │  validate: availableRooms > 0?
    │                    │                   │  validate: maxOccupancy ≥ guests?
    │                    │                   │  validate: checkOut > checkIn?
    │                    │                   │  compute: nights × pricePerNight
    │                    │                   │  create reservation (PENDING)
    │◄── 201 Reservation Created ───────────│                     │
    │    (note: rooms NOT decremented yet)  │                     │


  SCENARIO 2: Guest Pays for Reservation
  ────────────────────────────────────────
  Guest        api-gateway   reservation-service   payment-service   property-service
    │               │                 │                  │                  │
    │── POST /api/reservations/{id}/pay ──────────────────►│                │
    │               │  validate JWT   │                  │                  │
    │               │  inject headers │── POST /api/payments/process ──────►│
    │               │                 │◄── payment result ─────────────────│
    │               │                 │                  │                  │
    │               │                 │  if SUCCESS:     │                  │
    │               │                 │  paymentStatus → PAID               │
    │               │                 │── PUT /api/properties/{id}/rooms/{rId}/decrement ──►│
    │               │                 │   (reduce availableRooms by 1)      │
    │◄── 200 Payment Result ──────────│                  │                  │


  SCENARIO 3: Reservation Cancelled After Payment
  ──────────────────────────────────────────────────
  Guest       api-gateway    reservation-service    property-service
    │              │                 │                    │
    │── DELETE /api/reservations/{id} ──────────────────►│
    │              │  validate JWT   │                    │
    │              │  inject headers │  check cancellation window
    │              │                 │── PUT /api/properties/{id}/rooms/{rId}/increment ──►│
    │              │                 │   (restore availableRooms by 1)      │
    │              │                 │  paymentStatus → REFUNDED             │
    │◄── 200 Reservation Cancelled ──│                    │


  INTER-SERVICE RULES:
  • reservation-service calls property-service on reservation creation
    → to validate propertyId is published
    → to fetch roomType details (maxOccupancy, pricePerNight, availableRooms)
    → price is snapshotted — future price changes do not affect this reservation
  • reservation-service calls payment-service when guest pays
  • On SUCCESS, reservation-service calls property-service to decrement availableRooms
  • On cancellation after payment, reservation-service calls property-service to increment availableRooms
  • All inter-service calls use service names (Eureka discovery), NOT hardcoded URLs
```

---

## REST API Specification

### Auth Service (`/api/auth`) — No Authentication Required

#### POST /api/auth/register
**Guest Request:**
```json
{
  "username": "guest_kavya",
  "password": "travel@123",
  "firstName": "Kavya",
  "lastName": "Singh",
  "mobileNumber": "9876543333",
  "role": "CUSTOMER",
  "nationality": "Indian",
  "passportNumber": "J1234567",
  "preferredLanguage": "English"
}
```
**Host Request:**
```json
{
  "username": "host_rajesh",
  "password": "hotel@123",
  "firstName": "Rajesh",
  "lastName": "Verma",
  "mobileNumber": "9876548888",
  "role": "SUPPLIER",
  "businessName": "Grand Palace Hotels",
  "businessType": "HOTEL",
  "businessAddress": "Marine Drive, Mumbai 400002",
  "panNumber": "ABCDE1234F",
  "gstNumber": "27ABCDE1234F1Z5"
}
```
**Response 201:** `{ "token": "<jwt>", "userId": 5, "role": "CUSTOMER" }`

#### POST /api/auth/login
```json
{ "username": "admin", "password": "Admin@123" }
```
**Response 200:** `{ "token": "<jwt>", "userId": 1, "role": "ADMIN" }`

---

### Admin — User Management (`/api/admin`) — ADMIN role required

| Method | Endpoint | Description |
|---|---|---|
| GET | /api/admin/guests | List all guests |
| PUT | /api/admin/guests/{id}/disable | Disable guest |
| PUT | /api/admin/guests/{id}/enable | Enable guest |
| DELETE | /api/admin/guests/{id} | Delete guest |
| GET | /api/admin/hosts | List all hosts |
| PUT | /api/admin/hosts/{id}/verify | Verify host (allows publishing properties) |
| PUT | /api/admin/hosts/{id}/disable | Disable host |
| DELETE | /api/admin/hosts/{id} | Delete host |
| POST | /api/admin/admins | Create a new admin account |
| GET | /api/admin/admins | List all admins |

---

### Property Service (`/api/properties`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | /api/properties | Public | All published properties |
| GET | /api/properties/{id} | Public | Property detail including all room types |
| GET | /api/properties/city/{city} | Public | Filter published properties by city |
| GET | /api/properties/type/{type} | Public | Filter by property type |
| GET | /api/properties/search?city=&checkIn=&checkOut=&guests= | Public | Search with availability — returns properties having rooms that fit the guest count and have availability |
| GET | /api/properties/my | SUPPLIER | Host's own properties (including unpublished) |
| POST | /api/properties | SUPPLIER | Create a new property listing |
| PUT | /api/properties/{id} | SUPPLIER | Update property details (own only) |
| DELETE | /api/properties/{id} | SUPPLIER | Delete unpublished property (own only) |
| PUT | /api/properties/{id}/publish | SUPPLIER | Publish property (verified host + ≥1 room type required) |
| PUT | /api/properties/{id}/unpublish | SUPPLIER | Unpublish property |
| POST | /api/properties/{id}/rooms | SUPPLIER | Add a room type to the property |
| PUT | /api/properties/{id}/rooms/{rId} | SUPPLIER | Update room type details |
| DELETE | /api/properties/{id}/rooms/{rId} | SUPPLIER | Remove a room type |
| GET | /api/properties/all | ADMIN | All properties including unpublished |

**POST /api/properties — Request:**
```json
{
  "name": "Grand Palace Hotel",
  "description": "Luxury 5-star hotel on Marine Drive with sea-facing rooms",
  "propertyType": "HOTEL",
  "address": "18 Marine Drive",
  "city": "Mumbai",
  "state": "Maharashtra",
  "pincode": "400002",
  "starRating": 5,
  "checkInTime": "14:00",
  "checkOutTime": "11:00",
  "amenities": "WiFi,Pool,Gym,Spa,Restaurant,Parking,RoomService",
  "imageUrl": "https://example.com/hotel.jpg"
}
```

**POST /api/properties — Response 201:**
```json
{
  "id": 2,
  "hostName": "Rajesh Verma",
  "businessName": "Grand Palace Hotels",
  "name": "Grand Palace Hotel",
  "city": "Mumbai",
  "starRating": 5,
  "published": false,
  "roomTypes": []
}
```

**POST /api/properties/{id}/rooms — Request:**
```json
{
  "typeName": "Deluxe Sea View King",
  "description": "King-sized bed with panoramic sea view",
  "maxOccupancy": 2,
  "totalRooms": 20,
  "pricePerNight": 8500.00,
  "amenities": "AC,TV,Minibar,Balcony,Jacuzzi",
  "imageUrl": "https://example.com/deluxe-room.jpg"
}
```

**GET /api/properties/search?city=Mumbai&checkIn=2026-03-25&checkOut=2026-03-28&guests=2 — Response:**
```json
[
  {
    "id": 2,
    "name": "Grand Palace Hotel",
    "city": "Mumbai",
    "starRating": 5,
    "rating": 4.8,
    "checkInTime": "14:00",
    "checkOutTime": "11:00",
    "amenities": "WiFi,Pool,Gym,Spa,Restaurant",
    "availableRoomTypes": [
      {
        "id": 3,
        "typeName": "Deluxe Sea View King",
        "maxOccupancy": 2,
        "availableRooms": 12,
        "pricePerNight": 8500.00
      }
    ]
  }
]
```

---

### Reservation Service (`/api/reservations`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | /api/reservations | CUSTOMER | Make a reservation |
| GET | /api/reservations/my | CUSTOMER | List own reservations |
| GET | /api/reservations/{id} | CUSTOMER / SUPPLIER / ADMIN | Reservation detail |
| DELETE | /api/reservations/{id} | CUSTOMER | Cancel reservation |
| POST | /api/reservations/{id}/pay | CUSTOMER | Pay for the reservation |
| GET | /api/reservations/property | SUPPLIER | All reservations for own properties |
| PUT | /api/reservations/{id}/confirm | SUPPLIER | Confirm a pending reservation |
| PUT | /api/reservations/{id}/checkin | SUPPLIER | Mark guest as checked in |
| PUT | /api/reservations/{id}/checkout | SUPPLIER | Mark guest as checked out |
| GET | /api/reservations/all | ADMIN | All reservations on platform |

**POST /api/reservations — Request:**
```json
{
  "propertyId": 2,
  "roomTypeId": 3,
  "checkInDate": "2026-03-25",
  "checkOutDate": "2026-03-28",
  "numberOfGuests": 2,
  "specialRequests": "Late check-in at 10pm. Sea-facing room preferred."
}
```

**POST /api/reservations — Response 201:**
```json
{
  "id": 11,
  "propertyName": "Grand Palace Hotel",
  "city": "Mumbai",
  "roomTypeName": "Deluxe Sea View King",
  "checkInDate": "2026-03-25",
  "checkOutDate": "2026-03-28",
  "numberOfNights": 3,
  "numberOfGuests": 2,
  "pricePerNight": 8500.00,
  "totalAmount": 25500.00,
  "status": "PENDING",
  "paymentStatus": "PENDING"
}
```

**POST /api/reservations/{id}/pay — Request:**
```json
{
  "cardNumber": "4242424242424242",
  "cardExpiry": "12/26",
  "cardCvv": "456",
  "cardHolderName": "Kavya Singh"
}
```

**POST /api/reservations/{id}/pay — Response 200:**
```json
{
  "transactionId": "txn-uuid-here",
  "status": "SUCCESS",
  "amount": 25500.00,
  "reservationId": 11,
  "propertyName": "Grand Palace Hotel"
}
```

---

### Payment Service (`/api/payments`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | /api/payments/my | CUSTOMER | View own payment history |
| GET | /api/payments/reservation/{id} | CUSTOMER / ADMIN | Payment record for a reservation |
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

1. A guest cannot make a reservation for a room type with `availableRooms = 0`.
2. `numberOfGuests` must not exceed `maxOccupancy` of the selected room type.
3. `checkOutDate` must be strictly after `checkInDate` — minimum stay is 1 night.
4. `totalAmount` = `pricePerNight` × `numberOfNights` where `numberOfNights` = checkOut − checkIn in days.
5. `availableRooms` is decremented only after a successful payment, not when the reservation is created.
6. On cancellation after payment, `paymentStatus` must become `REFUNDED` and `availableRooms` must be restored.
7. Reservation cancellation is only allowed when status is `PENDING` or `CONFIRMED`. Cannot cancel a checked-in or checked-out reservation.
8. Check-in can only be performed on or after the `checkInDate`.
9. Check-out can only be performed when status is `CHECKED_IN`.
10. Only verified hosts can publish properties. A property must have at least 1 room type before publishing.
11. Hosts can only manage (confirm, check-in, check-out) reservations for their own properties.
12. Guests can only view and cancel their own reservations.
13. Published properties with all room types at `availableRooms = 0` should not appear in search results (no availability).
14. Price per night is snapshotted at reservation creation — price changes after that do not affect this reservation.

---

## Frontend Application

### Technology
- **Framework:** AngularJS 1.x (1.8.3)
- **Served at:** `http://localhost:3000`
- **API Base URL:** `http://localhost:8080` (API Gateway)
- **Auth storage:** JWT stored in `localStorage`; sent as `Authorization: Bearer <token>` on every authenticated request

### Folder Structure
```
frontend/
├── index.html
├── app.js
├── services/
│   ├── auth.service.js
│   └── api.service.js
├── controllers/
│   ├── auth.controller.js
│   ├── admin.controller.js
│   ├── guest.controller.js
│   └── host.controller.js
└── views/
    ├── login.html
    ├── register.html
    ├── admin/
    │   └── dashboard.html
    ├── guest/
    │   ├── search.html         ← city, check-in, check-out, guests search
    │   ├── property-detail.html ← room types with availability
    │   ├── reserve.html        ← reservation form
    │   ├── my-reservations.html
    │   └── payment.html
    └── host/
        ├── my-properties.html
        ├── property-form.html  ← create/edit property
        ├── rooms.html          ← add/edit/delete room types
        └── reservations.html   ← list + Confirm/Check-in/Check-out
```

### Pages & Role-Based Navigation

**Guest:**
| Page | Route | Description |
|---|---|---|
| Login | `#!/login` | Redirect by role on success |
| Register | `#!/register` | Toggle: Guest fields vs Host/Business fields |

**Guest (CUSTOMER):**
| Page | Route | Description |
|---|---|---|
| Search | `#!/search` | City, check-in date, check-out date, guest count; shows matching properties with available room types |
| Property Detail | `#!/properties/:id` | Property info, amenities, room type cards with price and availability count |
| Reserve | `#!/properties/:id/reserve` | Pre-filled room type, date range, guests; special requests textarea |
| My Reservations | `#!/reservations` | Table with status, total amount; Pay / Cancel buttons |
| Payment | `#!/reservations/:id/pay` | Card form; shows totalAmount |

**Host (SUPPLIER):**
| Page | Route | Description |
|---|---|---|
| My Properties | `#!/host/properties` | List own properties; Publish / Unpublish / Delete |
| Create/Edit Property | `#!/host/properties/new` | Property details form with amenities and star rating |
| Room Types | `#!/host/properties/:id/rooms` | Add / Edit / Delete room types; shows available vs total rooms |
| Reservations | `#!/host/reservations` | List reservations for own properties; Confirm / Check-in / Check-out |

**Admin:**
| Page | Route | Description |
|---|---|---|
| Admin Dashboard | `#!/admin` | Guests tab + Hosts tab; Verify Host action |

### API Integration Notes
- Decode JWT payload for `role`, `userId` after login
- Route guards redirect unauthenticated users to `#!/login`
- Search page calls `GET /api/properties/search?city=&checkIn=&checkOut=&guests=` and renders results
- Show "Host not yet verified" warning on Host dashboard if not verified

---

## Evaluation Rubric (115 Points)

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
| Registration | 3 | Guest and Host can self-register; Admin-only endpoint to create Admin accounts |
| Login & JWT | 4 | Login returns valid JWT encoding userId, username, role; expiry enforced |
| Role-Based Access | 5 | ADMIN, CUSTOMER, SUPPLIER roles enforced; wrong-role returns 403 |
| Admin Management | 4 | Admin can list, enable, disable, delete guests and hosts; can verify hosts |
| Default Admin Seed | 2 | `DataInitializer` seeds `admin / Admin@123` on first startup; no duplicate on restart |
| Password Security | 2 | Passwords stored as BCrypt hashes; plaintext never returned |

### Section 3 — Property Service (20 pts)

| Criteria | Points | Description |
|---|---|---|
| Entity & DB | 4 | Property and RoomType entities with all required fields; MySQL tables auto-created |
| CRUD Endpoints | 6 | All required endpoints functional; search endpoint filters by city and availability correctly |
| Role Enforcement | 4 | Public listing vs SUPPLIER-only management; hosts manage own properties only |
| Business Rules | 4 | Publish guards (verified host + ≥1 room type); availability count managed correctly |
| Error Handling | 2 | Structured error responses for not-found, forbidden, bad-request |

### Section 4 — Reservation Service (20 pts)

| Criteria | Points | Description |
|---|---|---|
| Entity & DB | 4 | Reservation entity with all required fields; MySQL table auto-created |
| CRUD Endpoints | 6 | All required endpoints functional with correct HTTP methods and status codes |
| Role Enforcement | 4 | Guests see/cancel own; hosts manage own property reservations; admin sees all |
| Business Rules | 4 | Availability check; price snapshot; status transitions; room count management; Feign calls work |
| Error Handling | 2 | Structured error responses for not-found, conflict, bad-request |

### Section 5 — Payment Service (10 pts)

| Criteria | Points | Description |
|---|---|---|
| Mock PSP Logic | 4 | Test card numbers route to correct outcomes |
| Payment Record | 3 | Payment entity persisted with status, amount, card last-4, timestamp |
| Query Endpoints | 3 | Guest views own history; Admin views all |

### Section 6 — Inter-Service Communication (10 pts)

| Criteria | Points | Description |
|---|---|---|
| reservation → property-service | 4 | Feign calls to validate property/room, check availability, fetch price; decrement/increment rooms |
| reservation → payment-service | 4 | Feign call to process payment; result drives reservation payment status |
| Fallback Handling | 2 | Graceful error when downstream service is unavailable |

### Section 7 — Code Quality & Design (5 pts)

| Criteria | Points | Description |
|---|---|---|
| DTO Pattern | 2 | Separate Request and Response classes; entities not returned from controllers |
| Lombok | 1 | Appropriate use of Lombok annotations |
| Clean Code | 2 | No dead code; no hardcoded secrets; sensible naming |

### Section 8 — Frontend Application (15 pts)

| Criteria | Points | Description |
|---|---|---|
| Auth Flow | 3 | Login and Register (Guest + Host) work; JWT stored in localStorage; role-based redirect |
| Guest Flows | 4 | Search properties by city/dates/guests; view room types with availability; make reservation; pay |
| Host Flows | 3 | Create property; add room types; publish/unpublish; confirm reservations and manage check-in/check-out |
| Admin Dashboard | 2 | List guests and hosts; verify host; enable/disable/delete actions |
| UI Quality | 3 | Correct AngularJS 1.x patterns; nights × price displayed in reservation summary; route guards redirect unauthenticated users |

### Grading Scale

| Score | Grade |
|---|---|
| 104–115 | O (Outstanding) |
| 92–103 | A+ |
| 80–91 | A |
| 69–79 | B |
| 57–68 | C |
| < 57 | F |

### Deductions

| Violation | Penalty |
|---|---|
| Any service fails to start | −5 per service |
| Admin endpoint accessible without ADMIN role | −5 |
| Plaintext passwords in DB or API response | −5 |
| Hardcoded secrets in source code | −3 |
| Frontend does not compile / load in browser | −5 |
