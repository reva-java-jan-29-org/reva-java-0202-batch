# 01 — Introduction to WebDriver & Setting Up the Environment

## What is Selenium?

Selenium is an open-source framework for automating web browsers. It allows you to write Java code that controls a real browser — clicking buttons, filling forms, reading text — just like a human would.

**Selenium has three main components:**

| Component | Purpose |
|---|---|
| **Selenium WebDriver** | Core API to control browsers programmatically |
| **Selenium Grid** | Run tests in parallel on multiple machines/browsers |
| **Selenium IDE** | Browser plugin to record/replay actions (not covered here) |

---

## Selenium 4 Architecture

```
Your Java Test Code
       │
       ▼
  Selenium WebDriver API (Java)
       │
       ▼ (W3C WebDriver Protocol over HTTP)
  Browser Driver (ChromeDriver / GeckoDriver / EdgeDriver)
       │
       ▼
  Actual Browser (Chrome / Firefox / Edge)
       │
       ▼
  Web Application (ShopEasy at http://localhost:4200)
```

**Key Change in Selenium 4:** Uses the W3C WebDriver standard protocol (Selenium 3 used JSONWireProtocol). Selenium 4 also includes built-in **Selenium Manager** that auto-downloads the correct browser driver — no manual setup needed.

---

## Step 1: Create the Maven Test Project

Create a new Maven project (in IntelliJ: File → New Project → Maven):

- **GroupId:** `com.shopeasy`
- **ArtifactId:** `shopeasy-selenium-tests`
- **Java Version:** 17

---

## Step 2: Configure `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.shopeasy</groupId>
    <artifactId>shopeasy-selenium-tests</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <selenium.version>4.18.1</selenium.version>
        <testng.version>7.9.0</testng.version>
    </properties>

    <dependencies>

        <!-- ── Selenium WebDriver ──────────────────────────────── -->
        <!-- selenium-java includes all browser bindings + support classes -->
        <dependency>
            <groupId>org.seleniumhq.selenium</groupId>
            <artifactId>selenium-java</artifactId>
            <version>${selenium.version}</version>
        </dependency>

        <!-- ── TestNG ─────────────────────────────────────────── -->
        <!-- Test framework: annotations, assertions, test runners -->
        <dependency>
            <groupId>org.testng</groupId>
            <artifactId>testng</artifactId>
            <version>${testng.version}</version>
            <scope>test</scope>
        </dependency>

    </dependencies>

    <build>
        <plugins>
            <!-- Surefire plugin runs TestNG tests via mvn test -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>
        </plugins>
    </build>

</project>
```

> **Note:** Selenium 4.6+ includes **Selenium Manager** built-in. It automatically downloads the correct ChromeDriver matching your Chrome version. You do NOT need to add WebDriverManager or set `webdriver.chrome.driver` system property.

---

## Step 3: Create the Base Test Class

The Base Test class contains shared setup (open browser before test) and teardown (close browser after test). All your test classes will extend this.

Create file: `src/test/java/com/shopeasy/tests/base/BaseTest.java`

```java
package com.shopeasy.tests.base;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.time.Duration;

public class BaseTest {

    // WebDriver instance — one per test method (thread-safe for now)
    protected WebDriver driver;

    // Base URL of the ShopEasy frontend
    protected static final String BASE_URL = "http://localhost:4200";

    @BeforeMethod
    public void setUp() {
        // ChromeOptions lets you configure Chrome behavior
        ChromeOptions options = new ChromeOptions();

        // Run Chrome in a standard window
        // For headless mode, see: 18-screenshots-ssl-headless.md

        // Create ChromeDriver — Selenium Manager auto-downloads ChromeDriver
        driver = new ChromeDriver(options);

        // Maximize the browser window
        driver.manage().window().maximize();

        // Implicit wait: if an element is not found immediately,
        // wait up to 10 seconds before throwing NoSuchElementException
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        // Page load timeout: wait up to 30 seconds for a page to fully load
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
    }

    @AfterMethod
    public void tearDown() {
        // Always close the browser after each test method
        // driver.close() — closes current window only
        // driver.quit() — closes ALL windows AND kills the driver process
        if (driver != null) {
            driver.quit();
        }
    }

    // ── Helper methods ────────────────────────────────────────────────

    /**
     * Navigate to a specific route in the ShopEasy SPA.
     * Examples: navigateTo("login"), navigateTo("products"), navigateTo("admin/dashboard")
     */
    protected void navigateTo(String route) {
        driver.get(BASE_URL + "/#!/" + route);
    }
}
```

---

## Step 4: Write Your First Test

Create file: `src/test/java/com/shopeasy/tests/FirstTest.java`

```java
package com.shopeasy.tests;

import com.shopeasy.tests.base.BaseTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

public class FirstTest extends BaseTest {

    @Test
    public void verifyPageTitle() {
        // Step 1: Open the ShopEasy login page
        driver.get(BASE_URL + "/#!/login");

        // Step 2: Get the page title from the browser tab
        String title = driver.getTitle();
        System.out.println("Page title: " + title);

        // Step 3: Assert it matches the expected title
        Assert.assertEquals(title, "ShopEasy - E-Commerce",
            "Page title did not match!");
    }

    @Test
    public void verifyLoginFormVisible() {
        // Navigate to login page
        driver.get(BASE_URL + "/#!/login");

        // Find the username input using CSS attribute selector
        // (no id on this input — common in AngularJS apps)
        WebElement usernameInput = driver.findElement(
            By.cssSelector("input[placeholder='Enter your username']")
        );

        WebElement passwordInput = driver.findElement(
            By.cssSelector("input[type='password']")
        );

        WebElement loginButton = driver.findElement(
            By.cssSelector("button[type='submit']")
        );

        // Assert elements are visible on the page
        Assert.assertTrue(usernameInput.isDisplayed(), "Username field not visible");
        Assert.assertTrue(passwordInput.isDisplayed(), "Password field not visible");
        Assert.assertTrue(loginButton.isDisplayed(), "Login button not visible");

        // Print element tag for reference
        System.out.println("Username input tag: " + usernameInput.getTagName());
    }
}
```

---

## Step 5: Run the Test

**From IntelliJ:** Right-click `FirstTest.java` → Run

**From terminal:**
```bash
mvn test -Dtest=FirstTest
```

**Expected output:**
```
Page title: ShopEasy - E-Commerce
Username input tag: input
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
```

---

## WebDriver Core Interfaces

| Interface/Class | What It Represents |
|---|---|
| `WebDriver` | The browser itself |
| `WebElement` | A single HTML element on the page |
| `By` | Factory for creating locator strategies |
| `ChromeDriver` | Chrome-specific WebDriver implementation |
| `ChromeOptions` | Configuration options for Chrome |

---

## WebDriver vs Browser Driver vs Browser

```
WebDriver (your Java code)
    └── ChromeDriver (Java implementation of W3C WebDriver)
             └── ChromeDriver binary (separate process, auto-managed)
                      └── Google Chrome (the actual browser)
```

- **`driver.get(url)`** → tells Chrome to navigate to a URL
- **`driver.findElement(By.cssSelector(...))`** → finds an HTML element
- **`element.click()`** → simulates a mouse click
- **`element.sendKeys("text")`** → types text into a field
- **`driver.quit()`** → closes Chrome and the ChromeDriver process

---

## Common WebDriver Exceptions

| Exception | Cause | Fix |
|---|---|---|
| `NoSuchElementException` | Element not found in DOM | Check locator; add wait |
| `StaleElementReferenceException` | Element found but DOM changed | Re-find the element |
| `ElementNotInteractableException` | Element exists but not clickable/visible | Scroll into view; wait for visibility |
| `TimeoutException` | Wait expired before condition met | Increase wait; check page loaded |
| `SessionNotFoundException` | Browser was closed unexpectedly | Ensure tearDown runs; check `driver.quit()` |
