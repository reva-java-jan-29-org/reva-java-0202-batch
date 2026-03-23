# Selenium Study Guide — ShopEasy E-Commerce Application

## What You Will Learn

This guide teaches Selenium WebDriver using the **ShopEasy** AngularJS 1.x frontend that is part of this microservices project. Every concept is demonstrated with real selectors and real pages from the app — not toy examples.

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
| 01 | [01-webdriver-setup.md](01-webdriver-setup.md) | Introduction to Selenium WebDriver, Maven setup, Base test class, First test |
| 02 | [02-locators-basic.md](02-locators-basic.md) | Locators: ID, Name, LinkText, Partial LinkText, TagName |
| 03 | [03-locators-css.md](03-locators-css.md) | Locators: CSS Selectors (tag, class, attribute, pseudo) |
| 04 | [04-locators-xpath.md](04-locators-xpath.md) | Locators: XPath (absolute, relative, predicates, functions) |
| 05 | [05-locators-xpath-axes.md](05-locators-xpath-axes.md) | Locators: XPath Axes (parent, child, sibling, ancestor, descendant) |
| 06 | [06-webdriver-methods.md](06-webdriver-methods.md) | Get methods, Conditional methods, Browser management methods |
| 07 | [07-waits.md](07-waits.md) | Implicit Wait, Explicit Wait, Fluent Wait, Angular-specific waits |
| 08 | [08-browser-navigation.md](08-browser-navigation.md) | navigate(), back/forward/refresh, window sizing |
| 09 | [09-checkboxes-radio.md](09-checkboxes-radio.md) | Handle Checkboxes and Radio Buttons |
| 10 | [10-alerts-frames.md](10-alerts-frames.md) | Handle Alerts (confirm/prompt/alert), Frames & iFrames |
| 11 | [11-dropdowns.md](11-dropdowns.md) | Handle Dropdowns with the Select class |
| 12 | [12-static-web-table.md](12-static-web-table.md) | Read and assert data in static HTML tables |
| 13 | [13-dynamic-pagination-table.md](13-dynamic-pagination-table.md) | Dynamic tables, lazy loading, pagination |
| 14 | [14-date-pickers.md](14-date-pickers.md) | Handle date picker components |
| 15 | [15-mouse-events.md](15-mouse-events.md) | Actions class: hover, drag-drop, double-click, right-click |
| 16 | [16-keyboard-tabs-windows.md](16-keyboard-tabs-windows.md) | Keyboard events, multiple tabs/windows handling |
| 17 | [17-file-upload-scroll-js.md](17-file-upload-scroll-js.md) | File upload, page scrolling, JavascriptExecutor |
| 18 | [18-screenshots-ssl-headless.md](18-screenshots-ssl-headless.md) | Screenshots, SSL, Headless browser, Blocking Ads, Extensions |
| 19 | [19-broken-links-svg-shadow.md](19-broken-links-svg-shadow.md) | Broken links, SVG elements, Shadow DOM |
| 20 | [20-interview-questions.md](20-interview-questions.md) | 40+ Interview Questions with Answers |

---

## Prerequisites

Before starting:

1. **Java 17+** installed — verify: `java -version`
2. **Maven 3.8+** installed — verify: `mvn -version`
3. **Google Chrome** installed (latest stable)
4. **IntelliJ IDEA** or any Java IDE
5. **ShopEasy app running** — start via docker-compose or manually
6. Basic Java knowledge (OOP, collections, exceptions)

---

## Test Project Structure

Create a separate Maven project `shopeasy-selenium-tests/` alongside the main project:

```
shopeasy-selenium-tests/
├── pom.xml
└── src/
    └── test/
        └── java/
            └── com/
                └── shopeasy/
                    └── tests/
                        ├── base/
                        │   └── BaseTest.java          ← WebDriver setup/teardown
                        ├── pages/                     ← Page Object Model classes
                        │   ├── LoginPage.java
                        │   ├── RegisterPage.java
                        │   ├── ProductsPage.java
                        │   ├── CartPage.java
                        │   └── AdminProductsPage.java
                        └── tests/                     ← Actual test classes
                            ├── LoginTest.java
                            ├── ProductTest.java
                            └── AdminTest.java
```

---

## Key Observations About ShopEasy's HTML

> **Important:** AngularJS 1.x apps often do NOT have explicit `id` attributes on form inputs. ShopEasy uses `ng-model`, `placeholder`, `type`, and CSS classes instead. You will primarily use **CSS selectors** and **XPath** as your locating strategies.

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
