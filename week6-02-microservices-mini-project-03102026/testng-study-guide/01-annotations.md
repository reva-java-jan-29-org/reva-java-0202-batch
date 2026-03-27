# 01 — TestNG Annotations

## What are Annotations?

TestNG annotations are special markers (prefixed with `@`) that you place on methods to tell TestNG what role each method plays. TestNG reads these annotations at runtime and calls each method at the right time in the test lifecycle.

**Without annotations, TestNG would not know:**
- Which methods are tests
- Which methods should run before/after a test
- Which methods should run once for the whole suite

---

## Complete List of TestNG Annotations

| Annotation | Runs... | Typical Use |
|---|---|---|
| `@BeforeSuite` | Once before the entire suite starts | Start reporting, read global config |
| `@AfterSuite` | Once after the entire suite finishes | Generate final report, send email |
| `@BeforeTest` | Before each `<test>` tag in testng.xml | Set up test-level shared state |
| `@AfterTest` | After each `<test>` tag in testng.xml | Clean up test-level shared state |
| `@BeforeGroups` | Before the first method of a named group runs | Set up DB for a group |
| `@AfterGroups` | After the last method of a named group finishes | Clean up group-level resources |
| `@BeforeClass` | Once before the first test method in the class | Open browser once for all methods |
| `@AfterClass` | Once after the last test method in the class | Close browser after all methods |
| `@BeforeMethod` | Before every single `@Test` method | Open browser fresh for each test |
| `@AfterMethod` | After every single `@Test` method | Close browser, take screenshot on fail |
| `@Test` | The actual test method | The test scenario being verified |

---

## Execution Order Diagram

```
SUITE START
│
├── @BeforeSuite          (runs once — before everything)
│
├── TEST BLOCK 1  (one <test> tag in testng.xml)
│   ├── @BeforeTest       (runs once per <test> block)
│   │
│   ├── @BeforeGroups     (runs before first test in matching group)
│   │
│   ├── CLASS: LoginTest
│   │   ├── @BeforeClass  (runs once for this class)
│   │   │
│   │   ├── @BeforeMethod (runs before EACH @Test)
│   │   ├── @Test         verifyLoginPageTitle
│   │   ├── @AfterMethod  (runs after EACH @Test)
│   │   │
│   │   ├── @BeforeMethod
│   │   ├── @Test         verifyLoginWithValidCredentials
│   │   ├── @AfterMethod
│   │   │
│   │   └── @AfterClass   (runs once after all @Test in class)
│   │
│   ├── CLASS: ProductTest
│   │   ├── @BeforeClass
│   │   ├── @BeforeMethod
│   │   ├── @Test         verifyProductsPageLoads
│   │   ├── @AfterMethod
│   │   └── @AfterClass
│   │
│   ├── @AfterGroups      (runs after last test in matching group)
│   │
│   └── @AfterTest        (runs once after all classes in <test> block)
│
└── @AfterSuite           (runs once — after everything)
```

> **Key rule:** `@BeforeMethod` / `@AfterMethod` run for EVERY single `@Test` method. If a class has 5 `@Test` methods, `@BeforeMethod` runs 5 times.

---

## `@Test` — The Core Annotation

Every method marked `@Test` is a test case that TestNG discovers and runs.

```java
package com.shopeasy.tests;

import com.shopeasy.tests.base.BaseTest;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {

    @Test
    public void verifyLoginPageTitle() {
        navigateTo("login");
        String title = driver.getTitle();
        Assert.assertEquals(title, "ShopEasy - E-Commerce");
    }

    @Test
    public void verifyUsernameFieldIsVisible() {
        navigateTo("login");
        boolean visible = driver.findElement(
            By.cssSelector("input[placeholder='Enter your username']")
        ).isDisplayed();
        Assert.assertTrue(visible, "Username field should be visible");
    }
}
```

---

## `@Test` Attributes

`@Test` accepts many optional attributes that control how the test runs:

### `enabled`

Temporarily disable a test without deleting it. Disabled tests are shown as "skipped" in reports.

```java
// This test will NOT run — shown as SKIPPED in the report
@Test(enabled = false, description = "Payment gateway is down in dev")
public void verifyCheckoutWithCard() {
    navigateTo("cart");
    // ... test logic
}

// This test WILL run (default is true)
@Test(enabled = true)
public void verifyLoginPageLoads() {
    navigateTo("login");
    Assert.assertEquals(driver.getTitle(), "ShopEasy - E-Commerce");
}
```

---

### `description`

A human-readable description shown in test reports. Always add descriptions — they make reports self-documenting.

```java
@Test(description = "Verify that the ShopEasy login page loads and the title is correct")
public void verifyLoginPageTitle() {
    navigateTo("login");
    Assert.assertEquals(driver.getTitle(), "ShopEasy - E-Commerce");
}

@Test(description = "Verify that the products page shows at least one product card")
public void verifyProductsPageHasProducts() {
    navigateTo("products");
    int productCount = driver.findElements(
        By.cssSelector(".card")
    ).size();
    Assert.assertTrue(productCount > 0, "Expected at least one product card");
}
```

---

### `timeOut`

Maximum milliseconds a test can take before it is marked as FAILED. Useful for catching hung tests.

```java
// Fail the test if it takes more than 5 seconds
@Test(timeOut = 5000)
public void verifyLoginPageLoadsQuickly() {
    navigateTo("login");
    // If the page takes > 5 seconds, TestNG throws TimeoutException
    Assert.assertTrue(
        driver.findElement(By.cssSelector("button[type='submit']")).isDisplayed()
    );
}

// Allow more time for a page with many products
@Test(timeOut = 15000, description = "Products page must load within 15 seconds")
public void verifyProductsPageLoadTime() {
    navigateTo("products");
    Assert.assertTrue(
        driver.findElements(By.cssSelector(".card")).size() > 0
    );
}
```

> **Note:** `timeOut` is in milliseconds. `timeOut = 5000` = 5 seconds.

---

### `invocationCount`

Run the same test method multiple times. Useful for testing reliability (flakiness detection).

```java
// Run this test 3 times to verify it passes consistently
@Test(invocationCount = 3, description = "Login should succeed reliably on every run")
public void verifyLoginStability() {
    navigateTo("login");
    driver.findElement(
        By.cssSelector("input[placeholder='Enter your username']")
    ).sendKeys("testuser");
    driver.findElement(
        By.cssSelector("input[type='password']")
    ).sendKeys("password123");
    driver.findElement(
        By.cssSelector("button[type='submit']")
    ).click();
    // Each of the 3 runs must pass
    Assert.assertEquals(driver.getCurrentUrl(),
        "http://localhost:4200/#!/products");
}
```

---

### `threadPoolSize`

Use with `invocationCount` to run multiple invocations in parallel across threads.

```java
// Run 5 times using 3 parallel threads
@Test(invocationCount = 5, threadPoolSize = 3,
      description = "Load test: 5 simultaneous login attempts with 3 threads")
public void verifyLoginUnderLoad() {
    navigateTo("login");
    driver.findElement(
        By.cssSelector("input[placeholder='Enter your username']")
    ).sendKeys("testuser");
    driver.findElement(
        By.cssSelector("input[type='password']")
    ).sendKeys("password123");
    driver.findElement(
        By.cssSelector("button[type='submit']")
    ).click();
    Assert.assertTrue(driver.getCurrentUrl().contains("products"));
}
```

> **Warning:** When using `threadPoolSize`, your `WebDriver` instance must be thread-safe. See `07-parallel-execution.md` for the `ThreadLocal<WebDriver>` pattern.

---

### `expectedExceptions`

Declare that the test expects a specific exception to be thrown. The test PASSES only if that exception is thrown.

```java
@Test(expectedExceptions = org.openqa.selenium.NoSuchElementException.class,
      description = "Verify that a non-existent element throws NoSuchElementException")
public void verifyNonExistentElementThrows() {
    navigateTo("login");
    // This should throw NoSuchElementException — which is what we expect
    driver.findElement(By.id("this-id-does-not-exist"));
}
```

---

## `@BeforeMethod` and `@AfterMethod`

These run before and after **every** `@Test` method in the class. In Selenium, this is the most common pattern for opening and closing the browser.

```java
package com.shopeasy.tests;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;

public class LoginPageTest {

    private WebDriver driver;
    private static final String BASE_URL = "http://localhost:4200";

    @BeforeMethod
    public void openBrowser() {
        System.out.println(">>> Opening browser");
        driver = new ChromeDriver(new ChromeOptions());
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        // Navigate to login page before each test
        driver.get(BASE_URL + "/#!/login");
    }

    @AfterMethod
    public void closeBrowser() {
        System.out.println(">>> Closing browser");
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void verifyLoginPageTitle() {
        // browser is already open and on the login page
        Assert.assertEquals(driver.getTitle(), "ShopEasy - E-Commerce");
    }

    @Test
    public void verifyLoginButtonText() {
        String buttonText = driver.findElement(
            By.cssSelector("button[type='submit']")
        ).getText();
        Assert.assertEquals(buttonText, "Login");
    }
}
```

**Output (2 tests):**
```
>>> Opening browser
>>> Closing browser
>>> Opening browser
>>> Closing browser
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
```

---

## `@BeforeClass` and `@AfterClass`

These run **once** per class — before the first test method and after the last test method. Use this when browser setup is expensive and you want to share one browser instance across all tests in the class.

```java
package com.shopeasy.tests;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.Duration;

public class AdminDashboardTest {

    // Shared driver for all tests in this class
    private WebDriver driver;
    private static final String BASE_URL = "http://localhost:4200";

    @BeforeClass
    public void setUpOnce() {
        System.out.println(">>> Browser opened ONCE for the class");
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        // Log in as admin once — all tests in this class use the session
        driver.get(BASE_URL + "/#!/login");
        driver.findElement(
            By.cssSelector("input[placeholder='Enter your username']")
        ).sendKeys("admin");
        driver.findElement(
            By.cssSelector("input[type='password']")
        ).sendKeys("admin123");
        driver.findElement(
            By.cssSelector("button[type='submit']")
        ).click();
    }

    @AfterClass
    public void tearDownOnce() {
        System.out.println(">>> Browser closed ONCE after all class tests");
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void verifyDashboardStatsVisible() {
        driver.get(BASE_URL + "/#!/admin/dashboard");
        Assert.assertTrue(
            driver.findElement(By.cssSelector(".card-title")).isDisplayed()
        );
    }

    @Test
    public void verifyAdminProductsPageLoads() {
        driver.get(BASE_URL + "/#!/admin/products");
        Assert.assertTrue(
            driver.findElement(By.cssSelector("table")).isDisplayed()
        );
    }

    @Test
    public void verifyAdminCustomersPageLoads() {
        driver.get(BASE_URL + "/#!/admin/customers");
        Assert.assertTrue(
            driver.findElement(By.cssSelector("table")).isDisplayed()
        );
    }
}
```

> **Important trade-off:** `@BeforeClass` is faster (one browser open/close) but tests are NOT independent — if one test fails it can affect the next. `@BeforeMethod` is safer because each test gets a clean browser state.

---

## `@BeforeSuite` and `@AfterSuite`

These run **once** for the entire suite (all test classes combined). Use for global setup like starting a test server, initializing a report framework, or reading configuration files.

```java
package com.shopeasy.tests.base;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

public class SuiteSetup {

    @BeforeSuite
    public void globalSetUp() {
        System.out.println("========================================");
        System.out.println("SUITE STARTED: ShopEasy Regression Suite");
        System.out.println("Base URL: http://localhost:4200");
        System.out.println("========================================");

        // Example: verify the app is reachable before running tests
        // Example: read environment variables or config files
        // Example: initialize Extent Reports
    }

    @AfterSuite
    public void globalTearDown() {
        System.out.println("========================================");
        System.out.println("SUITE FINISHED");
        System.out.println("Check reports at: target/surefire-reports/");
        System.out.println("========================================");

        // Example: send test results email
        // Example: flush Extent Reports
        // Example: clean up test database
    }
}
```

In `testng.xml`, include `SuiteSetup` as a class or it can be defined in `BaseTest`.

---

## `@BeforeTest` and `@AfterTest`

These run once per `<test>` block in `testng.xml`. A `<test>` block is a logical grouping of test classes. `@BeforeTest` and `@AfterTest` are used less frequently but are useful when different test groups need different setup.

```java
package com.shopeasy.tests.base;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;

public class BaseTest {

    @BeforeTest
    public void beforeTestBlock() {
        System.out.println("--- Starting test block ---");
        // Runs once before all classes in one <test> tag
    }

    @AfterTest
    public void afterTestBlock() {
        System.out.println("--- Finished test block ---");
        // Runs once after all classes in one <test> tag
    }
}
```

**When do you use `@BeforeTest` vs `@BeforeClass`?**

| Scope | Annotation | Example Use Case |
|---|---|---|
| Before every method | `@BeforeMethod` | Open a fresh browser per test |
| Before a class's tests | `@BeforeClass` | Log in once for all tests in a class |
| Before a `<test>` XML block | `@BeforeTest` | Set environment variable for a group of classes |
| Before the whole suite | `@BeforeSuite` | Start the app server, init global config |

---

## `@BeforeGroups` and `@AfterGroups`

These run before the first and after the last test method belonging to a named group.

```java
package com.shopeasy.tests;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

import java.time.Duration;

public class AdminTest {

    private WebDriver driver;

    @BeforeGroups(groups = "admin")
    public void adminSetUp() {
        System.out.println("Setting up admin session...");
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        // Log in as admin
        driver.get("http://localhost:4200/#!/login");
        driver.findElement(
            By.cssSelector("input[placeholder='Enter your username']")
        ).sendKeys("admin");
        driver.findElement(
            By.cssSelector("input[type='password']")
        ).sendKeys("admin123");
        driver.findElement(
            By.cssSelector("button[type='submit']")
        ).click();
    }

    @AfterGroups(groups = "admin")
    public void adminTearDown() {
        System.out.println("Tearing down admin session...");
        if (driver != null) {
            driver.quit();
        }
    }

    @Test(groups = "admin",
          description = "Admin dashboard should display total orders count")
    public void verifyDashboardOrderCount() {
        driver.get("http://localhost:4200/#!/admin/dashboard");
        Assert.assertTrue(
            driver.findElements(By.cssSelector(".card")).size() > 0
        );
    }

    @Test(groups = "admin",
          description = "Admin products page should show product table")
    public void verifyProductsTable() {
        driver.get("http://localhost:4200/#!/admin/products");
        Assert.assertTrue(
            driver.findElement(By.cssSelector("table tbody")).isDisplayed()
        );
    }
}
```

---

## Annotation Inheritance

When a test class extends `BaseTest`, it inherits the `@BeforeMethod` and `@AfterMethod` from the parent. Both the parent's and child's lifecycle methods run.

```java
// Parent class
public class BaseTest {
    @BeforeMethod
    public void setUp() {
        System.out.println("BaseTest: opening browser");
        // ... create driver
    }

    @AfterMethod
    public void tearDown() {
        System.out.println("BaseTest: closing browser");
        // ... driver.quit()
    }
}

// Child class
public class LoginTest extends BaseTest {

    @BeforeMethod
    public void navigateToLogin() {
        System.out.println("LoginTest: navigating to login page");
        driver.get("http://localhost:4200/#!/login");
    }

    @Test
    public void verifyLoginPageTitle() {
        Assert.assertEquals(driver.getTitle(), "ShopEasy - E-Commerce");
    }
}
```

**Execution order for one test method:**
```
1. BaseTest.setUp()           ← parent @BeforeMethod
2. LoginTest.navigateToLogin() ← child @BeforeMethod
3. LoginTest.verifyLoginPageTitle() ← @Test
4. BaseTest.tearDown()        ← parent @AfterMethod
```

> TestNG calls parent `@BeforeMethod` before child `@BeforeMethod`, and child `@AfterMethod` before parent `@AfterMethod` (stack-like cleanup).

---

## Complete Execution Order Reference

```
@BeforeSuite
  @BeforeTest
    @BeforeGroups
      @BeforeClass
        @BeforeMethod  ←────┐
        @Test               │  repeats for each @Test
        @AfterMethod   ←────┘
      @AfterClass
    @AfterGroups
  @AfterTest
@AfterSuite
```

---

## Common Mistakes

| Mistake | Problem | Fix |
|---|---|---|
| Using `@BeforeClass` to create WebDriver | Tests not isolated — one test failure corrupts others | Use `@BeforeMethod` unless you have a reason |
| Not calling `driver.quit()` in `@AfterMethod` | Browser processes pile up, memory leak | Always quit in `@AfterMethod` with null check |
| Having logic in `@AfterSuite` that can throw | Suite cleanup silently fails | Wrap in try-catch |
| Using `static WebDriver` with `@BeforeMethod` | Thread safety issues in parallel runs | Use `ThreadLocal<WebDriver>` — see 07-parallel-execution.md |
| Forgetting `@Test` on a method | TestNG silently ignores it | Always annotate test methods |
