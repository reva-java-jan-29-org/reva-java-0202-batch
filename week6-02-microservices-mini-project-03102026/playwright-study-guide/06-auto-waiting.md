# 06 — Auto-Waiting

## The Problem Playwright Solves

In Selenium, you must manually add waits before every interaction with a dynamic element:

```java
// Selenium — manual waiting required
WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
WebElement loginBtn = wait.until(
    ExpectedConditions.elementToBeClickable(By.cssSelector("button[type='submit']"))
);
loginBtn.click();
```

In Playwright, **every action automatically waits** for the element to be ready before performing the action:

```java
// Playwright — no wait code needed
page.locator("button[type='submit']").click();  // Automatically waits until clickable
```

---

## How Playwright Auto-Waits

When you call an action like `.click()`, `.fill()`, or `.check()`, Playwright performs a series of **actionability checks** in sequence:

```
Action called (e.g., locator.click())
        │
        ▼
1. Is the element attached to DOM?  ── NO ──► Retry until timeout
        │ YES
        ▼
2. Is the element visible?          ── NO ──► Retry until timeout
        │ YES
        ▼
3. Is the element stable?           ── NO ──► Retry until timeout
   (not animating / not moving)
        │ YES
        ▼
4. Is the element enabled?          ── NO ──► Retry until timeout
   (not disabled attribute)
        │ YES
        ▼
5. Scroll element into viewport
        │
        ▼
6. Receive pointer events?          ── NO ──► Retry until timeout
   (no other element intercepting)
        │ YES
        ▼
7. Perform the action  ✓
```

**Default timeout:** 30,000 ms (30 seconds). If the element is not actionable within 30 seconds, Playwright throws a `TimeoutError`.

---

## Actionability Checks Per Action

Not all actions require all checks. Here is what each action waits for:

| Action | Attached | Visible | Stable | Enabled | Receives Events |
|---|---|---|---|---|---|
| `click()` | Yes | Yes | Yes | Yes | Yes |
| `fill()` | Yes | Yes | Yes | Yes | Yes |
| `check()` | Yes | Yes | Yes | Yes | Yes |
| `hover()` | Yes | Yes | Yes | — | Yes |
| `textContent()` | Yes | — | — | — | — |
| `getAttribute()` | Yes | — | — | — | — |
| `isVisible()` | — | — | — | — | — |

> `textContent()` and `getAttribute()` do NOT wait for visibility — they only wait for the element to be in the DOM.

---

## Auto-Wait vs Selenium Explicit Wait — Comparison

| Scenario | Selenium Approach | Playwright Approach |
|---|---|---|
| Wait for element to appear | `WebDriverWait` + `visibilityOfElementLocated` | Automatic — just call `.click()` |
| Wait for URL to change | `WebDriverWait` + `urlContains()` | `page.waitForURL("**/route")` |
| Wait for spinner to disappear | `WebDriverWait` + `invisibilityOfElement` | Automatic — next action waits anyway |
| Wait for network request | No built-in support | `page.waitForResponse(...)` |
| Wait for specific text | `WebDriverWait` + `textToBePresentInElement` | `assertThat(locator).containsText()` — auto-retries |
| Wait for element count | `WebDriverWait` + `numberOfElementsToBeMoreThan` | `assertThat(locator).hasCount(n)` — auto-retries |

---

## waitForURL()

Waits for the page URL to match a pattern. Essential for SPA navigation (AngularJS hash routing).

```java
import com.microsoft.playwright.Page;

// Wait for exact URL
page.waitForURL("http://localhost:4200/#!/admin/dashboard");

// Wait for URL pattern (* = wildcard for single segment)
page.waitForURL("**/#!/admin/dashboard");

// Wait for URL containing a fragment
page.waitForURL("**/#!/products");
page.waitForURL("**/#!/cart");
page.waitForURL("**/#!/orders");

// With timeout override (default is 30s)
page.waitForURL("**/#!/admin/dashboard",
    new Page.WaitForURLOptions().setTimeout(15_000)
);

// Complete login and wait for redirect
page.locator("button[type='submit']").click();
page.waitForURL("**/#!/admin/dashboard");
System.out.println("Redirected to: " + page.url());
```

---

## waitForSelector()

Waits for an element matching a CSS or XPath selector to appear in the DOM and be visible.

```java
import com.microsoft.playwright.ElementHandle;

// Wait for product cards to appear (returns the first matching ElementHandle)
ElementHandle firstCard = page.waitForSelector(".product-card");

// Wait for specific state
page.waitForSelector(".product-card", new Page.WaitForSelectorOptions()
    .setState(WaitForSelectorState.VISIBLE));   // visible (default)

page.waitForSelector(".spinner-border", new Page.WaitForSelectorOptions()
    .setState(WaitForSelectorState.HIDDEN));    // hidden/gone

page.waitForSelector(".product-card", new Page.WaitForSelectorOptions()
    .setState(WaitForSelectorState.ATTACHED));  // in DOM but may not be visible

page.waitForSelector(".product-card", new Page.WaitForSelectorOptions()
    .setState(WaitForSelectorState.DETACHED));  // removed from DOM

// With timeout
page.waitForSelector(".product-card",
    new Page.WaitForSelectorOptions()
        .setState(WaitForSelectorState.VISIBLE)
        .setTimeout(20_000)
);
```

> **Prefer Locators:** For most use cases, use `assertThat(locator).isVisible()` or just call an action — both auto-wait. `waitForSelector()` is useful when you need the element handle directly.

---

## waitForLoadState()

Waits for the page to reach a specific load state.

```java
import com.microsoft.playwright.options.LoadState;

// Wait for the "load" event (default behavior of page.navigate())
page.waitForLoadState();                            // defaults to LOAD
page.waitForLoadState(LoadState.LOAD);              // all resources loaded

// Wait for DOM to be parsed (faster than LOAD)
page.waitForLoadState(LoadState.DOMCONTENTLOADED);

// Wait for no network requests for 500ms (good for SPA apps)
page.waitForLoadState(LoadState.NETWORKIDLE);

// Usage pattern — after triggering an action that navigates
page.locator("a[href='#!/products']").click();
page.waitForLoadState(LoadState.NETWORKIDLE);
System.out.println("Products page fully loaded");

// After page refresh
page.reload();
page.waitForLoadState(LoadState.LOAD);
```

**Which LoadState for ShopEasy?**

| Scenario | Recommended LoadState |
|---|---|
| Initial page navigate | `LOAD` (default) |
| After AngularJS renders products | `NETWORKIDLE` (waits for API calls to complete) |
| Quick state check | `DOMCONTENTLOADED` |

---

## waitForResponse()

Waits for a specific HTTP network response. Useful to ensure the API call completed before asserting on UI.

```java
import com.microsoft.playwright.Response;

// Wait for the products API response
Response productsResponse = page.waitForResponse(
    response -> response.url().contains("/api/products") && response.status() == 200,
    () -> {
        // Action that triggers the network request
        page.navigate("http://localhost:4200/#!/products");
    }
);
System.out.println("Products API responded with status: " + productsResponse.status());

// Wait for login API response
Response loginResponse = page.waitForResponse(
    response -> response.url().contains("/api/users/login"),
    () -> {
        page.locator("button[type='submit']").click();
    }
);
System.out.println("Login response body: " + loginResponse.text());

// Simple URL match
Response response = page.waitForResponse("**/api/products", () -> {
    navigateTo("products");
});
System.out.println("API status: " + response.status());
```

---

## waitForRequest()

Waits for a specific HTTP request to be sent.

```java
import com.microsoft.playwright.Request;

// Wait for the add-to-cart request to be sent
Request addToCartRequest = page.waitForRequest(
    request -> request.url().contains("/api/cart") && request.method().equals("POST"),
    () -> {
        page.locator(".product-card").first().locator("button.btn-primary").click();
    }
);
System.out.println("Cart request URL: " + addToCartRequest.url());
System.out.println("Cart request body: " + addToCartRequest.postData());
```

---

## waitForFunction()

Waits until a JavaScript expression in the browser evaluates to a truthy value.

```java
// Wait until AngularJS finishes rendering (checks $http pending requests)
page.waitForFunction(
    "window.angular && angular.element(document.body).injector().get('$http').pendingRequests.length === 0"
);

// Wait until a specific element has a particular text
page.waitForFunction(
    "document.querySelector('.product-count') && document.querySelector('.product-count').textContent.includes('10')"
);

// Wait until localStorage has the auth token (after login)
page.waitForFunction(
    "localStorage.getItem('token') !== null"
);
```

---

## page.waitForTimeout() — Use Sparingly

`waitForTimeout()` pauses execution for a fixed duration. This is the Playwright equivalent of `Thread.sleep()`.

```java
// Fixed delay — pauses for 2 seconds
page.waitForTimeout(2000);
```

**Why to avoid it:**

```
User Action → API Call → DOM Update
    ↑
    You don't know how long this takes.

waitForTimeout(2000) — wastes 2 seconds even if DOM updated in 100ms
waitForTimeout(2000) — FAILS if DOM takes 2001ms to update

Better approach:
assertThat(page.locator(".product-card").first()).isVisible();
// This waits EXACTLY as long as needed (up to 30s)
```

**The only acceptable uses:**

```java
// 1. Debugging — slow down to watch what's happening
page.waitForTimeout(1000);  // Do not commit this to version control

// 2. Testing time-sensitive UI (e.g., auto-dismiss alert after 3 seconds)
page.locator(".product-card").first().locator("button").click();
page.waitForTimeout(3500);   // Wait for the 3-second auto-dismiss timer
assertThat(page.locator(".alert-success")).isHidden();
```

---

## Network Idle — Waiting for AngularJS

ShopEasy's AngularJS app makes multiple `$http` requests when loading a page. The `NETWORKIDLE` load state is ideal for ensuring all API calls complete before asserting.

```java
// Navigate and wait for ALL API calls to finish
page.navigate("http://localhost:4200/#!/products",
    new Page.NavigateOptions()
        .setWaitUntil(WaitUntilState.NETWORKIDLE)
);
// At this point, AngularJS has received all responses and updated the DOM
assertThat(page.locator(".product-card").first()).isVisible();

// After triggering an action that causes API calls
page.locator("select[ng-model='selectedCategory']").selectOption("Electronics");
page.waitForLoadState(LoadState.NETWORKIDLE);   // Wait for filter API call
assertThat(page.locator(".product-card").first()).isVisible();
```

---

## Auto-Wait for Animations

Playwright waits for CSS animations to complete before interacting. This handles Bootstrap modal animations automatically:

```java
// Open admin product modal — Bootstrap animates it (fade in ~300ms)
page.locator("button[data-bs-target='#productModal']").click();

// Playwright auto-waits for the animation to finish
// No need for Thread.sleep(300) like in Selenium!
page.getByPlaceholder("Product name").fill("New Product");
```

---

## Complete Example: SPA Navigation with Waits

```java
package com.shopeasy.playwright.tests;

import com.shopeasy.playwright.base.BaseTest;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.LoadState;
import org.testng.Assert;
import org.testng.annotations.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class WaitDemoTest extends BaseTest {

    @Test
    public void loginAndVerifyDashboard() {
        navigateTo("login");

        // Fill form — auto-wait for inputs to be ready
        page.getByPlaceholder("Enter your username").fill("admin");
        page.getByPlaceholder("Enter your password").fill("admin123");
        page.locator("button[type='submit']").click();

        // Wait for SPA hash route to change
        page.waitForURL("**/#!/admin/dashboard");

        // Wait for network idle (dashboard loads stats via API)
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Now assert dashboard is fully loaded
        assertThat(page.locator(".card.shadow-sm").first()).isVisible();
        System.out.println("Dashboard loaded successfully");
    }

    @Test
    public void waitForProductsApiResponse() {
        // Use waitForResponse to intercept and verify the API call
        Response response = page.waitForResponse(
            resp -> resp.url().contains("/api/products") && resp.status() == 200,
            () -> navigateTo("products")
        );

        Assert.assertEquals(response.status(), 200, "Products API should return 200");
        System.out.println("Products API URL: " + response.url());

        // Products should now be visible in the UI
        assertThat(page.locator(".product-card").first()).isVisible();
    }

    @Test
    public void waitForSpinnerToDisappear() {
        navigateTo("products");

        // If spinner appears, wait for it to become hidden
        // (this is idiomatic Playwright — assert hidden auto-waits)
        assertThat(page.locator(".spinner-border")).isHidden();

        // Products should be visible now
        assertThat(page.locator(".product-card").first()).isVisible();
    }

    @Test
    public void verifySpaHashRouting() {
        navigateTo("login");
        assertThat(page).hasURL("**/#!/login");

        // Click register link — AngularJS handles routing in-browser
        page.locator("a[href='#!/register']").click();

        // Wait for hash route change
        assertThat(page).hasURL("**/#!/register");

        // Go back to login
        page.goBack();
        assertThat(page).hasURL("**/#!/login");
    }
}
```

---

## Configuring Global Timeouts

Override default timeouts at different levels:

```java
// ── Per test class (in BaseTest setUp) ────────────────────────────
// Default action timeout (used by all actions in this context)
context.setDefaultTimeout(30_000);            // 30 seconds for all actions
context.setDefaultNavigationTimeout(60_000);  // 60 seconds for navigation

// ── Per action ────────────────────────────────────────────────────
page.locator("button").click(
    new Locator.ClickOptions().setTimeout(5_000)  // only 5 seconds for this click
);

// ── Per assertion ─────────────────────────────────────────────────
assertThat(page.locator(".product-card").first())
    .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(15_000));
```
