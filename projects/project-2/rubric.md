# Common Evaluation Rubric — Microservices Mini Project
### Total: 115 Points | Applies to All 5 Projects

---

## Section 1 — Infrastructure & Configuration (15 points)

| Criteria | Points | Description |
|---|---|---|
| Config Server | 3 | Spring Cloud Config Server running on port 8888; all services fetch config from it at startup |
| Eureka Discovery | 3 | Eureka Server on port 8761; all services register and are visible in dashboard |
| API Gateway | 5 | Gateway on port 8080; JWT filter validates token, rejects unauthenticated requests with 401, injects `X-User-Id`, `X-User-Role`, `X-Username` headers downstream |
| Docker Compose | 4 | `docker-compose up -d` starts all services including databases; healthchecks + `depends_on` ordering correct; all services reachable at expected ports |

---

## Section 2 — User Service & Authentication (20 points)

| Criteria | Points | Description |
|---|---|---|
| Registration | 3 | Customer and Supplier can self-register; Admin-only endpoint to create Admin accounts |
| Login & JWT | 4 | Login returns valid JWT; token encodes userId, username, role; expiry enforced |
| Role-Based Access | 5 | ADMIN, CUSTOMER, SUPPLIER roles enforced correctly; wrong-role requests return 403 |
| Admin Management | 4 | Admin can list, enable, disable, delete customers and suppliers |
| Default Admin Seed | 2 | `DataInitializer` seeds `admin / Admin@123` on first startup; does not duplicate on restart |
| Password Security | 2 | Passwords stored as BCrypt hashes; plaintext never returned in responses |

---

## Section 3 — Business Service 1 (20 points)

| Criteria | Points | Description |
|---|---|---|
| Entity & DB | 4 | Correct JPA entity with all required fields; MySQL table auto-created; relationships correct |
| CRUD Endpoints | 6 | All required REST endpoints functional; correct HTTP methods and status codes |
| Role Enforcement | 4 | Public vs authenticated vs role-restricted endpoints work as specified |
| Business Rules | 4 | Domain-specific rules implemented (e.g. stock check, availability check, capacity check) |
| Error Handling | 2 | `@RestControllerAdvice` returns structured error responses for not-found, bad-request, conflict |

---

## Section 4 — Business Service 2 (20 points)

| Criteria | Points | Description |
|---|---|---|
| Entity & DB | 4 | Correct JPA entity with all required fields; MySQL table auto-created; relationships correct |
| CRUD Endpoints | 6 | All required REST endpoints functional; correct HTTP methods and status codes |
| Role Enforcement | 4 | Public vs authenticated vs role-restricted endpoints work as specified |
| Business Rules | 4 | Domain-specific rules implemented; OpenFeign call to Service 1 succeeds and affects state |
| Error Handling | 2 | `@RestControllerAdvice` returns structured error responses for not-found, bad-request, conflict |

---

## Section 5 — Payment Service (10 points)

| Criteria | Points | Description |
|---|---|---|
| Mock PSP Logic | 4 | Test card numbers route to correct outcomes (SUCCESS / DECLINED / INSUFFICIENT_FUNDS / EXPIRED) |
| Payment Record | 3 | Payment entity persisted with status, amount, card last-4, timestamp |
| Query Endpoints | 3 | Customer can view own payment history; Admin can view all payments |

---

## Section 6 — Inter-Service Communication (10 points)

| Criteria | Points | Description |
|---|---|---|
| OpenFeign Client | 4 | At least one Feign client declared; correct service-name routing via Eureka |
| Data Flow | 4 | Feign call result used meaningfully (e.g. fetch price from Service 1 before creating record in Service 2) |
| Fallback Handling | 2 | Graceful error when downstream service is unavailable (circuit-breaker or try-catch with meaningful message) |

---

## Section 7 — Code Quality & Design (5 points)

| Criteria | Points | Description |
|---|---|---|
| DTO Pattern | 2 | Separate `*Request` and `*Response` classes; entities not returned directly from controllers |
| Lombok Usage | 1 | `@RequiredArgsConstructor`, `@Getter`/`@Setter`, `@Builder` used appropriately |
| Clean Code | 2 | No dead code; no hardcoded secrets; reasonable naming conventions followed |

---

## Section 8 — Frontend Application (15 points)

> Implemented using **AngularJS 1.x (1.8.3)**. Refer to `frontend-skeleton.md` for the common project structure and coding conventions.

| Criteria | Points | Description |
|---|---|---|
| Auth Flow | 3 | Login and Register work for both roles (Customer + Supplier); JWT stored in `localStorage`; role-based redirect after login; route guards redirect unauthenticated users to `#!/login` |
| Customer Flows | 4 | Core customer journeys functional end-to-end: browse listings → create transaction record → pay via card form |
| Supplier Flows | 3 | Core supplier journeys functional: manage own listings/profile → view and update associated transactions |
| Admin Dashboard | 2 | Tabbed dashboard to list, enable/disable/delete customers and suppliers; project-specific approval/verification action works |
| UI Quality | 3 | Correct AngularJS 1.x patterns (`ng-repeat`, `ng-if`, `ng-model`, `$http`); meaningful loading/error states; no hardcoded credentials or API tokens in JS |

---

## Grading Scale

| Score | Grade |
|---|---|
| 104 – 115 | O (Outstanding) |
| 92 – 103 | A+ |
| 80 – 91 | A |
| 69 – 79 | B |
| 57 – 68 | C |
| < 57 | F |

---

## Deductions

- Service fails to start: **−5 per service**
- Hardcoded JWT secret or DB password in source (not in config): **−3**
- SQL/NoSQL injection vulnerability in a query: **−5**
- Admin endpoint accessible without ADMIN role: **−5**
- Plaintext passwords in DB or responses: **−5**
- Frontend does not compile / load in browser: **−5**
