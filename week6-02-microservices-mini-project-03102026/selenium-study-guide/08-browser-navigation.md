# 08 — WebDriver Methods: Browser Methods & Navigation

## Overview

This module covers how to control the browser itself — navigating between URLs, going back/forward in history, refreshing pages, and managing the browser window.

---

## 1. Navigation Methods

### `driver.get(url)` vs `driver.navigate().to(url)`

Both load a URL, but there is a subtle difference:

| Method | Behavior |
|---|---|
| `driver.get(url)` | Loads the URL and **waits for page load** to complete |
| `driver.navigate().to(url)` | Same as `get()` but returns a `Navigation` object, enabling chaining |

```java
// Using get()
driver.get("http://localhost:4200/#!/login");

// Using navigate().to()
driver.navigate().to("http://localhost:4200/#!/login");
driver.navigate().to(new java.net.URL("http://localhost:4200"));
```

**For ShopEasy (SPA with hash routing):**
Both work the same way. Use your `navigateTo()` helper method:

```java
navigateTo("login");      // → http://localhost:4200/#!/login
navigateTo("products");   // → http://localhost:4200/#!/products
navigateTo("admin/dashboard"); // → http://localhost:4200/#!/admin/dashboard
```

---

### `driver.navigate().back()`

Simulates clicking the browser's Back button — goes to the previous URL in the browser history.

```java
// Start at products page
navigateTo("products");
Assert.assertTrue(driver.getCurrentUrl().contains("products"));

// Navigate to login page
navigateTo("login");
Assert.assertTrue(driver.getCurrentUrl().contains("login"));

// Go back — returns to products page
driver.navigate().back();
Assert.assertTrue(driver.getCurrentUrl().contains("products"));
```

**SPA Caveat:** In AngularJS SPAs using hash routing (`#!/route`), each route change creates a browser history entry. So `back()` works as expected between AngularJS routes.

---

### `driver.navigate().forward()`

Simulates the browser's Forward button — goes to the next URL in history (only works after going back).

```java
navigateTo("login");
navigateTo("register");
driver.navigate().back();     // Back to login
driver.navigate().forward();  // Forward to register
Assert.assertTrue(driver.getCurrentUrl().contains("register"));
```

---

### `driver.navigate().refresh()`

Reloads the current page — same as pressing F5.

```java
navigateTo("products");

// Refresh the page (AngularJS will re-fetch products from API)
driver.navigate().refresh();

// Wait for products to reload after refresh
WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
    By.cssSelector(".product-card"), 0
));
System.out.println("Products reloaded after refresh");
```

**When refresh is useful in tests:**
- Testing that the page state is consistent after reload
- Verifying localStorage-based auth (ShopEasy stores JWT in localStorage — after refresh, auth should persist)
- Clearing partial form states

---

## 2. Window Management

### Maximize, Minimize, Fullscreen

```java
driver.manage().window().maximize();    // Maximize to screen size
driver.manage().window().minimize();    // Minimize to taskbar
driver.manage().window().fullscreen();  // True fullscreen (F11)
```

### Set Custom Window Size

```java
// Set to specific dimensions — useful for responsive testing
driver.manage().window().setSize(new Dimension(1366, 768));  // HD laptop
driver.manage().window().setSize(new Dimension(1920, 1080)); // Full HD
driver.manage().window().setSize(new Dimension(375, 812));   // iPhone X portrait
```

### Get Current Window Size and Position

```java
Dimension size = driver.manage().window().getSize();
System.out.println("Width: " + size.getWidth() + ", Height: " + size.getHeight());

Point position = driver.manage().window().getPosition();
System.out.println("X: " + position.getX() + ", Y: " + position.getY());
```

### Set Window Position

```java
// Move window to top-left corner
driver.manage().window().setPosition(new Point(0, 0));
```

---

## 3. Responsive Testing — ShopEasy's Bootstrap Grid

ShopEasy uses Bootstrap 5's responsive grid:
- `col-lg-4` — 4 product columns on large screens (≥992px)
- `col-md-3` — 3 columns on medium screens (≥768px)
- `col-sm-2` — 2 columns on small screens (≥576px)
- `col-1` — 1 column on extra small screens

```java
@Test
public void testProductGridResponsive() {
    navigateTo("products");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

    // Desktop view — 4 columns
    driver.manage().window().setSize(new Dimension(1200, 800));
    wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
        By.cssSelector(".product-card"), 0
    ));
    System.out.println("Desktop: Products grid loaded");

    // Tablet view — 3 columns
    driver.manage().window().setSize(new Dimension(800, 600));
    System.out.println("Tablet: Products grid adapted");

    // Mobile view — 1 column
    driver.manage().window().setSize(new Dimension(375, 812));
    // Verify navbar toggler is visible (hamburger menu)
    WebElement navbarToggler = driver.findElement(By.cssSelector(".navbar-toggler"));
    Assert.assertTrue(navbarToggler.isDisplayed(),
        "Hamburger menu should appear on mobile");

    // Restore
    driver.manage().window().maximize();
}
```

---

## 4. Page Load Strategy

Controls how long WebDriver waits after calling `get()` or `navigate().to()`:

```java
ChromeOptions options = new ChromeOptions();
options.setPageLoadStrategy(PageLoadStrategy.NORMAL);   // Default: wait for full page load
options.setPageLoadStrategy(PageLoadStrategy.EAGER);    // Wait for DOM ready
options.setPageLoadStrategy(PageLoadStrategy.NONE);     // Don't wait — manual wait required

driver = new ChromeDriver(options);
```

| Strategy | Waits for | Use Case |
|---|---|---|
| `NORMAL` | `window.onload` event | Standard web pages |
| `EAGER` | DOM interactive | SPAs (AngularJS handles content loading) |
| `NONE` | Nothing | Manual waits for each element |

**For ShopEasy:** `NORMAL` is fine. Since AngularJS loads content asynchronously after the DOM, you need explicit waits anyway.

---

## 5. Navigation History Example — ShopEasy User Flow

```java
@Test
public void testBrowserNavigationInShopEasy() {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

    // Step 1: Start at products page
    navigateTo("products");
    wait.until(ExpectedConditions.urlContains("products"));
    System.out.println("1. " + driver.getCurrentUrl());

    // Step 2: Navigate to login
    navigateTo("login");
    wait.until(ExpectedConditions.urlContains("login"));
    System.out.println("2. " + driver.getCurrentUrl());

    // Step 3: Navigate to register
    navigateTo("register");
    wait.until(ExpectedConditions.urlContains("register"));
    System.out.println("3. " + driver.getCurrentUrl());

    // Step 4: Go back to login
    driver.navigate().back();
    wait.until(ExpectedConditions.urlContains("login"));
    System.out.println("4. Back to: " + driver.getCurrentUrl());

    // Step 5: Go back to products
    driver.navigate().back();
    wait.until(ExpectedConditions.urlContains("products"));
    System.out.println("5. Back to: " + driver.getCurrentUrl());

    // Step 6: Go forward to login
    driver.navigate().forward();
    wait.until(ExpectedConditions.urlContains("login"));
    System.out.println("6. Forward to: " + driver.getCurrentUrl());

    // Step 7: Refresh the page
    driver.navigate().refresh();
    wait.until(ExpectedConditions.titleIs("ShopEasy - E-Commerce"));
    System.out.println("7. After refresh: " + driver.getCurrentUrl());
}
```

---

## 6. `driver.close()` vs `driver.quit()`

| Method | What It Does |
|---|---|
| `driver.close()` | Closes the **current** browser window only. If multiple windows are open, others stay. Does NOT kill ChromeDriver process. |
| `driver.quit()` | Closes **ALL** browser windows and kills the ChromeDriver process. |

```java
// Always use quit() in @AfterMethod to fully clean up
@AfterMethod
public void tearDown() {
    if (driver != null) {
        driver.quit(); // closes all windows + kills driver process
    }
}

// close() usage — when you open a new window/tab and want to close just that one
// (see 16-keyboard-tabs-windows.md for multi-window handling)
```

---

## Common Navigation Errors and Fixes

| Error | Cause | Fix |
|---|---|---|
| `TimeoutException` on `get()` | Page load takes too long | Increase `pageLoadTimeout` or use `EAGER` strategy |
| URL doesn't change after `navigate().back()` | No history available | Check that you navigated forward first |
| `back()` goes to previous page, not previous SPA route | Hash routing history entry missing | ShopEasy's hash routing should be fine; check AngularJS `$location` config |
| Refresh causes logout | App doesn't persist auth | ShopEasy uses `localStorage` — refresh should maintain login |
