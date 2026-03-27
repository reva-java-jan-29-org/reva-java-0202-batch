# 10 — Playwright Interview Questions & Answers

## Category 1: Playwright Basics & Architecture

**Q1. What is Playwright and who developed it?**

Playwright is an open-source browser automation framework developed by **Microsoft**, released in 2020. It allows writing tests in Java, TypeScript/JavaScript, Python, and C# that control Chromium, Firefox, and WebKit browsers. Its defining feature is reliable auto-waiting and a rich built-in toolset (tracing, video, API testing) that reduces test flakiness without manual wait code.

---

**Q2. How does Playwright's communication with the browser differ from Selenium?**

| Aspect | Playwright | Selenium |
|---|---|---|
| Protocol | Browser DevTools Protocol (CDP/WebSocket) | W3C WebDriver (HTTP) |
| Connection | Persistent WebSocket | Separate HTTP request per command |
| Speed | Faster (low latency) | Slower (HTTP round-trips) |
| Event listening | Can listen to browser events in real time | Cannot listen to events |
| Browser bundling | Playwright bundles its own browsers | Uses system-installed browsers |

Playwright's WebSocket connection allows it to receive events (network requests, console logs) as they happen, rather than polling.

---

**Q3. What are the four main objects in the Playwright object hierarchy?**

```
Playwright → Browser → BrowserContext → Page
```

- **`Playwright`** — Entry point factory; creates browser types
- **`Browser`** — A running browser process (Chromium/Firefox/WebKit)
- **`BrowserContext`** — An isolated browsing session (own cookies, localStorage)
- **`Page`** — A single browser tab; the primary interaction surface

---

**Q4. What is BrowserContext and why is it important for test isolation?**

`BrowserContext` is a completely isolated browsing session — its own cookies, localStorage, sessionStorage, and cache. It is the equivalent of a fresh incognito window.

**Why important:** Without context isolation, cookies from one test leak into another. If Test 1 logs in as admin, Test 2 would inherit that session. By creating a new `BrowserContext` per test method, each test starts with a clean slate — guaranteed isolation without restarting the browser.

```java
// Each test creates a fresh context — no state from other tests
BrowserContext context = browser.newContext();
Page page = context.newPage();
```

---

**Q5. What is the difference between `browser.newContext()` and `context.newPage()`?**

- `browser.newContext()` — Creates a new isolated session (cookies, storage are separate from all other contexts)
- `context.newPage()` — Creates a new browser tab **within** an existing context (shares cookies with other pages in the same context)

```
browser.newContext()  →  isolated session
    context.newPage()  →  Tab 1 (shares cookies with Tab 2 below)
    context.newPage()  →  Tab 2
```

---

**Q6. How do you run tests against Firefox or Safari (WebKit) with Playwright?**

```java
// Chromium
Browser browser = playwright.chromium().launch();

// Firefox
Browser browser = playwright.firefox().launch();

// WebKit (Safari engine)
Browser browser = playwright.webkit().launch();
```

All three use the same `Browser`, `BrowserContext`, and `Page` API — no test code changes needed.

---

## Category 2: Auto-Waiting

**Q7. What is auto-waiting in Playwright? What does it check before an action?**

Auto-waiting means that every action in Playwright automatically waits for the element to be ready before performing the action. The checks performed (in order):

1. Element is **attached** to the DOM
2. Element is **visible** (not `display:none` or `visibility:hidden`)
3. Element is **stable** (not animating or moving)
4. Element is **enabled** (no `disabled` attribute)
5. Element **receives pointer events** (no other element blocking it)

Only after all checks pass does Playwright perform the action.

---

**Q8. What is the default timeout for actions in Playwright?**

**30,000 ms (30 seconds)**. If an element is not actionable within 30 seconds, Playwright throws a `TimeoutError`.

Override globally on context:
```java
context.setDefaultTimeout(15_000);      // 15 seconds for all actions
context.setDefaultNavigationTimeout(60_000);  // 60 seconds for navigation
```

Override per action:
```java
page.locator("button").click(new Locator.ClickOptions().setTimeout(5_000));
```

---

**Q9. How does Playwright's auto-waiting compare to Selenium's explicit wait?**

| | Playwright auto-wait | Selenium explicit wait |
|---|---|---|
| Code required | None — built into every action | Must write `WebDriverWait` + `ExpectedConditions` |
| What it checks | 5 actionability conditions | Only the one condition you specify |
| Retry on failure | Yes — retries until timeout | Yes — polls until condition or timeout |
| Assertion retrying | `assertThat()` also retries | `Assert.assertEquals()` does not retry |

---

**Q10. When would you use `page.waitForURL()` if auto-waiting already handles most cases?**

`page.waitForURL()` is specifically for waiting after **navigation triggers** — clicking a link or submit button that causes a URL change. Auto-waiting handles element actionability but does NOT wait for the URL to change. You use `waitForURL()` to pause until the SPA hash route has updated:

```java
page.locator("button[type='submit']").click();    // submits login form
page.waitForURL("**/#!/admin/dashboard");          // explicitly wait for redirect
```

---

**Q11. Why is `page.waitForTimeout()` considered bad practice?**

`waitForTimeout()` is a fixed-duration sleep. Problems:
- Wastes time if the element appears faster than the timeout
- Still fails if the element appears slower
- Makes the entire test suite artificially slower
- Masks real performance issues

The correct approach is to use `assertThat(locator).isVisible()` (auto-retries), `page.waitForURL()`, or `page.waitForResponse()` — these wait exactly as long as needed.

---

**Q12. What is the difference between `waitForLoadState(LOAD)` and `waitForLoadState(NETWORKIDLE)`?**

- `LOAD` — Waits for the browser's `load` event (all resources like images, scripts loaded)
- `NETWORKIDLE` — Waits until there are no more network requests for 500ms (good for SPAs that make many async API calls)

For ShopEasy (AngularJS SPA), `NETWORKIDLE` is more reliable after actions that trigger API calls, because `LOAD` fires before AngularJS finishes processing the API response.

---

## Category 3: Locators

**Q13. What is the difference between a Locator in Playwright and a WebElement in Selenium?**

| | Playwright `Locator` | Selenium `WebElement` |
|---|---|---|
| When evaluated | Lazily — at the moment of action | Eagerly — immediately when `findElement()` called |
| DOM re-query | Re-queries on every use | Cached reference — can become stale |
| StaleElement issue | Never — always fresh | `StaleElementReferenceException` possible |
| Multiple matches | Strict mode — throws if >1 match | Returns first match silently |

---

**Q14. What is Playwright's strict mode for locators?**

If a locator expression matches more than one element, calling a single-element action (like `.click()`) throws an error. This prevents accidentally interacting with the wrong element.

```java
// THROWS if multiple "button[type='submit']" exist
page.locator("button[type='submit']").click();

// Fix: be more specific or use .first()
page.locator("form#loginForm button[type='submit']").click();
page.locator("button[type='submit']").first().click();
```

---

**Q15. What is `getByRole()` and why is it preferred?**

`getByRole()` finds elements by their ARIA role — the semantic meaning of the element from an accessibility perspective. It is preferred because:

1. It tests the app the way a screen reader user experiences it
2. It is more robust than CSS selectors — if a class name changes, `getByRole()` still works as long as the element's purpose is the same

```java
page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();
page.getByRole(AriaRole.LINK,   new Page.GetByRoleOptions().setName("Register here")).click();
page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username")).fill("admin");
```

---

**Q16. What does the `filter()` method do on a Locator?**

`filter()` narrows down a list of matching elements by additional criteria — text content or the presence of a nested element. It does not throw in strict mode because it returns a new (possibly smaller) locator list.

```java
// Find the product card for "Laptop Pro" specifically
page.locator(".product-card")
    .filter(new Locator.FilterOptions().setHasText("Laptop Pro"))
    .locator("button.btn-primary")
    .click();
```

---

**Q17. What is the difference between `getByText()` and `getByPlaceholder()`?**

- `getByText("Login")` — finds elements by their **visible text content** (button labels, link text, paragraph text)
- `getByPlaceholder("Enter your username")` — finds **input elements** by their `placeholder` attribute

For ShopEasy (AngularJS without explicit labels or IDs), `getByPlaceholder()` is the primary locator for form inputs.

---

**Q18. How do you handle a locator that matches multiple elements?**

```java
// Get all matching elements
Locator allCards = page.locator(".product-card");

// First element
allCards.first()

// Last element
allCards.last()

// By index (0-based)
allCards.nth(2)

// Iterate over all
List<Locator> cardList = allCards.all();
for (Locator card : cardList) { ... }

// Count
int count = allCards.count();

// Filter by text
allCards.filter(new Locator.FilterOptions().setHasText("Electronics"))
```

---

## Category 4: Assertions

**Q19. How does `assertThat()` differ from TestNG's `Assert`?**

| Feature | Playwright `assertThat()` | TestNG `Assert` |
|---|---|---|
| Auto-retries | Yes — retries until timeout (5s default) | No — evaluates once |
| Element state checks | Built-in (`isVisible`, `hasText`, etc.) | Must call element methods manually |
| Page state checks | Built-in (`hasURL`, `hasTitle`) | Must call `page.url()` manually |
| Failure messages | Descriptive diff | Can customize |
| Async-aware | Yes | No |

---

**Q20. What are soft assertions and when would you use them?**

Soft assertions collect all assertion failures instead of stopping at the first failure. After all assertions run, you call `assertAll()` which throws a combined failure report.

```java
var soft = PlaywrightAssertions.assertThatSoftly();
soft.assertThat(page.locator(".username")).isVisible();
soft.assertThat(page.locator(".password")).isVisible();
soft.assertThat(page.locator(".submit")).isVisible();
soft.assertAll();   // Reports all 3 failures at once if any failed
```

Use soft assertions when verifying multiple independent UI elements on the same page (e.g., page load verification) where you want to know ALL failures, not just the first one.

---

**Q21. How do you assert that an element is NOT visible?**

```java
// Using .not() negation
assertThat(page.locator(".alert-danger")).not().isVisible();

// Or use isHidden() directly
assertThat(page.locator(".alert-danger")).isHidden();
```

Both retry (auto-wait) until the element becomes hidden or timeout expires.

---

## Category 5: Page Object Model

**Q22. How is Playwright POM different from Selenium POM?**

| Aspect | Playwright POM | Selenium POM |
|---|---|---|
| Constructor receives | `Page` | `WebDriver` |
| Locator type stored | `Locator` (lazy) | `By` or `@FindBy` |
| Stale element risk | None — re-queried on every use | Present — must re-findElement after DOM changes |
| Waits in methods | Not needed — actions auto-wait | Often need `WebDriverWait` calls |
| PageFactory | Not applicable | Optional `@FindBy` + `PageFactory.initElements` |

---

**Q23. Why should page object methods return `this` or a new page object?**

Returning `this` (or a new Page Object) enables **fluent method chaining** — readable test code that resembles natural language:

```java
new LoginPage(page)
    .navigate()
    .enterUsername("admin")       // returns this (LoginPage)
    .enterPassword("admin123")    // returns this (LoginPage)
    .loginAsAdmin("admin","admin123")  // void — login happened
// Or:
ProductsPage products = loginPage.loginAsCustomer("user","pass");
CartPage cart = products.addFirstProductToCart().goToCart();
```

---

## Category 6: API Testing

**Q24. What is `APIRequestContext` in Playwright?**

`APIRequestContext` is Playwright's built-in HTTP client. It can:
- Send GET, POST, PUT, DELETE, PATCH requests
- Set headers (including `Authorization`)
- Send JSON body
- Receive and parse responses

It does not require a browser to be open.

```java
APIRequestContext request = playwright.request().newContext(
    new APIRequest.NewContextOptions().setBaseURL("http://localhost:8080")
);
APIResponse response = request.get("/api/products");
```

---

**Q25. What is the difference between `page.request()` and `playwright.request().newContext()`?**

- `page.request()` — Shares cookies with the browser session. Use when you want the API call to be authenticated with the same session as the browser.
- `playwright.request().newContext()` — Fully independent HTTP client. Use for pure API tests that don't involve a browser.

---

**Q26. How would you use Playwright to test that adding a product to the cart via UI is reflected in the API?**

```java
// 1. Login and add product via UI
page.locator(".product-card").first().locator("button.btn-primary").click();
assertThat(page.locator(".alert-success")).isVisible();

// 2. Call the API to get cart — uses same session (cookies) as the browser
APIResponse cartResponse = page.request().get("http://localhost:8080/api/cart");
Assert.assertEquals(cartResponse.status(), 200);
Assert.assertTrue(cartResponse.text().contains("items"));
```

This validates that the UI action actually persisted to the backend.

---

**Q27. What is `page.route()` used for?**

`page.route()` intercepts outgoing network requests from the browser and lets you:
- Return mock responses (useful for testing UI without a real backend)
- Modify requests (add headers, change body)
- Block requests (speed up tests by blocking images/analytics)

```java
// Mock the products API
page.route("**/api/products", route ->
    route.fulfill(new Route.FulfillOptions()
        .setStatus(200)
        .setBody("[{\"id\":1,\"name\":\"Mock Product\",\"price\":9.99}]")
    )
);
```

---

## Category 7: Screenshots, Video, Tracing

**Q28. How do you take a full-page screenshot in Playwright?**

```java
page.screenshot(new Page.ScreenshotOptions()
    .setPath(Paths.get("screenshot.png"))
    .setFullPage(true)   // Captures full scrollable page
);
```

For an element screenshot:
```java
page.locator(".product-card").first()
    .screenshot(new Locator.ScreenshotOptions()
        .setPath(Paths.get("card.png"))
    );
```

---

**Q29. How do you record a video of a test in Playwright?**

Enable video recording when creating `BrowserContext`:

```java
BrowserContext context = browser.newContext(
    new Browser.NewContextOptions()
        .setRecordVideoDir(Paths.get("videos/"))
);
// ... run test ...
context.close();  // Video is written when context closes
String videoPath = page.video().path().toString();
```

---

**Q30. What is the Playwright Trace Viewer and what does it show?**

Trace Viewer is a browser-based debugging tool that replays a saved trace file. A trace captures:

- Screenshot at every action
- Full DOM snapshot at each step
- Network requests and responses
- Console messages
- Action timings and durations

```bash
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI \
    -D exec.args="show-trace test-output/traces/failure.zip"
```

This opens a visual timeline where you can click any action and see exactly what the page looked like at that moment.

---

**Q31. How do you run Playwright tests in headless mode?**

```java
Browser browser = playwright.chromium().launch(
    new BrowserType.LaunchOptions().setHeadless(true)
);
```

Playwright is **headless by default** (unlike Selenium). For CI/CD pipelines, headless mode is the standard. Set `setHeadless(false)` for local debugging.

---

## Category 8: Parallel Execution

**Q32. How do you run Playwright tests in parallel?**

Two strategies:

**Option 1: Multiple BrowserContexts in one test** — two users simultaneously in one test:
```java
BrowserContext adminCtx    = browser.newContext();
BrowserContext customerCtx = browser.newContext();
// Use both concurrently in the same test
```

**Option 2: TestNG parallel test execution** — each test method gets its own thread:

In `testng.xml`:
```xml
<suite name="Playwright" parallel="methods" thread-count="4">
    <test name="ShopEasy">
        <classes>
            <class name="com.shopeasy.playwright.tests.LoginTest"/>
        </classes>
    </test>
</suite>
```

In `BaseTest`, use `ThreadLocal` to keep each thread's `Page` separate:
```java
private static ThreadLocal<Page> threadLocalPage = new ThreadLocal<>();

@BeforeMethod
public void setUp() {
    // ... create playwright, browser, context per thread ...
    threadLocalPage.set(context.newPage());
}

protected Page getPage() {
    return threadLocalPage.get();
}
```

---

**Q33. What is `ThreadLocal` and why is it needed for parallel Playwright tests?**

`ThreadLocal` stores a separate value per thread. In parallel testing, multiple test methods run concurrently on different threads. If all methods share one `page` field, they would interfere with each other. With `ThreadLocal<Page>`, each thread gets its own `Page` instance — complete isolation.

---

## Category 9: Advanced Topics

**Q34. What is the Playwright Inspector?**

The Playwright Inspector is a GUI debugging tool launched with `PWDEBUG=1`. It shows:
- The test execution step by step
- The current locator being used (with highlighting in the browser)
- A console to run locator expressions interactively

```bash
PWDEBUG=1 mvn test -Dtest=LoginTest
```

---

**Q35. How does Playwright handle Shadow DOM?**

Playwright can **pierce Shadow DOM by default** when using CSS selectors — many shadow boundaries are transparent. For direct access:

```java
// Access shadow root
Locator host   = page.locator("custom-element");
Locator shadow = host.locator("css=input"); // Playwright auto-pierces shadow DOM
```

---

**Q36. What is `page.evaluate()` and when would you use it?**

`page.evaluate()` executes JavaScript in the browser context and returns the result.

```java
// Read localStorage (e.g., auth token after login)
Object token = page.evaluate("localStorage.getItem('token')");

// Set a value in localStorage
page.evaluate("localStorage.setItem('theme', 'dark')");

// Scroll to bottom
page.evaluate("window.scrollTo(0, document.body.scrollHeight)");

// Click a hidden element (bypass actionability checks)
page.evaluate("document.querySelector('.hidden-btn').click()");
```

Use it when no built-in Playwright method covers the scenario — similar to `JavascriptExecutor` in Selenium.

---

**Q37. How does device emulation work in Playwright?**

```java
// Use a built-in device descriptor
BrowserContext mobileCtx = browser.newContext(
    playwright.devices().get("iPhone 13")
);
Page mobilePage = mobileCtx.newPage();
mobilePage.navigate("http://localhost:4200/#!/products");
// Playwright sets correct viewport, user-agent, deviceScaleFactor, hasTouch
```

The device descriptors include viewport size, user-agent, device scale factor, and touch support — all set automatically.

---

**Q38. How would you handle a Bootstrap modal (not a JS alert) in Playwright?**

Bootstrap modals are standard DOM elements — no `switchTo()` like in Selenium. Playwright handles them automatically because of auto-waiting for animations:

```java
// Click the button that opens the modal
page.locator("button[data-bs-target='#productModal']").click();

// Auto-wait handles Bootstrap's fade animation (~300ms)
// Fill in the modal fields directly
page.getByPlaceholder("Product name").fill("New Product");
page.getByPlaceholder("Price").fill("99.99");
page.locator("#productModal button[type='submit']").click();
```

---

**Q39. What is the difference between `textContent()` and `innerText()`?**

| Method | Returns | Includes hidden text? |
|---|---|---|
| `textContent()` | All text content of element and descendants | Yes |
| `innerText()` | Only **rendered** (visible) text | No |

```java
// If an element has: <span>Hello <span style="display:none">World</span></span>
String all     = locator.textContent();   // "Hello World"
String visible = locator.innerText();     // "Hello "
```

---

**Q40. How would you save and reuse authentication state across tests?**

```java
// Step 1: Login and save state to a file
BrowserContext loginCtx = browser.newContext();
Page loginPage = loginCtx.newPage();
loginPage.navigate("http://localhost:4200/#!/login");
loginPage.getByPlaceholder("Enter your username").fill("admin");
loginPage.getByPlaceholder("Enter your password").fill("admin123");
loginPage.locator("button[type='submit']").click();
loginPage.waitForURL("**/#!/admin/dashboard");

loginCtx.storageState(
    new BrowserContext.StorageStateOptions()
        .setPath(Paths.get("auth-state/admin.json"))
);
loginCtx.close();

// Step 2: Reuse saved auth state in each test (no login form)
BrowserContext ctx = browser.newContext(
    new Browser.NewContextOptions()
        .setStorageStatePath(Paths.get("auth-state/admin.json"))
);
Page page = ctx.newPage();
page.navigate("http://localhost:4200/#!/admin/dashboard");
// Already logged in — no login form interaction needed
```

This technique reduces test time significantly in suites with many tests that all require login.

---

**Q41. What is `waitForResponse()` and how is it different from auto-waiting?**

Auto-waiting handles **element actionability** (is the element visible and clickable?). `waitForResponse()` handles **network-level completion** — waiting until a specific HTTP response is received.

```java
Response response = page.waitForResponse(
    resp -> resp.url().contains("/api/products") && resp.status() == 200,
    () -> navigateTo("products")   // The action that triggers the request
);
Assert.assertEquals(response.status(), 200);
```

Use `waitForResponse()` when you need to verify the API response itself, or ensure the API call completed before asserting on UI elements that depend on it.

---

**Q42. How do you assert the count of elements in Playwright?**

```java
// Assert exactly 4 product cards visible
assertThat(page.locator(".product-card")).hasCount(4);

// Assert at least 1 (use TestNG Assert for "at least N")
int count = page.locator(".product-card").count();
Assert.assertTrue(count >= 1, "At least one product should be visible");

// Assert after filter — 0 results for unknown search
page.getByPlaceholder("Search products...").fill("zzz_unknown_xyz");
assertThat(page.locator(".product-card")).hasCount(0);
```

---

**Q43. How is Playwright typically integrated into a CI/CD pipeline?**

1. **Headless mode** — default in Playwright (no display needed)
2. **Docker** — use `mcr.microsoft.com/playwright/java:v1.43.0` base image (browsers pre-installed)
3. **GitHub Actions / Jenkins** — run `mvn test` directly
4. **Artifacts** — save `test-output/screenshots/`, `test-output/traces/`, `test-output/videos/` as CI artifacts
5. **Reports** — TestNG HTML report (`target/surefire-reports/`) or Allure report

```yaml
# GitHub Actions example
- name: Run Playwright Tests
  run: mvn test
- name: Upload test artifacts on failure
  if: failure()
  uses: actions/upload-artifact@v3
  with:
    name: playwright-artifacts
    path: |
      test-output/screenshots/
      test-output/traces/
```

---

## Quick Tips for Interviews

1. **Lead with auto-waiting** — The key differentiator: "In Playwright, I don't write wait code because every action waits automatically"
2. **Mention BrowserContext** — Shows you understand test isolation at the right level
3. **Know the locator hierarchy** — CSS/XPath → getByRole → getByPlaceholder — explain when to use each
4. **Mention Trace Viewer** — Demonstrates awareness of production-quality debugging
5. **Discuss API + UI combination** — Shows architectural thinking: "I use API to set up test data, UI to verify user-facing behavior"
6. **Understand the difference from Selenium** — Interviewers often want you to compare; know the protocol difference (CDP vs W3C WebDriver)
