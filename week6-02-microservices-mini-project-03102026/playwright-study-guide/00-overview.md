# Playwright (Java) Study Guide — ShopEasy E-Commerce Application

## What You Will Learn

This guide teaches Playwright for Java using the **ShopEasy** AngularJS 1.x frontend that is part of this microservices project. Every concept is demonstrated with real selectors and real pages from the app — not toy examples.

---

## Application Reference

| Detail | Value |
|---|---|
| App Name | ShopEasy |
| Page Title | `ShopEasy - E-Commerce` |
| Frontend Tech | AngularJS 1.x (Hash routing: `#!/route`) |
| Frontend Port | `http://localhost:4200` (or as configured in docker-compose) |
| API Gateway | `http://localhost:8080/api` |
| CSS Framework | Bootstrap 5.3.0 |

### Pages in the Application

| URL | Description | Role |
|---|---|---|
| `http://localhost:4200/#!/products` | Product catalog with search & filter | Public |
| `http://localhost:4200/#!/login` | Username + Password login form | Public |
| `http://localhost:4200/#!/register` | New user registration form | Public |
| `http://localhost:4200/#!/cart` | Shopping cart + checkout + payment | Customer |
| `http://localhost:4200/#!/orders` | Customer order history + details modal | Customer |
| `http://localhost:4200/#!/payments` | Payment history table | Customer |
| `http://localhost:4200/#!/admin/dashboard` | Admin dashboard with stats | Admin |
| `http://localhost:4200/#!/admin/products` | Product CRUD table + modal | Admin |
| `http://localhost:4200/#!/admin/customers` | Customer management table | Admin |
| `http://localhost:4200/#!/admin/orders` | All orders view | Admin |
| `http://localhost:4200/#!/admin/admins` | Admin account management | Admin |

---

## Study Guide Navigation

| # | File | Topics Covered |
|---|---|---|
| 01 | [01-setup.md](01-setup.md) | What is Playwright, Playwright vs Selenium, Maven setup, BaseTest class |
| 02 | [02-browser-context-page.md](02-browser-context-page.md) | Three-layer model, BrowserContext isolation, page navigation, viewport |
| 03 | [03-locators.md](03-locators.md) | All locator strategies: CSS, XPath, getByText, getByRole, getByLabel, chaining |
| 04 | [04-actions.md](04-actions.md) | click, fill, type, selectOption, hover, drag, keyboard — full login + cart examples |
| 05 | [05-assertions.md](05-assertions.md) | assertThat() API, soft assertions, comparison with TestNG Assert |
| 06 | [06-auto-waiting.md](06-auto-waiting.md) | How Playwright auto-waits, waitForURL, waitForResponse, waitForLoadState |
| 07 | [07-page-object-model.md](07-page-object-model.md) | Full POM implementation: LoginPage, ProductsPage, CartPage + test class |
| 08 | [08-screenshots-video.md](08-screenshots-video.md) | Screenshots, video recording, tracing, Trace Viewer, headless config |
| 09 | [09-api-testing.md](09-api-testing.md) | APIRequestContext, REST endpoint testing, UI + API combination tests |
| 10 | [10-interview-questions.md](10-interview-questions.md) | 40+ Interview Questions on Playwright concepts |

---

## Prerequisites

Before starting:

1. **Java 17+** installed — verify: `java -version`
2. **Maven 3.8+** installed — verify: `mvn -version`
3. **Chromium** will be installed by Playwright automatically (see 01-setup.md)
4. **IntelliJ IDEA** or any Java IDE
5. **ShopEasy app running** — start via docker-compose or manually
6. Basic Java knowledge (OOP, collections, exceptions)
7. **Node.js** — only needed if you later use Playwright CLI tools directly

---

## Test Project Structure

Create a separate Maven project `shopeasy-playwright-tests/` alongside the main project:

```
shopeasy-playwright-tests/
├── pom.xml
└── src/
    └── test/
        └── java/
            └── com/
                └── shopeasy/
                    └── playwright/
                        ├── base/
                        │   └── BaseTest.java          ← Playwright setup/teardown
                        ├── pages/                     ← Page Object Model classes
                        │   ├── LoginPage.java
                        │   ├── ProductsPage.java
                        │   ├── CartPage.java
                        │   └── AdminProductsPage.java
                        └── tests/                     ← Actual test classes
                            ├── LoginTest.java
                            ├── ProductTest.java
                            ├── ApiTest.java
                            └── AdminTest.java
```

---

## Key Observations About ShopEasy's HTML

> **Important:** AngularJS 1.x apps often do NOT have explicit `id` attributes on form inputs. ShopEasy uses `ng-model`, `placeholder`, `type`, and CSS classes instead. Playwright's `getByPlaceholder()` and `getByRole()` locators work especially well here.

```
Login form input — no id, no name:
  <input type="text" class="form-control"
         ng-model="loginData.username"
         placeholder="Enter your username" required>

Category dropdown — identified by ng-model:
  <select class="form-select form-select-lg"
          ng-model="selectedCategory" ...>

Product modal — identified by id:
  <div class="modal fade" id="productModal" ...>
```

---

## Test Credentials (for writing tests)

| Role | Username | Password |
|---|---|---|
| Admin | Create via admin panel | — |
| Customer | Register via register page | — |

> Test card numbers: `4242 4242 4242 4242` (success), `4000 0000 0000 0002` (declined)

---

## Playwright vs Selenium — Quick Summary

| Feature | Playwright | Selenium |
|---|---|---|
| Auto-waiting | Built-in (no explicit waits needed) | Manual (WebDriverWait required) |
| Browser support | Chromium, Firefox, WebKit | Chrome, Firefox, Edge, Safari |
| Language support | Java, JS/TS, Python, C#, .NET | Java, JS, Python, C#, Ruby |
| Screenshot / Video | Native API | Requires TakesScreenshot; video needs plugins |
| API testing | Built-in `APIRequestContext` | Not supported |
| Parallel execution | BrowserContext isolation | Separate WebDriver instances |
| Network interception | `page.route()` | Requires proxy setup |
| Trace viewer | Built-in UI for debugging | No equivalent |
