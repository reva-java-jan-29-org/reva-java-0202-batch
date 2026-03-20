# Project 1 — MediConnect: Healthcare Clinic Network

## Scenario

MediConnect is a digital platform connecting patients with doctors across a city. Patients can browse available doctors by specialization, book consultation appointments, and pay fees online. Doctors manage their profiles, availability, and appointment statuses. Admins oversee the entire network.

**Roles:**
- **Admin** — manages the platform, users, and oversees all data
- **Patient (Customer)** — registers, books appointments, pays consultation fees
- **Doctor (Supplier)** — maintains profile and availability, manages appointments

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
    │  user-service   │  │  doctor-service  │  │ payment-service  │
    │     :8081       │  │     :8082        │  │     :8085        │
    │                 │  │                  │  │                  │
    │  • Register     │  │  • Doctor        │  │  • Process       │
    │  • Login (JWT)  │  │    profiles      │  │    payments      │
    │  • Admin CRUD   │  │  • Specializations│  │  • Payment       │
    │                 │  │  • Availability  │  │    history       │
    └─────────────────┘  └──────────────────┘  └──────────────────┘
                                   ▲
                        ┌──────────┴───────────┐
                        │  appointment-service  │
                        │        :8083          │
                        │                       │
                        │  • Book appointments  │
                        │  • Manage scheduling  │
                        │  • Trigger payment    │
                        └───────────────────────┘
```

### Service Summary

| Service | Port | Database | Responsibility |
|---|---|---|---|
| config-server | 8888 | — | Stores all service configuration centrally |
| eureka-server | 8761 | — | Service discovery and registration |
| api-gateway | 8080 | — | JWT authentication, request routing |
| user-service | 8081 | `mediconnect_users` | Registration, login, JWT issuance, admin user management |
| doctor-service | 8082 | `mediconnect_doctors` | Doctor profiles, specializations, availability |
| appointment-service | 8083 | `mediconnect_appointments` | Appointment booking, status management, payment |
| payment-service | 8085 | `mediconnect_payments` | Mock payment processing, payment records |

---

## User Roles & Access

| Role | How Created | Default Credentials |
|---|---|---|
| ADMIN | Seeded by `DataInitializer` on first startup | `admin` / `Admin@123` |
| CUSTOMER (Patient) | Self-register at `POST /api/auth/register` with `role: CUSTOMER` | — |
| SUPPLIER (Doctor) | Self-register at `POST /api/auth/register` with `role: SUPPLIER` | — |

The API Gateway reads the role from the JWT and injects it as the `X-User-Role` header. Downstream services use this header for authorization — they never validate the JWT themselves.

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
  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
  │   patients   │ │   doctors    │ │    admins    │
  │  (CUSTOMER)  │ │  (SUPPLIER)  │ │   (ADMIN)    │
  │              │ │              │ │              │
  │ date_of_birth│ │specialization│ │  (no extra   │
  │ blood_group  │ │qualifications│ │   fields)    │
  └──────────────┘ │experience_yrs│ └──────────────┘
                   │consult_fee   │
                   │clinic_address│
                   │available_from│
                   │available_to  │
                   │available_days│
                   └──────────────┘

doctor-service database
─────────────────────────────────────────────────────────────
  ┌──────────────────────────────────────────────────────┐
  │                   doctor_profiles                    │
  │  id | user_id | username | first_name | last_name   │
  │  specialization | qualifications | experience_years │
  │  consultation_fee | clinic_address                  │
  │  available_from | available_to | available_days     │
  │  rating | total_reviews | active | created_at       │
  └──────────────────────────────────────────────────────┘
  (mirrors doctor data from user-service; kept in sync)

appointment-service database
─────────────────────────────────────────────────────────────
  ┌──────────────────────────────────────────────────────┐
  │                    appointments                      │
  │  id | patient_id | patient_name                     │
  │  doctor_id (FK → doctor_profiles.id)                │
  │  doctor_name | specialization                       │
  │  appointment_date | appointment_time                │
  │  symptoms | status | payment_status                 │
  │  consultation_fee | notes | created_at              │
  └──────────────────────────────────────────────────────┘

  status values:   PENDING → CONFIRMED → COMPLETED
                                       ↘ CANCELLED

  payment_status:  PENDING → PAID
                           ↘ REFUNDED (on cancellation after payment)

payment-service database
─────────────────────────────────────────────────────────────
  ┌──────────────────────────────────────────────────────┐
  │                     payments                         │
  │  id | appointment_id | patient_id | amount           │
  │  status | card_last4 | transaction_id | paid_at      │
  └──────────────────────────────────────────────────────┘

  status values: SUCCESS | DECLINED | INSUFFICIENT_FUNDS | EXPIRED
```

### Entity Field Reference

**Patient** (stored in users table with role = CUSTOMER)
| Field | Type | Notes |
|---|---|---|
| id | Long | Primary key |
| username | String | Unique |
| password | String | BCrypt hashed |
| firstName, lastName | String | |
| mobileNumber | String | |
| role | Enum | CUSTOMER |
| accountEnabled | Boolean | Default true |
| dateOfBirth | LocalDate | |
| bloodGroup | String | e.g. "O+" |

**Doctor** (stored in users table with role = SUPPLIER)
| Field | Type | Notes |
|---|---|---|
| specialization | String | e.g. "Cardiology" |
| qualifications | String | e.g. "MBBS, MD" |
| experienceYears | Integer | |
| consultationFee | BigDecimal | |
| clinicAddress | String | |
| availableFrom | LocalTime | e.g. 09:00 |
| availableTo | LocalTime | e.g. 17:00 |
| availableDays | String | e.g. "MON,TUE,WED,THU,FRI" |

**DoctorProfile** (doctor-service, mirrors doctor data)
| Field | Type | Notes |
|---|---|---|
| id | Long | Primary key |
| userId | Long | Refers to user-service doctor id |
| username, firstName, lastName | String | |
| specialization, qualifications | String | |
| experienceYears | Integer | |
| consultationFee | BigDecimal | |
| clinicAddress | String | |
| availableFrom, availableTo | LocalTime | |
| availableDays | String | |
| rating | Double | Default 0.0 |
| totalReviews | Integer | Default 0 |
| active | Boolean | Default true |

**Appointment** (appointment-service)
| Field | Type | Notes |
|---|---|---|
| id | Long | Primary key |
| patientId | Long | From X-User-Id header |
| patientName | String | |
| doctorId | Long | References DoctorProfile.id |
| doctorName, specialization | String | |
| appointmentDate | LocalDate | |
| appointmentTime | LocalTime | |
| symptoms | String | |
| status | Enum | PENDING, CONFIRMED, COMPLETED, CANCELLED |
| paymentStatus | Enum | PENDING, PAID, REFUNDED |
| consultationFee | BigDecimal | Fetched from doctor-service at booking time |
| notes | String | Doctor adds after consultation |

**Payment** (payment-service)
| Field | Type | Notes |
|---|---|---|
| id | Long | Primary key |
| appointmentId | Long | |
| patientId | Long | |
| amount | BigDecimal | |
| status | Enum | SUCCESS, DECLINED, INSUFFICIENT_FUNDS, EXPIRED |
| cardLast4 | String | Last 4 digits only |
| transactionId | String | UUID |
| paidAt | LocalDateTime | |

---

## Inter-Service Communication

```
  SCENARIO 1: Patient Books Appointment
  ──────────────────────────────────────
  Patient                api-gateway         appointment-service      doctor-service
    │                        │                       │                      │
    │── POST /api/appointments ──────────────────────►│                      │
    │                        │  validate JWT          │                      │
    │                        │  inject headers        │                      │
    │                        │                        │── GET /api/doctors/{id} ──►│
    │                        │                        │◄── doctor profile, fee ────│
    │                        │                        │                      │
    │                        │                        │  create appointment  │
    │                        │                        │  with fetched fee    │
    │◄── 201 Appointment Created ────────────────────│                      │


  SCENARIO 2: Patient Pays for Appointment
  ─────────────────────────────────────────
  Patient          api-gateway     appointment-service     payment-service
    │                   │                  │                     │
    │── POST /api/appointments/{id}/pay ──►│                     │
    │                   │  validate JWT    │                     │
    │                   │  inject headers  │                     │
    │                   │                  │── POST /api/payments/process ──►│
    │                   │                  │                     │  process card
    │                   │                  │◄── payment result ──│
    │                   │                  │                     │
    │                   │                  │  update paymentStatus│
    │◄── 200 Payment Result ──────────────│                     │


  INTER-SERVICE RULES:
  • appointment-service calls doctor-service when a booking is created
    → to validate the doctorId exists
    → to fetch the current consultationFee
  • appointment-service calls payment-service when patient pays
    → passes amount, card details, appointmentId
    → updates appointment paymentStatus based on result
  • All inter-service calls go through service names (Eureka discovery),
    NOT hardcoded URLs
```

---

## REST API Specification

### Auth Service (`/api/auth`) — No Authentication Required

#### POST /api/auth/register
Register as a patient or doctor.

**Patient Request:**
```json
{
  "username": "patient_raj",
  "password": "pass@123",
  "firstName": "Raj",
  "lastName": "Kumar",
  "mobileNumber": "9876543210",
  "role": "CUSTOMER",
  "dateOfBirth": "1990-05-15",
  "bloodGroup": "O+"
}
```

**Doctor Request:**
```json
{
  "username": "dr_priya",
  "password": "doc@123",
  "firstName": "Priya",
  "lastName": "Sharma",
  "mobileNumber": "9876541111",
  "role": "SUPPLIER",
  "specialization": "Cardiology",
  "qualifications": "MBBS, MD",
  "experienceYears": 8,
  "consultationFee": 500.00,
  "clinicAddress": "12 MG Road, Bangalore",
  "availableFrom": "09:00",
  "availableTo": "17:00",
  "availableDays": "MON,TUE,WED,THU,FRI"
}
```

**Response 201:**
```json
{ "token": "<jwt>", "userId": 2, "role": "CUSTOMER" }
```

#### POST /api/auth/login
```json
{ "username": "admin", "password": "Admin@123" }
```
**Response 200:**
```json
{ "token": "<jwt>", "userId": 1, "role": "ADMIN" }
```

---

### Admin — User Management (`/api/admin`) — ADMIN role required

| Method | Endpoint | Description |
|---|---|---|
| GET | /api/admin/patients | List all patients |
| PUT | /api/admin/patients/{id}/disable | Disable patient account |
| PUT | /api/admin/patients/{id}/enable | Enable patient account |
| DELETE | /api/admin/patients/{id} | Delete patient |
| GET | /api/admin/doctors | List all doctors |
| PUT | /api/admin/doctors/{id}/disable | Disable doctor account |
| PUT | /api/admin/doctors/{id}/enable | Enable doctor account |
| DELETE | /api/admin/doctors/{id} | Delete doctor |
| POST | /api/admin/admins | Create a new admin account |
| GET | /api/admin/admins | List all admins |
| DELETE | /api/admin/admins/{id} | Delete an admin |

---

### Doctor Service (`/api/doctors`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | /api/doctors | Public | List all active doctors |
| GET | /api/doctors/{id} | Public | Get doctor profile by ID |
| GET | /api/doctors/specialization/{spec} | Public | Filter doctors by specialization |
| GET | /api/doctors/search?q= | Public | Search by name or specialization |
| GET | /api/doctors/my-profile | SUPPLIER | Doctor views own profile |
| PUT | /api/doctors/profile | SUPPLIER | Update own profile details |
| PUT | /api/doctors/availability | SUPPLIER | Update available days and hours |

**GET /api/doctors — Sample Response:**
```json
[
  {
    "id": 1,
    "firstName": "Priya",
    "lastName": "Sharma",
    "specialization": "Cardiology",
    "qualifications": "MBBS, MD",
    "experienceYears": 8,
    "consultationFee": 500.00,
    "clinicAddress": "12 MG Road, Bangalore",
    "availableFrom": "09:00",
    "availableTo": "17:00",
    "availableDays": "MON,TUE,WED,THU,FRI",
    "rating": 4.7,
    "totalReviews": 142
  }
]
```

**PUT /api/doctors/profile — Request:**
```json
{
  "consultationFee": 600.00,
  "clinicAddress": "25 Brigade Road, Bangalore",
  "qualifications": "MBBS, MD, DM"
}
```

**PUT /api/doctors/availability — Request:**
```json
{
  "availableFrom": "10:00",
  "availableTo": "18:00",
  "availableDays": "MON,WED,FRI,SAT"
}
```

---

### Appointment Service (`/api/appointments`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | /api/appointments | CUSTOMER | Book an appointment |
| GET | /api/appointments/my | CUSTOMER | List own appointments |
| GET | /api/appointments/{id} | CUSTOMER / SUPPLIER / ADMIN | Get appointment detail |
| DELETE | /api/appointments/{id} | CUSTOMER | Cancel appointment (PENDING status only) |
| GET | /api/appointments/doctor | SUPPLIER | View all appointments assigned to doctor |
| PUT | /api/appointments/{id}/confirm | SUPPLIER | Confirm a pending appointment |
| PUT | /api/appointments/{id}/complete | SUPPLIER | Mark as completed, add clinical notes |
| PUT | /api/appointments/{id}/cancel | SUPPLIER | Cancel an appointment |
| POST | /api/appointments/{id}/pay | CUSTOMER | Pay the consultation fee |
| GET | /api/appointments/all | ADMIN | View all appointments on platform |

**POST /api/appointments — Request:**
```json
{
  "doctorId": 1,
  "appointmentDate": "2026-03-25",
  "appointmentTime": "10:30",
  "symptoms": "Chest pain and breathlessness for 3 days"
}
```

**POST /api/appointments — Response 201:**
```json
{
  "id": 5,
  "doctorName": "Dr. Priya Sharma",
  "specialization": "Cardiology",
  "appointmentDate": "2026-03-25",
  "appointmentTime": "10:30",
  "consultationFee": 500.00,
  "status": "PENDING",
  "paymentStatus": "PENDING"
}
```

**PUT /api/appointments/{id}/complete — Request:**
```json
{
  "notes": "Prescribed beta-blockers. Follow up in 2 weeks."
}
```

**POST /api/appointments/{id}/pay — Request:**
```json
{
  "cardNumber": "4242424242424242",
  "cardExpiry": "12/26",
  "cardCvv": "123",
  "cardHolderName": "Raj Kumar"
}
```

**POST /api/appointments/{id}/pay — Response 200:**
```json
{
  "transactionId": "a3f4c2d1-...",
  "status": "SUCCESS",
  "amount": 500.00,
  "appointmentId": 5
}
```

---

### Payment Service (`/api/payments`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | /api/payments/my | CUSTOMER | View own payment history |
| GET | /api/payments/appointment/{id} | CUSTOMER / ADMIN | Get payment record for an appointment |
| GET | /api/payments/all | ADMIN | View all payments on platform |

**Test Card Numbers for Payment Simulation:**

| Card Number | Result |
|---|---|
| 4242 4242 4242 4242 | SUCCESS |
| 4000 0000 0000 0002 | DECLINED |
| 4000 0000 0000 9995 | INSUFFICIENT_FUNDS |
| 4000 0000 0000 0069 | EXPIRED |

---

## Business Rules

1. A patient cannot book two appointments with the same doctor on the same date and time.
2. An appointment can only be cancelled when its status is `PENDING`.
3. Payment can only be initiated when the appointment status is `CONFIRMED` and payment status is `PENDING`.
4. A doctor can only confirm, complete, or cancel their own appointments.
5. A patient can only view and cancel their own appointments.
6. When an appointment is cancelled after payment, the payment status must update to `REFUNDED`.
7. A doctor's profile must be created in the doctor-service automatically when the doctor registers in user-service (use inter-service communication).
8. If the requested appointment date falls outside the doctor's `availableDays`, the booking must be rejected with a meaningful error.
9. Disabled accounts cannot log in.
10. Completed appointments cannot be cancelled.

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
| Registration | 3 | Patient and Doctor can self-register; Admin-only endpoint to create Admin accounts |
| Login & JWT | 4 | Login returns valid JWT encoding userId, username, role; expiry enforced |
| Role-Based Access | 5 | ADMIN, CUSTOMER, SUPPLIER roles enforced; wrong-role returns 403 |
| Admin Management | 4 | Admin can list, enable, disable, delete patients and doctors |
| Default Admin Seed | 2 | `DataInitializer` seeds `admin / Admin@123` on first startup; no duplicate on restart |
| Password Security | 2 | Passwords stored as BCrypt hashes; plaintext never returned |

### Section 3 — Doctor Service (20 pts)

| Criteria | Points | Description |
|---|---|---|
| Entity & DB | 4 | DoctorProfile entity with all required fields; MySQL table auto-created |
| CRUD Endpoints | 6 | All required endpoints functional with correct HTTP methods and status codes |
| Role Enforcement | 4 | Public listing vs SUPPLIER-only profile management works correctly |
| Business Rules | 4 | Availability validation; doctors can only edit own profile |
| Error Handling | 2 | Structured error responses for not-found, unauthorized, bad-request |

### Section 4 — Appointment Service (20 pts)

| Criteria | Points | Description |
|---|---|---|
| Entity & DB | 4 | Appointment entity with all required fields; MySQL table auto-created |
| CRUD Endpoints | 6 | All required endpoints functional with correct HTTP methods and status codes |
| Role Enforcement | 4 | Patients see/cancel own; doctors see own; admin sees all |
| Business Rules | 4 | Double-booking prevention; status transition validation; Feign call to doctor-service works |
| Error Handling | 2 | Structured error responses for not-found, conflict, bad-request |

### Section 5 — Payment Service (10 pts)

| Criteria | Points | Description |
|---|---|---|
| Mock PSP Logic | 4 | Test card numbers route to correct outcomes |
| Payment Record | 3 | Payment entity persisted with status, amount, card last-4, timestamp |
| Query Endpoints | 3 | Patient views own history; Admin views all |

### Section 6 — Inter-Service Communication (10 pts)

| Criteria | Points | Description |
|---|---|---|
| appointment → doctor-service | 4 | Feign call to fetch doctor info and fee when booking |
| appointment → payment-service | 4 | Feign call to process payment; result updates appointment |
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
