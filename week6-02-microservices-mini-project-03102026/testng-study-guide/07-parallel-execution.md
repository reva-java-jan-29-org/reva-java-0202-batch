# 07 — Parallel Execution

## Why Run Tests in Parallel?

Sequential test execution is slow. If each Selenium test takes 10 seconds and you have 100 tests, that is 1000 seconds (~17 minutes). With 4 parallel threads, you can finish in ~4 minutes.

**Parallel execution in TestNG:**
- Multiple tests run simultaneously in separate threads
- Each thread needs its own independent `WebDriver` instance
- The critical pattern: `ThreadLocal<WebDriver>` — ensures each thread has its own driver

---

## Parallel Modes in TestNG

TestNG supports four parallel modes, set in `testng.xml`:

| Mode | What Runs in Parallel |
|---|---|
| `parallel="methods"` | Each `@Test` method runs in its own thread |
| `parallel="tests"` | Each `<test>` block in testng.xml runs in its own thread |
| `parallel="classes"` | Each test class runs in its own thread |
| `parallel="instances"` | Each instance of a test class runs in its own thread |

---

## `parallel="methods"` — Method-Level Parallelism

The most granular mode. Every `@Test` method runs concurrently:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">

<suite name="ShopEasy Parallel Suite"
       parallel="methods"
       thread-count="4">

    <test name="Parallel Method Tests">
        <classes>
            <class name="com.shopeasy.tests.LoginTest"/>
            <class name="com.shopeasy.tests.ProductTest"/>
            <class name="com.shopeasy.tests.CartTest"/>
        </classes>
    </test>

</suite>
```

With `thread-count="4"`, up to 4 test methods run simultaneously. If LoginTest has 3 methods and ProductTest has 3 methods, all 6 can start nearly at the same time (4 at once, then the remaining 2).

---

## `parallel="tests"` — Test Block Parallelism

Each `<test>` block runs in parallel — all classes within one block still run sequentially:

```xml
<suite name="ShopEasy Suite"
       parallel="tests"
       thread-count="3">

    <!-- These three <test> blocks run simultaneously -->

    <test name="Login Tests">
        <classes>
            <class name="com.shopeasy.tests.LoginTest"/>
        </classes>
    </test>

    <test name="Product Tests">
        <classes>
            <class name="com.shopeasy.tests.ProductTest"/>
        </classes>
    </test>

    <test name="Cart Tests">
        <classes>
            <class name="com.shopeasy.tests.CartTest"/>
        </classes>
    </test>

</suite>
```

---

## `parallel="classes"` — Class-Level Parallelism

Each class runs in its own thread, but methods within a class run sequentially:

```xml
<suite name="ShopEasy Suite"
       parallel="classes"
       thread-count="3">

    <test name="All Tests">
        <classes>
            <!-- Each class runs in its own thread -->
            <class name="com.shopeasy.tests.LoginTest"/>
            <class name="com.shopeasy.tests.ProductTest"/>
            <class name="com.shopeasy.tests.CartTest"/>
        </classes>
    </test>

</suite>
```

---

## The Problem: Shared `WebDriver` is Not Thread-Safe

```java
// BROKEN — NOT thread-safe
public class BaseTest {
    protected WebDriver driver;  // shared across threads → race condition!

    @BeforeMethod
    public void setUp() {
        driver = new ChromeDriver();  // Thread 1 creates driver
        // Thread 2 immediately overwrites it with a new driver
        // Thread 1's driver is now gone → NullPointerException
    }
}
```

When two threads both call `@BeforeMethod` at the same time, they both try to assign to the same `driver` field. Thread 2 overwrites Thread 1's driver. Thread 1 now has a null or wrong driver.

---

## The Solution: `ThreadLocal<WebDriver>`

`ThreadLocal` stores a separate value per thread. Thread 1 gets its own `WebDriver`. Thread 2 gets its own separate `WebDriver`. They never interfere.

```
Thread 1 → ThreadLocal.get() → ChromeDriver instance A
Thread 2 → ThreadLocal.get() → ChromeDriver instance B
Thread 3 → ThreadLocal.get() → ChromeDriver instance C
```

---

## Thread-Safe `BaseTest` Implementation

```java
package com.shopeasy.tests.base;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.time.Duration;

/**
 * Thread-safe BaseTest using ThreadLocal<WebDriver>.
 *
 * Each thread (parallel test) gets its own isolated WebDriver instance.
 * No shared state — no race conditions.
 */
public class BaseTest {

    // ThreadLocal: each thread has its own WebDriver
    private static final ThreadLocal<WebDriver> driverThreadLocal =
        new ThreadLocal<>();

    protected static final String BASE_URL = "http://localhost:4200";

    /**
     * Get the WebDriver for the current thread.
     * Use this instead of a direct 'driver' field.
     */
    protected WebDriver getDriver() {
        return driverThreadLocal.get();
    }

    @BeforeMethod
    public void setUp() {
        ChromeOptions options = new ChromeOptions();

        // Each thread creates its own ChromeDriver instance
        WebDriver driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));

        // Store this driver for the current thread
        driverThreadLocal.set(driver);
    }

    @AfterMethod
    public void tearDown() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            driver.quit();
        }
        // CRITICAL: remove the ThreadLocal value to prevent memory leaks
        driverThreadLocal.remove();
    }

    protected void navigateTo(String route) {
        getDriver().get(BASE_URL + "/#!/" + route);
    }
}
```

---

## Test Classes Using Thread-Safe BaseTest

```java
package com.shopeasy.tests;

import com.shopeasy.tests.base.BaseTest;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {

    @Test(description = "Verify login page title")
    public void verifyLoginPageTitle() {
        // getDriver() returns THIS thread's WebDriver — not another thread's
        navigateTo("login");
        Assert.assertEquals(getDriver().getTitle(), "ShopEasy - E-Commerce");
    }

    @Test(description = "Verify login page has form elements")
    public void verifyLoginFormElements() {
        navigateTo("login");
        Assert.assertTrue(
            getDriver().findElement(
                By.cssSelector("input[placeholder='Enter your username']")
            ).isDisplayed()
        );
    }

    @Test(description = "Verify successful login")
    public void verifySuccessfulLogin() {
        navigateTo("login");
        getDriver().findElement(
            By.cssSelector("input[placeholder='Enter your username']")
        ).sendKeys("testuser");
        getDriver().findElement(
            By.cssSelector("input[type='password']")
        ).sendKeys("password123");
        getDriver().findElement(
            By.cssSelector("button[type='submit']")
        ).click();
        Assert.assertTrue(getDriver().getCurrentUrl().contains("products"));
    }
}
```

---

## Parallel with `@DataProvider`

Data providers can also run parallel. Set `parallel = true` on the `@DataProvider`:

```java
package com.shopeasy.tests;

import com.shopeasy.tests.base.BaseTest;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ParallelSearchTest extends BaseTest {

    // parallel=true: each data row runs in its own thread
    @DataProvider(name = "searchTermsParallel", parallel = true)
    public Object[][] searchData() {
        return new Object[][] {
            { "laptop"   },
            { "phone"    },
            { "tablet"   },
            { "shirt"    },
            { "book"     },
        };
    }

    @Test(dataProvider = "searchTermsParallel",
          description = "Parallel product search test")
    public void verifyProductSearch(String keyword) {
        // Each thread has its own driver via ThreadLocal
        navigateTo("products");

        getDriver().findElement(
            By.cssSelector("input[placeholder]")
        ).sendKeys(keyword);

        try { Thread.sleep(500); } catch (InterruptedException e) { }

        int count = getDriver().findElements(By.cssSelector(".card")).size();
        // Just verify page didn't crash
        Assert.assertTrue(count >= 0,
            "Page should load successfully for keyword: " + keyword);
    }
}
```

In `testng.xml`, set the thread count to control parallelism:

```xml
<suite name="Parallel Data Suite"
       parallel="methods"
       thread-count="5"
       data-provider-thread-count="5">

    <test name="Parallel Search Tests">
        <classes>
            <class name="com.shopeasy.tests.ParallelSearchTest"/>
        </classes>
    </test>

</suite>
```

> `data-provider-thread-count` controls how many parallel data-provider threads are used.

---

## Race Conditions and How to Avoid Them

A **race condition** occurs when two threads access and modify shared data simultaneously, causing unpredictable results.

**Common race conditions in Selenium tests:**

| Problem | Cause | Fix |
|---|---|---|
| Shared `WebDriver` | Two threads write to same field | Use `ThreadLocal<WebDriver>` |
| Shared test data files | Two threads read/write same file | Use separate files per thread, or synchronize |
| Shared counters/state | Two threads increment same counter | Use `AtomicInteger`, `synchronized` block |
| Shared screenshots folder | Two threads write file with same name | Include thread ID or timestamp in file name |

**Thread-safe screenshot naming:**

```java
private void captureScreenshot(WebDriver driver, String testName) {
    try {
        java.io.File src = ((TakesScreenshot) driver)
            .getScreenshotAs(OutputType.FILE);

        // Include thread ID to prevent file name collisions
        String threadId  = String.valueOf(Thread.currentThread().getId());
        String timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        );

        Path dest = Paths.get("target/screenshots",
                              testName + "_thread" + threadId + "_" + timestamp + ".png");
        Files.createDirectories(dest.getParent());
        Files.copy(src.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);

    } catch (IOException e) {
        System.out.println("Screenshot error: " + e.getMessage());
    }
}
```

---

## Complete Thread-Safe BaseTest — Full Version

```java
package com.shopeasy.tests.base;

import com.shopeasy.tests.listeners.ScreenshotListenerAdapter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import java.time.Duration;

@Listeners(ScreenshotListenerAdapter.class)
public class BaseTest {

    // ── ThreadLocal WebDriver ─────────────────────────────────────────
    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    protected static final String BASE_URL = "http://localhost:4200";

    // ── Driver accessor for current thread ───────────────────────────
    protected WebDriver getDriver() {
        return DRIVER.get();
    }

    // ── Also expose as public for listeners ──────────────────────────
    public WebDriver driver() {
        return DRIVER.get();
    }

    // ── Setup ─────────────────────────────────────────────────────────
    @BeforeMethod
    @Parameters({"browser"})
    public void setUp(@Optional("chrome") String browser) {
        WebDriver driver;

        switch (browser.toLowerCase()) {
            case "firefox":
                driver = new FirefoxDriver(new FirefoxOptions());
                break;
            case "chrome":
            default:
                driver = new ChromeDriver(new ChromeOptions());
                break;
        }

        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));

        DRIVER.set(driver);
    }

    // ── Teardown ──────────────────────────────────────────────────────
    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            driver.quit();
        }
        DRIVER.remove();  // Prevent thread-local memory leak
    }

    // ── Navigation helper ─────────────────────────────────────────────
    protected void navigateTo(String route) {
        getDriver().get(BASE_URL + "/#!/" + route);
    }
}
```

---

## `parallel="instances"` Mode

Creates multiple instances of the same test class and runs each instance in a separate thread. Most useful combined with `@Factory`:

```xml
<suite name="Multi-Instance Suite"
       parallel="instances"
       thread-count="3">

    <test name="Multi Browser">
        <classes>
            <class name="com.shopeasy.tests.LoginTest"/>
        </classes>
    </test>

</suite>
```

> See `09-factories.md` for `@Factory` + parallel instances for cross-browser testing.

---

## Quick Reference: Parallel Modes

```
parallel="methods"   → Best for: Large test suite, independent tests
                        Risk:    High (most concurrent, needs ThreadLocal)
                        When:    Standard Selenium regression runs

parallel="tests"     → Best for: Different environments/browsers per <test> block
                        Risk:    Medium (classes within block still sequential)
                        When:    Multi-environment testing

parallel="classes"   → Best for: Classes with shared @BeforeClass setup
                        Risk:    Medium
                        When:    Admin suite + Customer suite in parallel

parallel="instances" → Best for: Same test class, different configurations
                        Risk:    High (needs @Factory)
                        When:    Cross-browser testing with factories
```
