# 02 — Browser, BrowserContext, and Page

## The Three-Layer Model

Playwright uses a **three-layer hierarchy** to organize browser automation. Understanding this model is essential to writing correct, isolated, and efficient tests.

```
Playwright (factory)
    │
    ├── Browser (one Chromium process)
    │       │
    │       ├── BrowserContext A  ← User A's isolated session
    │       │        └── Page 1  ← Tab 1
    │       │        └── Page 2  ← Tab 2
    │       │
    │       └── BrowserContext B  ← User B's isolated session
    │                └── Page 1  ← Tab 1
    │
    └── Browser (one Firefox process)
            └── BrowserContext
                     └── Page
```

---

## Layer 1: Playwright

`Playwright` is the entry point. It provides access to browser type launchers.

```java
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.BrowserType;

// Create the Playwright instance
Playwright playwright = Playwright.create();

// Access browser types
playwright.chromium()   // Chromium (Google Chrome engine)
playwright.firefox()    // Firefox (Gecko engine)
playwright.webkit()     // Safari (WebKit engine)
```

**Lifecycle:** Create once per test run (or test class). Expensive to create.

---

## Layer 2: Browser

`Browser` represents a running browser process. Launch it from a browser type.

```java
// Launch Chromium — headless (no window)
Browser browser = playwright.chromium().launch();

// Launch with options
Browser browserVisible = playwright.chromium().launch(
    new BrowserType.LaunchOptions()
        .setHeadless(false)       // Show the browser window
        .setSlowMo(100)           // Slow down each action by 100ms (for demos)
        .setChannel("chrome")     // Use installed Google Chrome instead of bundled Chromium
);
```

**Lifecycle:** Create once per test suite. One browser process shared across tests via contexts.

---

## Layer 3: BrowserContext

`BrowserContext` is the most important concept in Playwright. It is a **fully isolated browsing session** — its own cookies, localStorage, sessionStorage, and cache. It is equivalent to a separate incognito window in Chrome.

```java
// Create a context with default settings
BrowserContext context = browser.newContext();

// Create a context with custom settings
BrowserContext contextWithOptions = browser.newContext(
    new Browser.NewContextOptions()
        .setViewportSize(1280, 720)
        .setLocale("en-US")
        .setTimezoneId("America/New_York")
        .setIgnoreHTTPSErrors(true)
        .setRecordVideoDir(Paths.get("videos/"))   // Record video
);
```

**Why BrowserContext matters for test isolation:**

```
Test 1: Admin login
    └── BrowserContext A
             ├── Cookie: session=admin-token
             └── Page: http://localhost:4200/#!/admin/dashboard

Test 2: Customer login
    └── BrowserContext B       ← Completely separate — no admin cookie
             ├── Cookie: session=customer-token
             └── Page: http://localhost:4200/#!/cart
```

Without BrowserContext isolation, Test 2 would inherit the admin login from Test 1 — causing incorrect test behavior.

---

## Layer 4: Page

`Page` represents a single browser tab. It is the object you use most in your test code.

```java
// Create a page inside a context
Page page = context.newPage();

// Navigate
page.navigate("http://localhost:4200/#!/login");

// Get page information
String url   = page.url();
String title = page.title();

// Close the page (tab)
page.close();
```

**Lifecycle:** Create one per test method. Each test gets a fresh page inside a fresh context.

---

## BrowserContext vs Browser vs Page — Comparison

| Feature | Browser | BrowserContext | Page |
|---|---|---|---|
| Represents | The browser process | An isolated session | One tab |
| Cookies | Shared across all contexts | Isolated — not shared | Same as context |
| localStorage | Shared across all contexts | Isolated | Same as context |
| Cache | Shared | Isolated | Same as context |
| Heavyweight? | Yes (process) | Lightweight (< 1ms) | Lightweight |
| Analogy | Chrome.exe | Incognito window | One browser tab |
| Create per | Test suite | Test method | Test method |

---

## page.navigate()

Navigate to a URL and wait for the page to reach a "load" state.

```java
// Navigate to an absolute URL
page.navigate("http://localhost:4200/#!/login");

// Navigate with custom wait condition
page.navigate("http://localhost:4200/#!/products",
    new Page.NavigateOptions()
        .setWaitUntil(WaitUntilState.NETWORKIDLE)  // Wait until no network requests for 500ms
);

// Navigate using the helper in BaseTest
navigateTo("products");   // expands to http://localhost:4200/#!/products
navigateTo("admin/dashboard");
```

**WaitUntil options:**

| Option | Meaning |
|---|---|
| `LOAD` | Wait for the `load` event (default) |
| `DOMCONTENTLOADED` | Wait for HTML parsed, not all resources |
| `NETWORKIDLE` | Wait until no network requests for 500ms |
| `COMMIT` | Wait until response headers received |

For ShopEasy (AngularJS SPA), `LOAD` is usually sufficient because AngularJS handles its own async rendering.

---

## page.title() and page.url()

```java
page.navigate("http://localhost:4200/#!/login");

// Get the page title (text in <title> tag)
String title = page.title();
System.out.println(title);  // ShopEasy - E-Commerce

// Get the current URL
String url = page.url();
System.out.println(url);    // http://localhost:4200/#!/login

// After AngularJS navigation (clicking a link), URL updates automatically
page.locator("a[href='#!/products']").click();
System.out.println(page.url());  // http://localhost:4200/#!/products
```

---

## Viewport Settings

The viewport is the visible area of the browser window. Setting it ensures consistent screenshots and test behavior.

```java
// Set viewport when creating context (recommended)
BrowserContext context = browser.newContext(
    new Browser.NewContextOptions()
        .setViewportSize(1280, 720)   // Width x Height in pixels
);

// Or change viewport on an existing page
page.setViewportSize(1920, 1080);

// Common viewport sizes
// 1280x720  — Standard laptop (default in most Playwright tests)
// 1920x1080 — Full HD desktop
// 375x667   — iPhone 6/7/8
// 390x844   — iPhone 12/13/14
```

---

## Device Emulation

Playwright has built-in device descriptors for mobile testing.

```java
import com.microsoft.playwright.options.Geolocation;

// Emulate iPhone 13
BrowserContext mobileContext = browser.newContext(
    playwright.devices().get("iPhone 13")
);

// Emulate iPhone 13 with additional options
BrowserContext mobileContextCustom = browser.newContext(
    new Browser.NewContextOptions()
        .setUserAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X)")
        .setViewportSize(390, 844)
        .setDeviceScaleFactor(3.0)
        .setIsMobile(true)
        .setHasTouch(true)
);

Page mobilePage = mobileContext.newPage();
mobilePage.navigate("http://localhost:4200/#!/products");
// ShopEasy now renders in mobile view (Bootstrap responsive layout)
```

**Available device names** (partial list):

```java
playwright.devices().get("Desktop Chrome")
playwright.devices().get("Desktop Firefox")
playwright.devices().get("iPhone 13")
playwright.devices().get("iPhone 13 Pro Max")
playwright.devices().get("iPad (gen 7)")
playwright.devices().get("Galaxy S9+")
playwright.devices().get("Pixel 5")
```

---

## Creating Multiple Contexts for Parallel Sessions

This is where Playwright shines. You can test **two users simultaneously** in one test:

```java
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.BrowserType;
import org.testng.annotations.Test;

public class MultiUserTest {

    @Test
    public void testAdminAndCustomerSimultaneously() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(false)
            );

            // ── Admin session ──────────────────────────────────────
            BrowserContext adminContext = browser.newContext();
            Page adminPage = adminContext.newPage();
            adminPage.navigate("http://localhost:4200/#!/login");
            adminPage.locator("input[placeholder='Enter your username']").fill("admin");
            adminPage.locator("input[type='password']").fill("admin123");
            adminPage.locator("button[type='submit']").click();
            adminPage.waitForURL("**/#!/admin/dashboard");

            // ── Customer session ───────────────────────────────────
            BrowserContext customerContext = browser.newContext();
            Page customerPage = customerContext.newPage();
            customerPage.navigate("http://localhost:4200/#!/login");
            customerPage.locator("input[placeholder='Enter your username']").fill("customer1");
            customerPage.locator("input[type='password']").fill("pass123");
            customerPage.locator("button[type='submit']").click();
            customerPage.waitForURL("**/#!/products");

            // Both sessions are active simultaneously
            System.out.println("Admin URL: " + adminPage.url());
            System.out.println("Customer URL: " + customerPage.url());

            // Admin adds a product
            adminPage.navigate("http://localhost:4200/#!/admin/products");

            // Customer refreshes product list — sees the new product
            customerPage.reload();

            // Cleanup
            adminContext.close();
            customerContext.close();
            browser.close();
        }
    }
}
```

---

## try-with-resources Pattern

Playwright objects implement `AutoCloseable`. Use try-with-resources for cleaner resource management:

```java
try (Playwright playwright = Playwright.create()) {
    try (Browser browser = playwright.chromium().launch()) {
        try (BrowserContext context = browser.newContext()) {
            Page page = context.newPage();
            page.navigate("http://localhost:4200/#!/login");
            // ... test code ...
        } // context.close() called automatically
    }   // browser.close() called automatically
}       // playwright.close() called automatically
```

> In test classes extending `BaseTest`, the `@AfterMethod` handles cleanup. The try-with-resources pattern is better for one-off utility tests or scripts.

---

## Saving and Restoring Auth State

For tests that require login, re-logging in for every test is slow. Playwright allows you to save the browser state (cookies + localStorage) and reuse it:

```java
import java.nio.file.Paths;

// ── Step 1: Login once and save state ─────────────────────────────
BrowserContext loginContext = browser.newContext();
Page loginPage = loginContext.newPage();
loginPage.navigate("http://localhost:4200/#!/login");
loginPage.locator("input[placeholder='Enter your username']").fill("admin");
loginPage.locator("input[type='password']").fill("admin123");
loginPage.locator("button[type='submit']").click();
loginPage.waitForURL("**/#!/admin/dashboard");

// Save cookies + localStorage to a JSON file
loginContext.storageState(
    new BrowserContext.StorageStateOptions()
        .setPath(Paths.get("auth-state/admin.json"))
);
loginContext.close();

// ── Step 2: Reuse saved state in subsequent tests ─────────────────
BrowserContext reusedContext = browser.newContext(
    new Browser.NewContextOptions()
        .setStorageStatePath(Paths.get("auth-state/admin.json"))
);
Page dashboardPage = reusedContext.newPage();
dashboardPage.navigate("http://localhost:4200/#!/admin/dashboard");
// Already logged in — no login form needed!
```

---

## Page Events

`Page` emits events you can listen to:

```java
// Listen for console messages from the browser
page.onConsoleMessage(msg ->
    System.out.println("Browser console [" + msg.type() + "]: " + msg.text())
);

// Listen for page errors (JavaScript errors)
page.onPageError(error ->
    System.err.println("Page error: " + error)
);

// Listen for new pages opened (popups)
page.onPopup(popup -> {
    System.out.println("New popup URL: " + popup.url());
    popup.close();
});

// Listen for all network requests
page.onRequest(request ->
    System.out.println("Request: " + request.method() + " " + request.url())
);

// Listen for all network responses
page.onResponse(response ->
    System.out.println("Response: " + response.status() + " " + response.url())
);
```

These event listeners are particularly useful for debugging ShopEasy's API calls.
