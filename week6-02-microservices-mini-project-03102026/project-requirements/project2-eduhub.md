# Project 2 — EduHub: Online Learning Platform

## Scenario

EduHub is an online platform where Instructors publish courses and Students enroll to learn. Instructors create course content with multiple modules. Students browse the catalog, enroll in courses, pay fees, and track their learning progress. Admins manage the platform and approve instructors before they can publish content.

**Roles:**
- **Admin** — manages the platform, approves instructors, oversees all data
- **Student (Customer)** — browses courses, enrolls, pays, tracks progress
- **Instructor (Supplier)** — creates and publishes courses, adds modules, monitors enrollments

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
    │  user-service   │  │  course-service  │  │ payment-service  │
    │     :8081       │  │     :8082        │  │     :8085        │
    │                 │  │                  │  │                  │
    │  • Register     │  │  • Course catalog │  │  • Process       │
    │  • Login (JWT)  │  │  • Course modules │  │    payments      │
    │  • Admin CRUD   │  │  • Publish/unpub  │  │  • Payment       │
    │  • Instructor   │  │                  │  │    history       │
    │    approval     │  └──────────────────┘  └──────────────────┘
    └─────────────────┘           ▲
                        ┌─────────┴────────────┐
                        │  enrollment-service   │
                        │        :8083          │
                        │                       │
                        │  • Enroll in course   │
                        │  • Track progress     │
                        │  • Trigger payment    │
                        │  • Mark completion    │
                        └───────────────────────┘
```

### Service Summary

| Service | Port | Database | Responsibility |
|---|---|---|---|
| config-server | 8888 | — | Stores all service configuration centrally |
| eureka-server | 8761 | — | Service discovery and registration |
| api-gateway | 8080 | — | JWT authentication, request routing |
| user-service | 8081 | `eduhub_users` | Registration, login, JWT, admin + instructor approval |
| course-service | 8082 | `eduhub_courses` | Course catalog, modules, publish lifecycle |
| enrollment-service | 8083 | `eduhub_enrollments` | Enrollment lifecycle, progress, payment trigger |
| payment-service | 8085 | `eduhub_payments` | Mock payment processing and history |

---

## User Roles & Access

| Role | How Created | Default Credentials |
|---|---|---|
| ADMIN | Seeded by `DataInitializer` on first startup | `admin` / `Admin@123` |
| CUSTOMER (Student) | Self-register at `POST /api/auth/register` with `role: CUSTOMER` | — |
| SUPPLIER (Instructor) | Self-register at `POST /api/auth/register` with `role: SUPPLIER` | — |

> **Note:** Instructors must be approved by an Admin before they can publish courses. Unapproved instructors can create courses but not publish them.

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
  │   students   │ │  instructors  │ │    admins    │
  │  (CUSTOMER)  │ │  (SUPPLIER)   │ │   (ADMIN)    │
  │              │ │               │ │              │
  │education_lvl │ │bio            │ │  (no extra   │
  │interests     │ │expertise      │ │   fields)    │
  └──────────────┘ │linkedin_url   │ └──────────────┘
                   │total_students │
                   │total_courses  │
                   │rating         │
                   │approved       │
                   └───────────────┘

course-service database
─────────────────────────────────────────────────────────────
  ┌──────────────────────────────────────────────────────┐
  │                      courses                         │
  │  id | instructor_id | instructor_name | title        │
  │  description | category | level | price              │
  │  discounted_price | duration_hours | language        │
  │  thumbnail_url | published | total_enrollments       │
  │  rating | total_ratings | created_at | updated_at    │
  └────────────────────────┬─────────────────────────────┘
                           │ ONE course has MANY modules
                           ▼
  ┌──────────────────────────────────────────────────────┐
  │                   course_modules                     │
  │  id | course_id (FK) | title | description           │
  │  content_url | duration_minutes | module_order       │
  └──────────────────────────────────────────────────────┘

  Course lifecycle:
  DRAFT (published=false) ──publish──► PUBLISHED (published=true)
                         ◄─unpublish──

  Constraint: course must have ≥ 1 module before publishing
  Constraint: instructor must be approved before publishing

enrollment-service database
─────────────────────────────────────────────────────────────
  ┌──────────────────────────────────────────────────────┐
  │                    enrollments                       │
  │  id | student_id | student_name                     │
  │  course_id | course_title | instructor_name         │
  │  amount_paid | status | enrolled_at                 │
  │  completed_at | progress_percent                    │
  └──────────────────────────────────────────────────────┘

  status values:
  PENDING_PAYMENT ──pay (SUCCESS)──► ACTIVE ──progress=100%──► COMPLETED
  PENDING_PAYMENT ──pay (FAILED)──► PENDING_PAYMENT (unchanged)
  ACTIVE ──────────────────────────────────────────────► REFUNDED

payment-service database
─────────────────────────────────────────────────────────────
  ┌──────────────────────────────────────────────────────┐
  │                     payments                         │
  │  id | enrollment_id | student_id | course_id        │
  │  amount | status | card_last4                        │
  │  transaction_id | paid_at                            │
  └──────────────────────────────────────────────────────┘

  status values: SUCCESS | DECLINED | INSUFFICIENT_FUNDS | EXPIRED
```

### Entity Field Reference

**Student** (role = CUSTOMER)
| Field | Type | Notes |
|---|---|---|
| id | Long | Primary key |
| username | String | Unique |
| password | String | BCrypt hashed |
| firstName, lastName | String | |
| mobileNumber | String | |
| educationLevel | String | e.g. "Undergraduate", "Graduate" |
| interests | String | Comma-separated e.g. "Java,Python,Cloud" |

**Instructor** (role = SUPPLIER)
| Field | Type | Notes |
|---|---|---|
| bio | String | Short professional bio |
| expertise | String | e.g. "Java, Spring Boot, Microservices" |
| linkedinUrl | String | |
| totalStudents | Integer | Default 0 |
| totalCourses | Integer | Default 0 |
| rating | Double | Default 0.0 |
| approved | Boolean | Default false — Admin must approve |

**Course** (course-service)
| Field | Type | Notes |
|---|---|---|
| id | Long | Primary key |
| instructorId | Long | From user-service |
| instructorName | String | |
| title | String | |
| description | String | |
| category | String | e.g. "Programming", "Data Science", "Design" |
| level | Enum | BEGINNER, INTERMEDIATE, ADVANCED |
| price | BigDecimal | |
| discountedPrice | BigDecimal | Optional — shown if present |
| durationHours | Integer | |
| language | String | Default "English" |
| thumbnailUrl | String | |
| published | Boolean | Default false |
| totalEnrollments | Integer | Default 0 |
| rating | Double | Default 0.0 |
| totalRatings | Integer | Default 0 |

**CourseModule** (course-service, child of Course)
| Field | Type | Notes |
|---|---|---|
| id | Long | Primary key |
| courseId | Long | FK to courses |
| title | String | |
| description | String | |
| contentUrl | String | Video/PDF link |
| durationMinutes | Integer | |
| moduleOrder | Integer | Display order |

**Enrollment** (enrollment-service)
| Field | Type | Notes |
|---|---|---|
| id | Long | Primary key |
| studentId | Long | From X-User-Id |
| studentName | String | |
| courseId | Long | |
| courseTitle | String | |
| instructorName | String | |
| amountPaid | BigDecimal | |
| status | Enum | PENDING_PAYMENT, ACTIVE, COMPLETED, REFUNDED |
| progressPercent | Integer | 0–100 |
| enrolledAt | LocalDateTime | |
| completedAt | LocalDateTime | Set when progressPercent = 100 |

**Payment** (payment-service)
| Field | Type | Notes |
|---|---|---|
| id | Long | Primary key |
| enrollmentId | Long | |
| studentId | Long | |
| courseId | Long | |
| amount | BigDecimal | |
| status | Enum | SUCCESS, DECLINED, INSUFFICIENT_FUNDS, EXPIRED |
| cardLast4 | String | |
| transactionId | String | UUID |
| paidAt | LocalDateTime | |

---

## Inter-Service Communication

```
  SCENARIO 1: Student Enrolls in a Course
  ─────────────────────────────────────────
  Student             api-gateway       enrollment-service      course-service
    │                     │                    │                      │
    │── POST /api/enrollments ────────────────►│                      │
    │                     │  validate JWT      │                      │
    │                     │  inject headers    │── GET /api/courses/{id} ──►│
    │                     │                   │◄── course details, price ──│
    │                     │                   │                      │
    │                     │                   │  validate: published?│
    │                     │                   │  validate: not already enrolled?
    │                     │                   │  create enrollment   │
    │                     │                   │  status=PENDING_PAYMENT
    │◄── 201 Enrollment Created ─────────────│                      │


  SCENARIO 2: Student Pays for Enrollment
  ─────────────────────────────────────────
  Student        api-gateway    enrollment-service   payment-service   course-service
    │                │                 │                   │                │
    │── POST /api/enrollments/{id}/pay ►│                   │                │
    │                │  validate JWT   │                   │                │
    │                │  inject headers │── POST /api/payments/process ──────►│
    │                │                 │◄── payment result ─────────────────│
    │                │                 │                   │                │
    │                │                 │  if SUCCESS:       │                │
    │                │                 │  status → ACTIVE   │                │
    │                │                 │── PUT /api/courses/{id}/increment ──►│
    │                │                 │   (increment total_enrollments)     │
    │◄── 200 Payment Result ──────────│                   │                │


  INTER-SERVICE RULES:
  • enrollment-service calls course-service on enrollment creation
    → to validate courseId exists and is published
    → to fetch current price (discountedPrice if present, else price)
  • enrollment-service calls payment-service when student pays
    → passes amount, card details, enrollmentId, courseId
  • On SUCCESS, enrollment-service calls course-service again
    → to increment totalEnrollments count on the course
  • All inter-service calls use service names (Eureka discovery),
    NOT hardcoded IP addresses or ports
```

---

## REST API Specification

### Auth Service (`/api/auth`) — No Authentication Required

#### POST /api/auth/register
**Student Request:**
```json
{
  "username": "student_asha",
  "password": "learn@123",
  "firstName": "Asha",
  "lastName": "Patel",
  "mobileNumber": "9876541234",
  "role": "CUSTOMER",
  "educationLevel": "Undergraduate",
  "interests": "Java,Spring,Cloud"
}
```
**Instructor Request:**
```json
{
  "username": "instructor_ravi",
  "password": "teach@123",
  "firstName": "Ravi",
  "lastName": "Mehta",
  "mobileNumber": "9876549999",
  "role": "SUPPLIER",
  "bio": "10+ years Java developer and certified trainer",
  "expertise": "Java, Spring Boot, Microservices",
  "linkedinUrl": "https://linkedin.com/in/ravimehta"
}
```
**Response 201:** `{ "token": "<jwt>", "userId": 2, "role": "CUSTOMER" }`

#### POST /api/auth/login
```json
{ "username": "admin", "password": "Admin@123" }
```
**Response 200:** `{ "token": "<jwt>", "userId": 1, "role": "ADMIN" }`

---

### Admin — User Management (`/api/admin`) — ADMIN role required

| Method | Endpoint | Description |
|---|---|---|
| GET | /api/admin/students | List all students |
| PUT | /api/admin/students/{id}/disable | Disable student account |
| PUT | /api/admin/students/{id}/enable | Enable student account |
| DELETE | /api/admin/students/{id} | Delete student |
| GET | /api/admin/instructors | List all instructors |
| PUT | /api/admin/instructors/{id}/approve | Approve instructor (allows publishing) |
| PUT | /api/admin/instructors/{id}/disable | Disable instructor account |
| DELETE | /api/admin/instructors/{id} | Delete instructor |
| POST | /api/admin/admins | Create a new admin account |
| GET | /api/admin/admins | List all admins |

---

### Course Service (`/api/courses`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | /api/courses | Public | List all published courses |
| GET | /api/courses/{id} | Public | Course detail including modules list |
| GET | /api/courses/category/{cat} | Public | Filter by category |
| GET | /api/courses/search?q= | Public | Search by title or instructor name |
| GET | /api/courses/my | SUPPLIER | Instructor's own courses (all, including drafts) |
| POST | /api/courses | SUPPLIER | Create a new course (starts as draft) |
| PUT | /api/courses/{id} | SUPPLIER | Update course details (own only) |
| DELETE | /api/courses/{id} | SUPPLIER | Delete a draft course (own only) |
| PUT | /api/courses/{id}/publish | SUPPLIER | Publish course (must have ≥1 module, must be approved) |
| PUT | /api/courses/{id}/unpublish | SUPPLIER | Unpublish course |
| POST | /api/courses/{id}/modules | SUPPLIER | Add a module to the course |
| PUT | /api/courses/{id}/modules/{mId} | SUPPLIER | Update a module |
| DELETE | /api/courses/{id}/modules/{mId} | SUPPLIER | Remove a module |
| GET | /api/courses/all | ADMIN | All courses including drafts |

**POST /api/courses — Request:**
```json
{
  "title": "Spring Boot Microservices Masterclass",
  "description": "Learn to build production-grade microservices with Spring Boot 3",
  "category": "Programming",
  "level": "INTERMEDIATE",
  "price": 2999.00,
  "discountedPrice": 1999.00,
  "durationHours": 40,
  "language": "English",
  "thumbnailUrl": "https://example.com/thumb.jpg"
}
```

**POST /api/courses — Response 201:**
```json
{
  "id": 3,
  "instructorName": "Ravi Mehta",
  "title": "Spring Boot Microservices Masterclass",
  "category": "Programming",
  "level": "INTERMEDIATE",
  "price": 2999.00,
  "discountedPrice": 1999.00,
  "durationHours": 40,
  "published": false,
  "totalEnrollments": 0
}
```

**GET /api/courses — Sample Response:**
```json
[
  {
    "id": 1,
    "instructorName": "Ravi Mehta",
    "title": "Java Fundamentals",
    "category": "Programming",
    "level": "BEGINNER",
    "price": 999.00,
    "discountedPrice": 499.00,
    "durationHours": 20,
    "totalEnrollments": 312,
    "rating": 4.6
  }
]
```

**POST /api/courses/{id}/modules — Request:**
```json
{
  "title": "Introduction to Spring Boot",
  "description": "Setup, annotations, and your first REST API",
  "contentUrl": "https://cdn.eduhub.com/videos/module1.mp4",
  "durationMinutes": 45,
  "moduleOrder": 1
}
```

---

### Enrollment Service (`/api/enrollments`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | /api/enrollments | CUSTOMER | Enroll in a course |
| GET | /api/enrollments/my | CUSTOMER | List own enrollments |
| GET | /api/enrollments/{id} | CUSTOMER / ADMIN | Enrollment detail |
| POST | /api/enrollments/{id}/pay | CUSTOMER | Pay the course fee |
| PUT | /api/enrollments/{id}/progress | CUSTOMER | Update learning progress |
| PUT | /api/enrollments/{id}/complete | CUSTOMER | Mark course as completed |
| GET | /api/enrollments/course/{courseId} | SUPPLIER / ADMIN | All enrollments for a specific course |
| GET | /api/enrollments/all | ADMIN | All enrollments on platform |

**POST /api/enrollments — Request:**
```json
{ "courseId": 3 }
```

**POST /api/enrollments — Response 201:**
```json
{
  "id": 7,
  "courseId": 3,
  "courseTitle": "Spring Boot Microservices Masterclass",
  "instructorName": "Ravi Mehta",
  "amountPaid": 0,
  "status": "PENDING_PAYMENT",
  "progressPercent": 0
}
```

**POST /api/enrollments/{id}/pay — Request:**
```json
{
  "cardNumber": "4242424242424242",
  "cardExpiry": "12/26",
  "cardCvv": "456",
  "cardHolderName": "Asha Patel"
}
```

**POST /api/enrollments/{id}/pay — Response 200:**
```json
{
  "transactionId": "txn-uuid-here",
  "status": "SUCCESS",
  "amount": 1999.00,
  "enrollmentId": 7,
  "courseTitle": "Spring Boot Microservices Masterclass"
}
```

**PUT /api/enrollments/{id}/progress — Request:**
```json
{ "progressPercent": 45 }
```

---

### Payment Service (`/api/payments`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | /api/payments/my | CUSTOMER | View own payment history |
| GET | /api/payments/enrollment/{id} | CUSTOMER / ADMIN | Payment record for an enrollment |
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

1. A student cannot enroll in the same course twice if they already have an ACTIVE or COMPLETED enrollment.
2. Only published courses can be enrolled in — enrollment-service must reject requests for draft courses.
3. A course can only be published if it has at least 1 module.
4. An instructor must be approved by Admin before they can publish any course.
5. A published course cannot be deleted — the instructor must unpublish it first.
6. An instructor can only edit, delete, publish, or unpublish their own courses.
7. Progress can only be updated for enrollments with status ACTIVE.
8. When `progressPercent` reaches 100, the enrollment status must automatically change to COMPLETED and `completedAt` must be recorded.
9. Payment can only be initiated for enrollments in PENDING_PAYMENT status.
10. On failed payment, the enrollment status must remain PENDING_PAYMENT.
11. Students can access course modules only after enrollment status is ACTIVE.
12. Disabled accounts cannot log in or perform any actions.

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
| Registration | 3 | Student and Instructor can self-register; Admin-only endpoint to create Admin accounts |
| Login & JWT | 4 | Login returns valid JWT encoding userId, username, role; expiry enforced |
| Role-Based Access | 5 | ADMIN, CUSTOMER, SUPPLIER roles enforced; wrong-role returns 403 |
| Admin Management | 4 | Admin can list, enable, disable, delete students and instructors; can approve instructors |
| Default Admin Seed | 2 | `DataInitializer` seeds `admin / Admin@123` on first startup; no duplicate on restart |
| Password Security | 2 | Passwords stored as BCrypt hashes; plaintext never returned |

### Section 3 — Course Service (20 pts)

| Criteria | Points | Description |
|---|---|---|
| Entity & DB | 4 | Course and CourseModule entities with all required fields; MySQL tables auto-created |
| CRUD Endpoints | 6 | All required endpoints functional with correct HTTP methods and status codes |
| Role Enforcement | 4 | Public listing vs SUPPLIER-only management; instructors can only manage own courses |
| Business Rules | 4 | Publish guards (approved + ≥1 module); cannot delete published course |
| Error Handling | 2 | Structured error responses for not-found, forbidden, bad-request |

### Section 4 — Enrollment Service (20 pts)

| Criteria | Points | Description |
|---|---|---|
| Entity & DB | 4 | Enrollment entity with all required fields; MySQL table auto-created |
| CRUD Endpoints | 6 | All required endpoints functional with correct HTTP methods and status codes |
| Role Enforcement | 4 | Students see/update own enrollments; instructors see course enrollments; admin sees all |
| Business Rules | 4 | Duplicate enrollment prevention; status transitions; auto-complete at 100% progress; Feign calls work |
| Error Handling | 2 | Structured error responses for not-found, conflict, bad-request |

### Section 5 — Payment Service (10 pts)

| Criteria | Points | Description |
|---|---|---|
| Mock PSP Logic | 4 | Test card numbers route to correct outcomes |
| Payment Record | 3 | Payment entity persisted with status, amount, card last-4, timestamp |
| Query Endpoints | 3 | Student views own history; Admin views all |

### Section 6 — Inter-Service Communication (10 pts)

| Criteria | Points | Description |
|---|---|---|
| enrollment → course-service | 4 | Feign call to validate course and fetch price on enrollment; increment count on payment success |
| enrollment → payment-service | 4 | Feign call to process payment; result drives enrollment status update |
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
