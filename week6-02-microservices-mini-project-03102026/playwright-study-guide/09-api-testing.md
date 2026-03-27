# 09 — API Testing with Playwright

## Why Test APIs with Playwright?

Playwright is not just a UI testing tool — it has a built-in **HTTP client** called `APIRequestContext` that lets you send REST API requests directly, without a browser. This enables:

1. **Pure API tests** — test your endpoints in isolation
2. **API setup + UI verification** — create test data via API, verify via UI
3. **UI action + API verification** — click in UI, verify the database state via API
4. **Bypass login via API** — authenticate via API, skip the login form in UI tests

---

## APIRequestContext

`APIRequestContext` is Playwright's HTTP client. It maintains:

- Base URL configuration
- HTTP headers (like `Authorization`)
- Cookies (shared with browser if using `page.request`)

---

## Two Ways to Make API Requests

### Option 1: Standalone — `playwright.request().newContext()`

Use this when your API tests are **independent of the browser** — no UI involved.

```java
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;

import java.util.Map;

// Create a standalone API context
APIRequestContext request = playwright.request().newContext(
    new APIRequest.NewContextOptions()
        .setBaseURL("http://localhost:8080")          // ShopEasy API gateway base URL
        .addExtraHTTPHeaders(Map.of(
            "Content-Type", "application/json",
            "Accept", "application/json"
        ))
);

// Make a GET request
APIResponse response = request.get("/api/products");

System.out.println("Status: " + response.status());       // 200
System.out.println("Body:   " + response.text());         // JSON response body
```

### Option 2: Page-bound — `page.request()`

Use this when you want the API request to **share cookies with the browser** — for example, making an authenticated API call after logging in via the UI.

```java
// The page is already authenticated (logged in via UI)
// page.request() uses the same session cookies
APIResponse response = page.request().get("http://localhost:8080/api/products");
System.out.println("Status: " + response.status());
```

---

## GET Request — Fetch Products

```java
package com.shopeasy.playwright.tests;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.RequestOptions;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class ProductApiTest {

    private Playwright playwright;
    private APIRequestContext request;

    private static final String API_BASE = "http://localhost:8080";

    @BeforeTest
    public void setUp() {
        playwright = Playwright.create();
        request = playwright.request().newContext(
            new APIRequest.NewContextOptions()
                .setBaseURL(API_BASE)
                .addExtraHTTPHeaders(java.util.Map.of(
                    "Content-Type", "application/json"
                ))
        );
    }

    @AfterTest
    public void tearDown() {
        if (request != null) request.dispose();
        if (playwright != null) playwright.close();
    }

    @Test
    public void getAllProducts() {
        // GET /api/products
        APIResponse response = request.get("/api/products");

        // Assert HTTP status
        Assert.assertEquals(response.status(), 200, "Should return 200 OK");

        // Assert content type
        Assert.assertTrue(
            response.headers().get("content-type").contains("application/json"),
            "Response should be JSON"
        );

        // Assert body is not empty
        String body = response.text();
        Assert.assertNotNull(body, "Response body should not be null");
        Assert.assertTrue(body.startsWith("[") || body.startsWith("{"),
            "Body should be JSON");

        System.out.println("Products response: " + body.substring(0, Math.min(200, body.length())));
    }

    @Test
    public void getProductById() {
        // GET /api/products/1
        APIResponse response = request.get("/api/products/1");

        Assert.assertEquals(response.status(), 200, "Product should be found");

        String body = response.text();
        Assert.assertTrue(body.contains("id"), "Response should contain product id");
        Assert.assertTrue(body.contains("name"), "Response should contain product name");
        Assert.assertTrue(body.contains("price"), "Response should contain product price");

        System.out.println("Product 1: " + body);
    }

    @Test
    public void getNonExistentProductReturns404() {
        // GET /api/products/99999
        APIResponse response = request.get("/api/products/99999");

        Assert.assertEquals(response.status(), 404, "Non-existent product should return 404");
    }
}
```

---

## POST Request — Login and Extract Token

```java
@Test
public void loginAndGetToken() {
    // POST /api/users/login with JSON body
    APIResponse response = request.post("/api/users/login",
        RequestOptions.create()
            .setData("""
                {
                    "username": "admin",
                    "password": "admin123"
                }
                """)
    );

    Assert.assertEquals(response.status(), 200, "Login should succeed");

    String body = response.text();
    System.out.println("Login response: " + body);

    // Parse JSON manually (or use Jackson/Gson)
    Assert.assertTrue(body.contains("token") || body.contains("jwt"),
        "Response should contain auth token");
}
```

---

## POST Request with JSON Body — Create Product

```java
import com.microsoft.playwright.options.RequestOptions;

@Test
public void createProduct() {
    // First, get an admin token
    APIResponse loginResponse = request.post("/api/users/login",
        RequestOptions.create()
            .setData("{\"username\":\"admin\",\"password\":\"admin123\"}")
    );
    Assert.assertEquals(loginResponse.status(), 200);

    // Extract token from response (simple string parsing — use Jackson in real projects)
    String loginBody = loginResponse.text();
    // For demo: assume body is {"token":"abc123"}
    String token = extractToken(loginBody);

    // POST /api/products — Create a new product
    APIResponse createResponse = request.post("/api/admin/products",
        RequestOptions.create()
            .setHeader("Authorization", "Bearer " + token)
            .setData("""
                {
                    "name": "Test Product API",
                    "description": "Created via API test",
                    "price": 99.99,
                    "stock": 50,
                    "category": "Electronics"
                }
                """)
    );

    Assert.assertEquals(createResponse.status(), 201, "Product creation should return 201");
    System.out.println("Created product: " + createResponse.text());
}

private String extractToken(String responseBody) {
    // Simple extraction — in real code use Jackson ObjectMapper
    int start = responseBody.indexOf("\"token\":\"") + 9;
    int end   = responseBody.indexOf("\"", start);
    return responseBody.substring(start, end);
}
```

---

## DELETE Request

```java
@Test
public void deleteProduct() {
    // Authenticate first
    APIResponse loginResponse = request.post("/api/users/login",
        RequestOptions.create()
            .setData("{\"username\":\"admin\",\"password\":\"admin123\"}")
    );
    String token = extractToken(loginResponse.text());

    // DELETE /api/admin/products/{id}
    APIResponse deleteResponse = request.delete("/api/admin/products/5",
        RequestOptions.create()
            .setHeader("Authorization", "Bearer " + token)
    );

    Assert.assertEquals(deleteResponse.status(), 200,
        "Delete should return 200 or 204");
}
```

---

## PUT Request — Update Product

```java
@Test
public void updateProductPrice() {
    String token = getAdminToken();

    // PUT /api/admin/products/{id}
    APIResponse updateResponse = request.put("/api/admin/products/1",
        RequestOptions.create()
            .setHeader("Authorization", "Bearer " + token)
            .setData("""
                {
                    "name": "Laptop Pro",
                    "price": 1299.99,
                    "stock": 25
                }
                """)
    );

    Assert.assertEquals(updateResponse.status(), 200, "Update should return 200");
    Assert.assertTrue(updateResponse.text().contains("1299.99"),
        "Updated price should be in response");
}
```

---

## APIResponse Assertions

Playwright has `assertThat()` for API responses too:

```java
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

APIResponse response = request.get("/api/products");

// Status code assertion
assertThat(response).isOK();   // Asserts status is 2xx

// For specific status codes, use TestNG Assert:
Assert.assertEquals(response.status(), 200);
Assert.assertEquals(response.status(), 201);
Assert.assertEquals(response.status(), 404);
```

---

## Combining UI + API in the Same Test

This is one of Playwright's most powerful features. You can use the API for **fast setup/teardown** while using the UI for **user-facing verification**.

### Pattern 1: Login via API, Verify via UI

```java
package com.shopeasy.playwright.tests;

import com.shopeasy.playwright.base.BaseTest;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class HybridTest extends BaseTest {

    @Test
    public void loginViaApiThenVerifyUi() {
        // Step 1: Login via API — much faster than filling the UI form
        APIResponse loginResponse = page.request().post(
            "http://localhost:8080/api/users/login",
            RequestOptions.create()
                .setData("{\"username\":\"admin\",\"password\":\"admin123\"}")
        );
        Assert.assertEquals(loginResponse.status(), 200, "API login should succeed");

        // Step 2: Store token in localStorage so AngularJS picks it up
        String loginBody = loginResponse.text();
        // AngularJS app reads token from localStorage
        page.evaluate(String.format("""
            localStorage.setItem('token', '%s');
            localStorage.setItem('role', 'ADMIN');
            """, extractToken(loginBody)
        ));

        // Step 3: Navigate to admin page — should NOT redirect to login
        navigateTo("admin/dashboard");

        // Step 4: Verify UI shows admin dashboard (not login form)
        assertThat(page).hasURL("**/#!/admin/dashboard");
        assertThat(page.locator(".card.shadow-sm").first()).isVisible();
        System.out.println("Admin dashboard loaded after API login");
    }

    @Test
    public void createProductViaApiThenVerifyInUi() {
        // Step 1: Login via UI (traditional way)
        navigateTo("login");
        page.getByPlaceholder("Enter your username").fill("admin");
        page.getByPlaceholder("Enter your password").fill("admin123");
        page.locator("button[type='submit']").click();
        page.waitForURL("**/#!/admin/dashboard");

        // Step 2: Create a product via API (fast — no modal clicking needed)
        APIResponse createResponse = page.request().post(
            "http://localhost:8080/api/admin/products",
            RequestOptions.create()
                .setData("""
                    {
                        "name": "API Created Product",
                        "description": "Created via API for UI verification",
                        "price": 49.99,
                        "stock": 100,
                        "category": "Electronics"
                    }
                    """)
        );
        Assert.assertEquals(createResponse.status(), 201,
            "Product creation via API should return 201");

        // Step 3: Navigate to admin products page
        navigateTo("admin/products");

        // Step 4: Verify the new product appears in the UI table
        assertThat(page.locator("tbody"))
            .containsText("API Created Product");
        System.out.println("API-created product is visible in admin UI");
    }

    @Test
    public void addToCartViaUiThenVerifyViaApi() {
        // Step 1: Login as customer
        navigateTo("login");
        page.getByPlaceholder("Enter your username").fill("customer1");
        page.getByPlaceholder("Enter your password").fill("pass123");
        page.locator("button[type='submit']").click();
        page.waitForURL("**/#!/products");

        // Step 2: Add a product to cart via UI
        page.locator(".product-card").first().locator("button.btn-primary").click();
        assertThat(page.locator(".alert-success")).isVisible();

        // Step 3: Verify via API that the cart was updated
        APIResponse cartResponse = page.request().get(
            "http://localhost:8080/api/cart"
        );
        Assert.assertEquals(cartResponse.status(), 200, "Cart API should return 200");
        Assert.assertTrue(cartResponse.text().contains("items"),
            "Cart API should return items array");
        System.out.println("Cart API response: " + cartResponse.text());
    }

    private String extractToken(String responseBody) {
        int start = responseBody.indexOf("\"token\":\"") + 9;
        int end   = responseBody.indexOf("\"", start);
        if (start < 9 || end <= start) return "";
        return responseBody.substring(start, end);
    }
}
```

---

## API Test Utilities — Helper Class

For larger projects, extract API helpers into a reusable class:

```java
package com.shopeasy.playwright.util;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;

/**
 * Helper class for ShopEasy REST API operations.
 */
public class ApiHelper {

    private final APIRequestContext request;
    private static final String API_BASE = "http://localhost:8080";

    public ApiHelper(APIRequestContext request) {
        this.request = request;
    }

    public String getAdminToken() {
        APIResponse response = request.post(API_BASE + "/api/users/login",
            RequestOptions.create()
                .setData("{\"username\":\"admin\",\"password\":\"admin123\"}")
        );
        if (response.status() != 200) {
            throw new RuntimeException("Admin login failed: " + response.status());
        }
        return extractField(response.text(), "token");
    }

    public String getCustomerToken(String username, String password) {
        APIResponse response = request.post(API_BASE + "/api/users/login",
            RequestOptions.create()
                .setData(String.format(
                    "{\"username\":\"%s\",\"password\":\"%s\"}", username, password
                ))
        );
        if (response.status() != 200) {
            throw new RuntimeException("Customer login failed: " + response.status());
        }
        return extractField(response.text(), "token");
    }

    public APIResponse createProduct(String token, String name, double price, int stock) {
        return request.post(API_BASE + "/api/admin/products",
            RequestOptions.create()
                .setHeader("Authorization", "Bearer " + token)
                .setData(String.format(
                    "{\"name\":\"%s\",\"price\":%.2f,\"stock\":%d,\"category\":\"Electronics\"}",
                    name, price, stock
                ))
        );
    }

    public APIResponse deleteProduct(String token, int productId) {
        return request.delete(API_BASE + "/api/admin/products/" + productId,
            RequestOptions.create()
                .setHeader("Authorization", "Bearer " + token)
        );
    }

    public APIResponse getProducts() {
        return request.get(API_BASE + "/api/products");
    }

    private String extractField(String json, String field) {
        String search = "\"" + field + "\":\"";
        int start = json.indexOf(search) + search.length();
        int end   = json.indexOf("\"", start);
        if (start < search.length() || end <= start) return "";
        return json.substring(start, end);
    }
}
```

---

## Network Interception — Mock API Responses

Playwright can intercept network requests and return mock responses — useful for testing edge cases without a real server:

```java
// Intercept all requests to the products API and return mock data
page.route("**/api/products", route -> {
    route.fulfill(new Route.FulfillOptions()
        .setContentType("application/json")
        .setStatus(200)
        .setBody("""
            [
                {"id":1, "name":"Mock Product 1", "price":10.00, "stock":5},
                {"id":2, "name":"Mock Product 2", "price":20.00, "stock":10}
            ]
            """)
    );
});

page.navigate("http://localhost:4200/#!/products");
// The UI now receives mock data — actual server not needed!
assertThat(page.getByText("Mock Product 1")).isVisible();

// Intercept and modify a request (add a header)
page.route("**/api/**", route -> {
    route.continue_(new Route.ContinueOptions()
        .setHeaders(java.util.Map.of("X-Test-Header", "playwright"))
    );
});

// Block all image requests (speed up tests)
page.route("**/*.{png,jpg,jpeg,gif,svg}", route -> route.abort());
```
