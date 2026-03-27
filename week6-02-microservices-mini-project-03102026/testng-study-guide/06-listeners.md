# 06 — TestNG Listeners

## What are Listeners?

TestNG **Listeners** are classes that listen to test events and let you run custom code when those events happen. They follow the Observer/Event pattern.

**Events TestNG fires:**
- A test is about to start
- A test passed
- A test failed
- A test was skipped
- A suite started
- A suite finished

**What you can do in listeners:**
- Take a screenshot when a test fails
- Write test results to a log file or database
- Send Slack / email notifications on failure
- Generate custom HTML reports
- Print pass/fail statistics

---

## TestNG Listener Interfaces

| Interface | Key Methods | Use For |
|---|---|---|
| `ITestListener` | `onTestStart`, `onTestSuccess`, `onTestFailure`, `onTestSkipped`, `onFinish` | React to individual test results |
| `ISuiteListener` | `onStart`, `onFinish` | React to suite start/end |
| `IReporter` | `generateReport` | Generate custom reports after suite completes |
| `IAnnotationTransformer` | `transform` | Modify test annotations at runtime |
| `IMethodInterceptor` | `intercept` | Filter/reorder tests before run |

---

## `ITestListener` — The Most Important Interface

`ITestListener` has these methods:

```
onStart(ITestContext)           — Test block starts (before any @Test in a <test> tag)
onFinish(ITestContext)          — Test block finishes (after all @Test in a <test> tag)
onTestStart(ITestResult)        — Individual @Test method is about to run
onTestSuccess(ITestResult)      — Individual @Test method passed
onTestFailure(ITestResult)      — Individual @Test method failed
onTestSkipped(ITestResult)      — Individual @Test method was skipped
onTestFailedWithTimeout(ITestResult) — @Test failed due to timeout
onTestFailedButWithinSuccessPercentage(ITestResult) — Advanced: partial success
```

---

## Implementing `ITestListener`

```java
package com.shopeasy.tests.listeners;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Custom TestNG listener that logs test events to the console.
 * Register this via @Listeners annotation or testng.xml.
 */
public class TestLogger implements ITestListener {

    @Override
    public void onStart(ITestContext context) {
        System.out.println("=================================================");
        System.out.println("TEST BLOCK STARTED: " + context.getName());
        System.out.println("=================================================");
    }

    @Override
    public void onFinish(ITestContext context) {
        System.out.println("=================================================");
        System.out.println("TEST BLOCK FINISHED: " + context.getName());
        System.out.println("  Passed:  " + context.getPassedTests().size());
        System.out.println("  Failed:  " + context.getFailedTests().size());
        System.out.println("  Skipped: " + context.getSkippedTests().size());
        System.out.println("=================================================");
    }

    @Override
    public void onTestStart(ITestResult result) {
        System.out.println("[START]   " + getTestName(result));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        System.out.println("[PASS]    " + getTestName(result));
    }

    @Override
    public void onTestFailure(ITestResult result) {
        System.out.println("[FAIL]    " + getTestName(result));
        System.out.println("          Reason: " + result.getThrowable().getMessage());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        System.out.println("[SKIPPED] " + getTestName(result));
    }

    // Helper: get readable test name
    private String getTestName(ITestResult result) {
        return result.getTestClass().getName() + "." + result.getName();
    }
}
```

---

## Screenshot on Failure Listener

The most valuable listener in Selenium automation. When a test fails, automatically capture a screenshot and save it with the test name and timestamp.

```java
package com.shopeasy.tests.listeners;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Listener that automatically takes a screenshot when any @Test fails.
 *
 * How it gets the WebDriver:
 *   The test instance is retrieved via result.getInstance().
 *   Cast to BaseTest to access the protected 'driver' field.
 *
 * Where screenshots are saved:
 *   target/screenshots/{ClassName}/{testName}_{timestamp}.png
 */
public class ScreenshotListener implements ITestListener {

    private static final String SCREENSHOT_DIR = "target/screenshots";
    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Override
    public void onTestFailure(ITestResult result) {
        // Get the WebDriver from the test instance
        Object testInstance = result.getInstance();

        // Only proceed if the test class extends BaseTest
        if (testInstance instanceof com.shopeasy.tests.base.BaseTest) {
            WebDriver driver = ((com.shopeasy.tests.base.BaseTest) testInstance).driver;
            takeScreenshot(driver, result);
        }
    }

    private void takeScreenshot(WebDriver driver, ITestResult result) {
        if (driver == null) {
            System.out.println("[ScreenshotListener] Driver is null — cannot take screenshot");
            return;
        }

        try {
            // Cast WebDriver to TakesScreenshot interface
            TakesScreenshot ts = (TakesScreenshot) driver;
            File screenshot = ts.getScreenshotAs(OutputType.FILE);

            // Build the output path: target/screenshots/LoginTest/
            String className  = result.getTestClass().getRealClass().getSimpleName();
            String testName   = result.getName();
            String timestamp  = LocalDateTime.now().format(FORMATTER);
            String fileName   = testName + "_" + timestamp + ".png";

            Path outputDir  = Paths.get(SCREENSHOT_DIR, className);
            Path outputFile = outputDir.resolve(fileName);

            // Create directories if they don't exist
            Files.createDirectories(outputDir);

            // Copy the screenshot file to the output location
            Files.copy(screenshot.toPath(), outputFile, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("[ScreenshotListener] Screenshot saved: " + outputFile);

        } catch (IOException e) {
            System.out.println("[ScreenshotListener] Failed to save screenshot: " + e.getMessage());
        }
    }

    // ── Other lifecycle methods — empty implementations ───────────────

    @Override
    public void onTestStart(ITestResult result) { }

    @Override
    public void onTestSuccess(ITestResult result) { }

    @Override
    public void onTestSkipped(ITestResult result) { }

    @Override
    public void onStart(ITestContext context) { }

    @Override
    public void onFinish(ITestContext context) { }
}
```

> **Important:** For this listener to work, the `driver` field in `BaseTest` must be **`public`** or **`protected`** (not `private`).

```java
// In BaseTest — must be accessible to the listener
public class BaseTest {
    public WebDriver driver;    // public so listener can access it
    // ...
}
```

---

## `TestListenerAdapter` — Extend Instead of Implement

`TestListenerAdapter` is an abstract class that provides empty implementations of all `ITestListener` methods. Extend it instead of implementing `ITestListener` so you only override the methods you care about:

```java
package com.shopeasy.tests.listeners;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Extends TestListenerAdapter — cleaner than implementing ITestListener
 * because we only override the methods we need.
 */
public class ScreenshotListenerAdapter extends TestListenerAdapter {

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // Only override what we need — no empty method clutter
    @Override
    public void onTestFailure(ITestResult result) {
        Object instance = result.getInstance();
        if (instance instanceof com.shopeasy.tests.base.BaseTest) {
            WebDriver driver = ((com.shopeasy.tests.base.BaseTest) instance).driver;
            if (driver != null) {
                captureScreenshot(driver, result.getName());
            }
        }
    }

    private void captureScreenshot(WebDriver driver, String testName) {
        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String timestamp = LocalDateTime.now().format(FORMATTER);
            Path dest = Paths.get("target/screenshots",
                                  testName + "_" + timestamp + ".png");
            Files.createDirectories(dest.getParent());
            Files.copy(src.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[Screenshot] Saved: " + dest);
        } catch (IOException e) {
            System.out.println("[Screenshot] Error: " + e.getMessage());
        }
    }
}
```

---

## `ISuiteListener` — Suite-Level Events

```java
package com.shopeasy.tests.listeners;

import org.testng.ISuite;
import org.testng.ISuiteListener;

/**
 * Listener for suite start and finish events.
 */
public class SuiteLogger implements ISuiteListener {

    @Override
    public void onStart(ISuite suite) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║  SUITE STARTED: " + suite.getName());
        System.out.println("║  XML File:      " + suite.getXmlSuite().getFileName());
        System.out.println("╚══════════════════════════════════════════╝");
    }

    @Override
    public void onFinish(ISuite suite) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║  SUITE FINISHED: " + suite.getName());
        System.out.println("╚══════════════════════════════════════════╝");
    }
}
```

---

## Registering Listeners

There are two ways to register a listener:

### Method 1: `@Listeners` Annotation (Class Level)

Apply directly to a test class. Only affects that class (and its subclasses if applied to `BaseTest`):

```java
package com.shopeasy.tests;

import com.shopeasy.tests.base.BaseTest;
import com.shopeasy.tests.listeners.ScreenshotListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

// Register one or multiple listeners
@Listeners({ ScreenshotListener.class })
public class LoginTest extends BaseTest {

    @Test
    public void verifyLoginPageTitle() {
        navigateTo("login");
        // If this assertion fails → ScreenshotListener.onTestFailure fires → screenshot saved
        Assert.assertEquals(driver.getTitle(), "ShopEasy - E-Commerce");
    }
}
```

Apply to `BaseTest` for global coverage:

```java
package com.shopeasy.tests.base;

import com.shopeasy.tests.listeners.ScreenshotListener;
import com.shopeasy.tests.listeners.TestLogger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

import java.time.Duration;

// All subclasses automatically get these listeners
@Listeners({ ScreenshotListener.class, TestLogger.class })
public class BaseTest {

    public WebDriver driver;  // public for listener access
    protected static final String BASE_URL = "http://localhost:4200";

    @BeforeMethod
    public void setUp() {
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    protected void navigateTo(String route) {
        driver.get(BASE_URL + "/#!/" + route);
    }
}
```

---

### Method 2: `testng.xml` Registration (Global)

Register in `testng.xml` to apply to ALL tests in the suite:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">

<suite name="ShopEasy Suite">

    <!-- Listeners apply to ALL tests in this suite -->
    <listeners>
        <listener class-name="com.shopeasy.tests.listeners.ScreenshotListener"/>
        <listener class-name="com.shopeasy.tests.listeners.TestLogger"/>
        <listener class-name="com.shopeasy.tests.listeners.SuiteLogger"/>
    </listeners>

    <test name="All Tests">
        <packages>
            <package name="com.shopeasy.tests"/>
        </packages>
    </test>

</suite>
```

**`@Listeners` vs `testng.xml`:**

| | `@Listeners` | `testng.xml` |
|---|---|---|
| Scope | Only the annotated class and subclasses | All tests in the suite |
| Flexibility | Applied per class | Configured without code changes |
| CI/CD | Requires code change to add/remove | Edit XML only |
| Recommended | For class-specific listeners | For global listeners like screenshot |

---

## Complete: BaseTest with Screenshot on Failure

```java
package com.shopeasy.tests.base;

import com.shopeasy.tests.listeners.ScreenshotListenerAdapter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

import java.time.Duration;

@Listeners(ScreenshotListenerAdapter.class)
public class BaseTest {

    // public — accessible by ScreenshotListenerAdapter via result.getInstance()
    public WebDriver driver;

    protected static final String BASE_URL = "http://localhost:4200";

    @BeforeMethod
    public void setUp() {
        ChromeOptions options = new ChromeOptions();
        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    protected void navigateTo(String route) {
        driver.get(BASE_URL + "/#!/" + route);
    }
}
```

**When a test fails, you get:**
```
[FAIL]    LoginTest.verifyLoginPageTitle
[Screenshot] Saved: target/screenshots/verifyLoginPageTitle_20260327_143052.png
```

---

## `ITestResult` — The Result Object

`ITestResult` is passed to listener methods and contains rich information about the test:

| Method | Returns |
|---|---|
| `result.getName()` | Test method name |
| `result.getTestClass().getName()` | Fully qualified class name |
| `result.getThrowable()` | The exception that caused failure |
| `result.getStartMillis()` | Start time in milliseconds |
| `result.getEndMillis()` | End time in milliseconds |
| `result.getStatus()` | 1=SUCCESS, 2=FAILURE, 3=SKIP |
| `result.getInstance()` | The test class instance |
| `result.getParameters()` | Parameters passed to the test method |

```java
@Override
public void onTestFailure(ITestResult result) {
    long duration = result.getEndMillis() - result.getStartMillis();
    System.out.println("FAILED: " + result.getName() +
                       " (after " + duration + "ms)");
    System.out.println("Error:  " + result.getThrowable().getMessage());
    System.out.println("Class:  " + result.getTestClass().getName());

    // Print parameters if it's a DataProvider test
    Object[] params = result.getParameters();
    if (params.length > 0) {
        System.out.print("Params: ");
        for (Object p : params) {
            System.out.print(p + " ");
        }
        System.out.println();
    }
}
```
