# 01 — Introduction to Playwright & Setting Up the Environment

## What is Playwright?

Playwright is a modern open-source browser automation framework developed by **Microsoft**. It allows you to write Java (or Python/JavaScript/C#) code that controls a real browser — clicking buttons, filling forms, intercepting network requests, taking screenshots — reliably and without flakiness.

**Playwright's key design goals:**

| Goal | How Playwright Achieves It |
|---|---|
| No flaky tests | Auto-waits for elements to be ready before acting |
| Fast execution | Uses browser DevTools Protocol (not HTTP) |
| Multi-browser | Chromium, Firefox, and WebKit from one API |
| Modern web support | SPAs, shadow DOM, iframes, service workers |
| Built-in utilities | Screenshots, video, tracing, network mock, API testing |

---

## Playwright vs Selenium — Detailed Comparison

| Aspect | Playwright | Selenium |
|---|---|---|
| **Protocol** | Browser DevTools Protocol (CDP / WebSocket) | W3C WebDriver over HTTP |
| **Auto-waiting** | Built-in on every action | Manual — requires WebDriverWait |
| **Browsers** | Chromium, Firefox, WebKit (bundled) | Chrome, Firefox, Edge, Safari (external drivers) |
| **Driver setup** | No driver binary needed — browsers bundled | ChromeDriver / GeckoDriver must match browser version |
| **Languages** | Java, TypeScript/JavaScript, Python, C#, .NET | Java, TypeScript/JavaScript, Python, C#, Ruby |
| **API testing** | `APIRequestContext` — built-in | Not supported natively |
| **Screenshots** | `page.screenshot()` — full page or element | Cast to `TakesScreenshot` |
| **Video recording** | `BrowserContext` option — built-in | Requires external tools |
| **Tracing** | `context.tracing` — built-in Trace Viewer | No equivalent |
| **Network mocking** | `page.route()` — built-in | Requires BrowserMob Proxy |
| **Parallel isolation** | `BrowserContext` (lightweight) | New `WebDriver` instance per thread |
| **Shadow DOM** | `locator.shadowRoot()` — first-class support | `getShadowRoot()` in Selenium 4 |
| **Mobile emulation** | Device descriptors built-in | `ChromeOptions` with `mobileEmulation` |
| **Release** | 2020 (Microsoft) | 2004 (ThoughtWorks / Software Freedom Conservancy) |

---

## Playwright Architecture

```
Your Java Test Code
       │
       ▼
  Playwright Java API
       │
       ▼ (WebSocket — Browser DevTools Protocol)
  Playwright Server (Node.js — bundled in jar)
       │
       ▼
  Bundled Browser (Chromium / Firefox / WebKit)
       │
       ▼
  Web Application (ShopEasy at http://localhost:4200)
```

**Key difference from Selenium:** Playwright uses a persistent **WebSocket connection** to the browser, not HTTP requests. This makes communication faster and allows Playwright to listen to browser events in real time (network requests, console logs, page errors).

---

## Step 1: Create the Maven Test Project

Create a new Maven project (IntelliJ: File → New Project → Maven):

- **GroupId:** `com.shopeasy`
- **ArtifactId:** `shopeasy-playwright-tests`
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
    <artifactId>shopeasy-playwright-tests</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <playwright.version>1.43.0</playwright.version>
        <testng.version>7.9.0</testng.version>
    </properties>

    <dependencies>

        <!-- ── Playwright for Java ─────────────────────────────────── -->
        <!-- Includes bundled Node.js server + browser download tooling -->
        <dependency>
            <groupId>com.microsoft.playwright</groupId>
            <artifactId>playwright</artifactId>
            <version>${playwright.version}</version>
        </dependency>

        <!-- ── TestNG ─────────────────────────────────────────────── -->
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

> **Note:** The `playwright` jar includes a bundled Node.js runtime. You do NOT need to install Node.js to run tests. Playwright manages its own browser binaries separately from your system browsers.

---

## Step 3: Install Browser Binaries

After adding the dependency, run this command once to download Chromium (and optionally Firefox/WebKit):

```bash
# Install only Chromium (recommended — smallest download)
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"

# Install all browsers (Chromium + Firefox + WebKit)
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"
```

**What this does:**
- Downloads a specific Playwright-tested version of Chromium (~300MB)
- Stores it in `~/.cache/ms-playwright/` (macOS/Linux) or `%LOCALAPPDATA%\ms-playwright\` (Windows)
- The same browser version is used on all machines — no "works on my machine" issues

**Verify installation:**
```bash
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="--version"
```

---

## Step 4: Understand the Core Objects

Before writing tests, understand the four main Playwright objects:

```
Playwright          ← Entry point — creates browsers
    └── Browser     ← A browser instance (Chromium/Firefox/WebKit)
            └── BrowserContext  ← An isolated browser session (like an incognito window)
                        └── Page    ← A single browser tab
```

| Object | Analogy | Lifecycle |
|---|---|---|
| `Playwright` | The factory | Create once per JVM / test suite |
| `Browser` | One Chrome process | Create once per test suite |
| `BrowserContext` | One incognito session | Create per test — ensures isolation |
| `Page` | One browser tab | Create per test — the main interaction object |

---

## Step 5: Create the Base Test Class

Create file: `src/test/java/com/shopeasy/playwright/base/BaseTest.java`

```java
package com.shopeasy.playwright.base;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public class BaseTest {

    // ── Playwright object hierarchy ──────────────────────────────────
    protected Playwright playwright;    // Entry point — one per test run
    protected Browser     browser;      // The browser process
    protected BrowserContext context;   // Isolated session (like incognito)
    protected Page        page;         // The browser tab you interact with

    // Base URL of the ShopEasy frontend
    protected static final String BASE_URL = "http://localhost:4200";

    @BeforeMethod
    public void setUp() {
        // Step 1: Create the Playwright entry point
        playwright = Playwright.create();

        // Step 2: Launch Chromium (non-headless by default — you can see the browser)
        browser = playwright.chromium().launch(
            new BrowserType.LaunchOptions()
                .setHeadless(false)         // true = no visible window (for CI)
                .setSlowMo(0)               // add delay (ms) between actions for debugging
        );

        // Step 3: Create an isolated browser context
        // Each test gets a fresh context — no cookies, no localStorage from other tests
        context = browser.newContext(
            new Browser.NewContextOptions()
                .setViewportSize(1280, 720)   // Browser window size
        );

        // Step 4: Create a new page (tab) inside the context
        page = context.newPage();
    }

    @AfterMethod
    public void tearDown() {
        // Close in reverse order: Page → Context → Browser → Playwright
        if (page != null)       page.close();
        if (context != null)    context.close();
        if (browser != null)    browser.close();
        if (playwright != null) playwright.close();
    }

    // ── Helper methods ────────────────────────────────────────────────

    /**
     * Navigate to a specific route in the ShopEasy SPA.
     * Examples: navigateTo("login"), navigateTo("products"), navigateTo("admin/dashboard")
     */
    protected void navigateTo(String route) {
        page.navigate(BASE_URL + "/#!/" + route);
    }
}
```

---

## Step 6: Write Your First Test

Create file: `src/test/java/com/shopeasy/playwright/tests/FirstTest.java`

```java
package com.shopeasy.playwright.tests;

import com.shopeasy.playwright.base.BaseTest;
import com.microsoft.playwright.Locator;
import org.testng.Assert;
import org.testng.annotations.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class FirstTest extends BaseTest {

    @Test
    public void verifyPageTitle() {
        // Step 1: Navigate to the ShopEasy login page
        page.navigate(BASE_URL + "/#!/login");

        // Step 2: Get the page title from the browser tab
        String title = page.title();
        System.out.println("Page title: " + title);

        // Step 3: Assert using Playwright's built-in assertThat
        assertThat(page).hasTitle("ShopEasy - E-Commerce");
    }

    @Test
    public void verifyLoginFormVisible() {
        // Navigate to login page
        navigateTo("login");

        // Playwright auto-waits for these elements to be visible before asserting
        // No explicit wait code needed!
        Locator usernameInput = page.locator("input[placeholder='Enter your username']");
        Locator passwordInput = page.locator("input[type='password']");
        Locator loginButton   = page.locator("button[type='submit']");

        // Assert elements are visible — Playwright waits up to 30s automatically
        assertThat(usernameInput).isVisible();
        assertThat(passwordInput).isVisible();
        assertThat(loginButton).isVisible();

        System.out.println("All login form elements are visible");
    }
}
```

---

## Step 7: Run the Test

**From IntelliJ:** Right-click `FirstTest.java` → Run

**From terminal:**
```bash
mvn test -Dtest=FirstTest
```

**Expected output:**
```
Page title: ShopEasy - E-Commerce
All login form elements are visible
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
```

---

## Core Playwright Interfaces

| Class/Interface | What It Represents |
|---|---|
| `Playwright` | Entry point factory — creates browser types |
| `Browser` | A running browser process |
| `BrowserContext` | An isolated browsing session |
| `Page` | A browser tab — primary interaction surface |
| `Locator` | A reference to elements (lazy — evaluated on action) |
| `BrowserType` | `playwright.chromium()`, `playwright.firefox()`, `playwright.webkit()` |

---

## Common Playwright Exceptions

| Exception | Cause | Fix |
|---|---|---|
| `TimeoutError` | Element not ready within timeout (default 30s) | Check locator; increase timeout; verify app is running |
| `PlaywrightException` | Generic error — browser crashed or page closed | Check tearDown; verify browser is open |
| Element not found | Locator matches nothing | Use Playwright Inspector to find correct selector |

---

## Playwright Inspector — Debug Tool

Run any test in debug mode to step through actions visually:

```bash
# macOS/Linux
PWDEBUG=1 mvn test -Dtest=FirstTest

# Windows
set PWDEBUG=1 && mvn test -Dtest=FirstTest
```

The Playwright Inspector window opens alongside the browser, showing each action step by step — invaluable for finding correct selectors.
