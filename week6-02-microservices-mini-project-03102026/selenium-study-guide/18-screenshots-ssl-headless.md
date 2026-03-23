# 18 — Capture Screenshots, Handle SSL, Headless Browser, Blocking Ads & Chrome Extensions

## Part A: Capture Screenshots

## Why Take Screenshots?

- Capture visual evidence of test failures
- Debug unexpected UI states
- Attach to test reports (Allure, Extent Reports)
- Verify visual regression

---

## Taking a Full-Page Screenshot

```java
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
```

### Basic Screenshot

```java
// Cast driver to TakesScreenshot interface
TakesScreenshot screenshot = (TakesScreenshot) driver;

// Get screenshot as a File
File srcFile = screenshot.getScreenshotAs(OutputType.FILE);

// Save to a specific location
String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
String destPath = "screenshots/screenshot_" + timestamp + ".png";

// Create directories if they don't exist
new File("screenshots").mkdirs();
Files.copy(srcFile.toPath(), Paths.get(destPath));

System.out.println("Screenshot saved: " + destPath);
```

### Screenshot as Byte Array (for test reports)

```java
byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
// Attach to Allure, Extent Reports, or TestNG listeners
```

### Screenshot as Base64 String (for HTML embedding)

```java
String base64Screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
// Embed in HTML: <img src="data:image/png;base64,{base64Screenshot}">
```

---

## Screenshot of a Specific Element (Selenium 4+)

```java
// Capture only the product card, not the entire page
WebElement productCard = driver.findElement(By.cssSelector(".product-card"));
File elementScreenshot = productCard.getScreenshotAs(OutputType.FILE);

Files.copy(elementScreenshot.toPath(), Paths.get("screenshots/product-card.png"));
System.out.println("Element screenshot saved");
```

---

## Take Screenshot on Test Failure (TestNG Listener)

Create a listener that captures screenshots automatically on failure:

```java
// src/test/java/com/shopeasy/tests/listeners/ScreenshotListener.java
package com.shopeasy.tests.listeners;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ScreenshotListener implements ITestListener {

    @Override
    public void onTestFailure(ITestResult result) {
        // Get the driver from the test instance
        Object testInstance = result.getInstance();
        try {
            WebDriver driver = (WebDriver) testInstance.getClass()
                .getDeclaredField("driver")
                .get(testInstance);

            if (driver != null) {
                String testName = result.getMethod().getMethodName();
                String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String fileName = "screenshots/FAILED_" + testName + "_" + timestamp + ".png";

                new File("screenshots").mkdirs();
                File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                Files.copy(srcFile.toPath(), Paths.get(fileName));

                System.out.println("Screenshot on failure: " + fileName);
            }
        } catch (Exception e) {
            System.err.println("Failed to take screenshot: " + e.getMessage());
        }
    }
}
```

Register the listener in `testng.xml`:
```xml
<listeners>
    <listener class-name="com.shopeasy.tests.listeners.ScreenshotListener"/>
</listeners>
```

Or on the test class:
```java
@Listeners(ScreenshotListener.class)
public class LoginTest extends BaseTest { ... }
```

---

## BaseTest with Built-In Screenshot on Failure

```java
@AfterMethod
public void tearDown(ITestResult result) {
    // Take screenshot if test failed
    if (result.getStatus() == ITestResult.FAILURE) {
        takeScreenshot(result.getMethod().getMethodName());
    }
    if (driver != null) {
        driver.quit();
    }
}

private void takeScreenshot(String testName) {
    try {
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String path = "screenshots/FAILED_" + testName + "_" + timestamp + ".png";
        new File("screenshots").mkdirs();
        File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        Files.copy(srcFile.toPath(), Paths.get(path));
        System.out.println("Screenshot: " + path);
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

---

## Part B: Handle SSL Certificates

## Why SSL Issues Occur in Tests

Local development often uses self-signed or expired SSL certificates. Chrome blocks these by default with a "Your connection is not private" warning page.

---

## Accept Insecure Certificates (Selenium 4)

```java
ChromeOptions options = new ChromeOptions();

// Accept all SSL certificates (including self-signed)
options.setAcceptInsecureCerts(true);

driver = new ChromeDriver(options);
```

---

## Handle the SSL Warning Page Manually

If you already navigated to a broken SSL page:

```java
// The Chrome SSL error page shows a "Proceed to ... (unsafe)" link
// Its element ID changes by Chrome version — one common approach:
try {
    WebElement proceedLink = driver.findElement(By.id("proceed-link"));
    proceedLink.click();
} catch (NoSuchElementException e) {
    // Try via JavaScript (bypasses the UI)
    ((JavascriptExecutor) driver).executeScript(
        "document.getElementById('proceed-link').click();"
    );
}
```

**Best approach:** Use `setAcceptInsecureCerts(true)` — it prevents the error page from ever appearing.

---

## Part C: Headless Browser

## What Is Headless Mode?

Running Chrome without a visible UI window. Headless is faster, uses less memory, and is required for CI/CD pipelines (no display server on Jenkins/Docker).

---

## Configure Headless Chrome (Selenium 4 + Chrome 112+)

```java
ChromeOptions options = new ChromeOptions();

// New headless mode (Chrome 112+) — preferred
options.addArguments("--headless=new");

// Or legacy headless (Chrome < 112)
// options.addArguments("--headless");
// options.addArguments("--disable-gpu"); // Required on Windows

// Additional args for stable headless execution
options.addArguments("--window-size=1920,1080");      // Set viewport size
options.addArguments("--no-sandbox");                  // Required in Docker
options.addArguments("--disable-dev-shm-usage");      // Overcome limited resource in Docker

driver = new ChromeDriver(options);
driver.manage().window().setSize(new Dimension(1920, 1080));
```

---

## Toggle Headless in BaseTest (Configurable)

```java
public class BaseTest {
    protected WebDriver driver;
    // Set to true for CI/CD, false for local debugging
    private static final boolean HEADLESS = Boolean.parseBoolean(
        System.getProperty("headless", "false")
    );

    @BeforeMethod
    public void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(true);

        if (HEADLESS) {
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
        }

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        // Note: maximize() in headless does nothing — use setSize()
        if (HEADLESS) {
            driver.manage().window().setSize(new Dimension(1920, 1080));
        }
    }
}
```

Run headless from command line:
```bash
mvn test -Dheadless=true
```

---

## Part D: Blocking Ads & Chrome Extensions

## Blocking Ads in Tests

Ads can interfere with tests by overlaying elements. Options:

### Option 1: Use `--disable-extensions`

```java
ChromeOptions options = new ChromeOptions();
options.addArguments("--disable-extensions");
options.addArguments("--disable-plugins");
```

### Option 2: Load uBlock Origin Extension

```java
ChromeOptions options = new ChromeOptions();

// Download uBlock Origin .crx file and load it
File extensionFile = new File("extensions/ublock_origin.crx");
options.addExtensions(extensionFile);
```

### Option 3: Block URLs via Chrome DevTools Protocol (Selenium 4)

```java
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v120.network.Network;
import java.util.Optional;

ChromeDriver chromeDriver = new ChromeDriver();
DevTools devTools = chromeDriver.getDevTools();
devTools.createSession();

// Enable network interception
devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));

// Block ad domains
devTools.send(Network.setBlockedURLs(java.util.List.of(
    "*googlesyndication*",
    "*doubleclick*",
    "*adnxs*",
    "*ads.google*"
)));
System.out.println("Ad URLs blocked");
```

---

## Managing Chrome Extensions

### Load an Extension by Path

```java
ChromeOptions options = new ChromeOptions();

// Load unpacked extension (development mode)
File extensionDir = new File("extensions/my-extension");
options.addArguments("--load-extension=" + extensionDir.getAbsolutePath());

// Load packed .crx extension
options.addExtensions(new File("extensions/my-extension.crx"));
```

### Disable All Extensions

```java
ChromeOptions options = new ChromeOptions();
options.addArguments("--disable-extensions");
```

### Disable Specific Extension

```java
ChromeOptions options = new ChromeOptions();
// Extensions are identified by their IDs
options.addArguments("--disable-extensions-except=extension-id-here");
```

---

## Part E: Other Useful Chrome Arguments

```java
ChromeOptions options = new ChromeOptions();

// Performance & stability
options.addArguments("--no-sandbox");
options.addArguments("--disable-dev-shm-usage");
options.addArguments("--disable-gpu");
options.addArguments("--disable-extensions");

// Network
options.addArguments("--ignore-certificate-errors");
options.addArguments("--allow-insecure-localhost");

// UI
options.addArguments("--start-maximized");
options.addArguments("--window-size=1920,1080");
options.addArguments("--disable-infobars");             // Hide "Chrome is being controlled" bar
options.addArguments("--disable-notifications");        // Disable notification permission popups

// Logging
options.addArguments("--log-level=3");                  // Suppress Chrome logs
options.setExperimentalOption("excludeSwitches", java.util.List.of("enable-automation"));

// Disable popup blocker
options.addArguments("--disable-popup-blocking");
```

---

## Complete BaseTest with All Options

```java
public class BaseTest {
    protected WebDriver driver;
    protected WebDriverWait wait;
    protected static final String BASE_URL = "http://localhost:4200";
    private static final boolean HEADLESS =
        Boolean.parseBoolean(System.getProperty("headless", "false"));

    @BeforeMethod
    public void setUp() {
        ChromeOptions options = buildChromeOptions();
        driver = new ChromeDriver(options);

        if (HEADLESS) {
            driver.manage().window().setSize(new Dimension(1920, 1080));
        } else {
            driver.manage().window().maximize();
        }

        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    private ChromeOptions buildChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(true);
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--disable-infobars");
        options.setExperimentalOption("excludeSwitches",
            java.util.List.of("enable-automation"));

        if (HEADLESS) {
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
        }
        return options;
    }

    @AfterMethod
    public void tearDown(ITestResult result) {
        if (ITestResult.FAILURE == result.getStatus()) {
            takeScreenshot("FAILED_" + result.getMethod().getMethodName());
        }
        if (driver != null) driver.quit();
    }

    protected void takeScreenshot(String name) {
        try {
            new File("screenshots").mkdirs();
            String path = "screenshots/" + name + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss")) + ".png";
            Files.copy(
                ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE).toPath(),
                Paths.get(path)
            );
            System.out.println("Screenshot: " + path);
        } catch (Exception e) { e.printStackTrace(); }
    }

    protected void navigateTo(String route) {
        driver.get(BASE_URL + "/#!/" + route);
    }
}
```
