# 08 — Screenshots, Video, and Tracing

## Why Capture Screenshots and Videos?

When a test fails, the most valuable debugging tool is seeing **what the browser looked like** at the moment of failure. Playwright provides three levels of failure capture:

| Tool | What It Captures | Use Case |
|---|---|---|
| **Screenshot** | A static image of the page | Quick failure proof; attach to test report |
| **Video** | Full recording of the browser session | Replay exactly what happened |
| **Trace** | Screenshots + network + console + DOM snapshots | Deep debugging in Playwright Trace Viewer |

---

## Screenshots — page.screenshot()

### Basic Screenshot

```java
import com.microsoft.playwright.Page;
import java.nio.file.Paths;

// Take a screenshot of the visible viewport
page.screenshot(new Page.ScreenshotOptions()
    .setPath(Paths.get("screenshots/login-page.png"))
);
```

### Full Page Screenshot

Captures the entire page including content below the fold (scrolled content).

```java
// Full page screenshot — scrolls and stitches
page.screenshot(new Page.ScreenshotOptions()
    .setPath(Paths.get("screenshots/full-products-page.png"))
    .setFullPage(true)
);
```

### Element Screenshot

Capture only a specific element.

```java
// Screenshot of just the first product card
page.locator(".product-card").first()
    .screenshot(new Locator.ScreenshotOptions()
        .setPath(Paths.get("screenshots/first-product-card.png"))
    );

// Screenshot of the navigation bar
page.locator(".navbar")
    .screenshot(new Locator.ScreenshotOptions()
        .setPath(Paths.get("screenshots/navbar.png"))
    );
```

### Screenshot as Byte Array

Useful for attaching to test reports or comparing visually.

```java
// Returns the screenshot as byte[] — useful for reports
byte[] screenshotBytes = page.screenshot(
    new Page.ScreenshotOptions().setFullPage(true)
);
// Write to file manually
Files.write(Paths.get("screenshot.png"), screenshotBytes);
```

### Screenshot Options

```java
page.screenshot(new Page.ScreenshotOptions()
    .setPath(Paths.get("screenshots/page.png"))
    .setFullPage(true)                              // Capture full scroll height
    .setType(ScreenshotType.JPEG)                   // PNG (default) or JPEG
    .setQuality(85)                                 // JPEG quality 0-100 (JPEG only)
    .setClip(new Clip(0, 0, 800, 600))              // Crop to specific region
    .setAnimations(ScreenshotAnimations.DISABLED)   // Disable CSS animations
    .setOmitBackground(true)                        // Transparent background (PNG only)
);
```

---

## Screenshot on Test Failure

The most common pattern is to take a screenshot whenever a test fails. In TestNG, this is done via a `@AfterMethod` that checks the test result:

```java
package com.shopeasy.playwright.base;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BaseTest {

    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;

    protected static final String BASE_URL = "http://localhost:4200";

    @BeforeMethod
    public void setUp() {
        playwright = Playwright.create();
        browser    = playwright.chromium().launch(
            new BrowserType.LaunchOptions().setHeadless(false)
        );
        context = browser.newContext(
            new Browser.NewContextOptions().setViewportSize(1280, 720)
        );
        page = context.newPage();
    }

    @AfterMethod
    public void tearDown(ITestResult result) {
        // Take screenshot on failure
        if (result.getStatus() == ITestResult.FAILURE) {
            takeScreenshotOnFailure(result.getName());
        }

        if (page != null)       page.close();
        if (context != null)    context.close();
        if (browser != null)    browser.close();
        if (playwright != null) playwright.close();
    }

    private void takeScreenshotOnFailure(String testName) {
        try {
            // Create screenshots directory if it doesn't exist
            Files.createDirectories(Paths.get("test-output/screenshots"));

            // Timestamp for unique filename
            String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String fileName = "test-output/screenshots/FAIL_" + testName + "_" + timestamp + ".png";

            page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get(fileName))
                .setFullPage(true)
            );
            System.out.println("Screenshot saved: " + fileName);
        } catch (Exception e) {
            System.err.println("Failed to take screenshot: " + e.getMessage());
        }
    }

    protected void navigateTo(String route) {
        page.navigate(BASE_URL + "/#!/" + route);
    }
}
```

---

## Video Recording

Playwright can record a video of the entire browser session automatically.

### Enable Video in BrowserContext

```java
import java.nio.file.Paths;

// Enable video recording when creating the context
BrowserContext context = browser.newContext(
    new Browser.NewContextOptions()
        .setViewportSize(1280, 720)
        .setRecordVideoDir(Paths.get("test-output/videos/"))     // Save directory
        .setRecordVideoSize(new RecordVideoSize(1280, 720))      // Video resolution
);

Page page = context.newPage();
page.navigate("http://localhost:4200/#!/login");
// ... run your test ...

// IMPORTANT: Video is finalized when the context is CLOSED
context.close();

// Get the path to the recorded video
String videoPath = page.video().path().toString();
System.out.println("Video saved: " + videoPath);
```

> **Important:** You MUST call `context.close()` before accessing `page.video().path()`. The video file is not written until the context closes.

### Video in BaseTest — Record on Failure Only

```java
@BeforeMethod
public void setUp() {
    playwright = Playwright.create();
    browser    = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));

    // Always record video
    context = browser.newContext(
        new Browser.NewContextOptions()
            .setViewportSize(1280, 720)
            .setRecordVideoDir(Paths.get("test-output/videos/temp/"))
    );
    page = context.newPage();
}

@AfterMethod
public void tearDown(ITestResult result) {
    if (page != null) page.close();

    if (context != null) {
        // Get video path before closing context
        String videoPath = null;
        if (page != null && page.video() != null) {
            videoPath = page.video().path().toString();
        }
        context.close();   // Video finalized here

        // Move video on failure, delete on pass
        if (videoPath != null) {
            if (result.getStatus() == ITestResult.FAILURE) {
                // Keep the video for failed tests
                System.out.println("FAIL video: " + videoPath);
            } else {
                // Delete video for passing tests (save disk space)
                try { Files.deleteIfExists(Paths.get(videoPath)); }
                catch (Exception ignored) {}
            }
        }
    }

    if (browser != null)    browser.close();
    if (playwright != null) playwright.close();
}
```

---

## Tracing

Playwright Tracing is the most powerful debugging tool. A trace file captures:

- Screenshots at every action
- DOM snapshots (full HTML at each step)
- Network requests and responses
- Console messages
- Action timings

### Record a Trace

```java
// Start tracing before the test
context.tracing().start(new Tracing.StartOptions()
    .setScreenshots(true)   // Include screenshots at each step
    .setSnapshots(true)     // Include DOM snapshots
    .setSources(true)       // Include source code references
);

// ... run your test ...
page.navigate("http://localhost:4200/#!/login");
page.getByPlaceholder("Enter your username").fill("admin");
page.getByPlaceholder("Enter your password").fill("admin123");
page.locator("button[type='submit']").click();
page.waitForURL("**/#!/admin/dashboard");

// Stop tracing and save the .zip file
context.tracing().stop(new Tracing.StopOptions()
    .setPath(Paths.get("test-output/traces/admin-login.zip"))
);
```

### Tracing in BaseTest

```java
@BeforeMethod
public void setUp() {
    playwright = Playwright.create();
    browser    = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
    context    = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));

    // Start tracing for every test
    context.tracing().start(new Tracing.StartOptions()
        .setScreenshots(true)
        .setSnapshots(true)
    );

    page = context.newPage();
}

@AfterMethod
public void tearDown(ITestResult result) {
    // Save trace only on failure
    if (result.getStatus() == ITestResult.FAILURE) {
        try {
            Files.createDirectories(Paths.get("test-output/traces"));
            String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            context.tracing().stop(new Tracing.StopOptions()
                .setPath(Paths.get("test-output/traces/FAIL_"
                    + result.getName() + "_" + timestamp + ".zip"))
            );
            System.out.println("Trace saved for: " + result.getName());
        } catch (Exception e) {
            System.err.println("Failed to save trace: " + e.getMessage());
        }
    } else {
        context.tracing().stop();  // Stop without saving on pass
    }

    if (page != null)       page.close();
    if (context != null)    context.close();
    if (browser != null)    browser.close();
    if (playwright != null) playwright.close();
}
```

---

## Playwright Trace Viewer

Open a saved trace file in the Playwright Trace Viewer to debug visually:

```bash
# Open trace file in browser-based viewer
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI \
    -D exec.args="show-trace test-output/traces/FAIL_loginTest_20260327-103000.zip"
```

The Trace Viewer shows:

```
┌─────────────────────────────────────────────────────────┐
│  PLAYWRIGHT TRACE VIEWER                                 │
├──────────────────┬──────────────────────────────────────┤
│  Action Timeline │  Screenshot at selected step         │
│                  │                                      │
│  navigate()      │  [Browser Screenshot Here]           │
│  fill()          │                                      │
│  fill()          │                                      │
│  click()  ◄──── │  Shows page state at this moment     │
│  waitForURL()    │                                      │
├──────────────────┤                                      │
│  Network Calls   │                                      │
│  POST /api/login │                                      │
│  200 OK          │                                      │
├──────────────────┼──────────────────────────────────────┤
│  Console Logs    │  DOM Snapshot (hover to inspect)     │
└──────────────────┴──────────────────────────────────────┘
```

**What to look for when debugging:**
1. Click the failing action in the timeline
2. See exactly what the page looked like at that moment
3. Check network tab — did the API call fail?
4. Check console — any JavaScript errors?
5. Inspect DOM snapshot — was the element present?

---

## Headless Mode Configuration

Headless mode runs the browser without a visible window — essential for CI/CD pipelines.

```java
// Headless (no visible window) — for CI/CD
Browser browser = playwright.chromium().launch(
    new BrowserType.LaunchOptions()
        .setHeadless(true)        // Default is true in Playwright
);

// Headed (visible window) — for local development and debugging
Browser browser = playwright.chromium().launch(
    new BrowserType.LaunchOptions()
        .setHeadless(false)
        .setSlowMo(50)            // 50ms delay between actions (for observation)
);

// Recommended pattern — use system property for CI/CD flexibility
boolean headless = Boolean.parseBoolean(
    System.getProperty("headless", "true")  // default: headless
);
Browser browser = playwright.chromium().launch(
    new BrowserType.LaunchOptions().setHeadless(headless)
);

// Run tests headed from command line:
// mvn test -Dheadless=false
```

---

## Complete BaseTest with All Features

```java
package com.shopeasy.playwright.base;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.RecordVideoSize;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BaseTest {

    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;

    protected static final String BASE_URL = "http://localhost:4200";

    // Read from system property: mvn test -Dheadless=false
    private static final boolean HEADLESS =
        Boolean.parseBoolean(System.getProperty("headless", "true"));

    @BeforeMethod
    public void setUp() throws IOException {
        playwright = Playwright.create();
        browser    = playwright.chromium().launch(
            new BrowserType.LaunchOptions()
                .setHeadless(HEADLESS)
                .setSlowMo(HEADLESS ? 0 : 50)
        );

        // Create output directories
        Files.createDirectories(Paths.get("test-output/screenshots"));
        Files.createDirectories(Paths.get("test-output/traces"));
        Files.createDirectories(Paths.get("test-output/videos"));

        context = browser.newContext(
            new Browser.NewContextOptions()
                .setViewportSize(1280, 720)
                .setRecordVideoDir(Paths.get("test-output/videos/"))
        );

        // Start tracing
        context.tracing().start(
            new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
        );

        page = context.newPage();
    }

    @AfterMethod
    public void tearDown(ITestResult result) {
        String testName = result.getName();
        boolean failed  = result.getStatus() == ITestResult.FAILURE;
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));

        // Screenshot on failure
        if (failed && page != null) {
            try {
                page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get("test-output/screenshots/FAIL_" + testName + "_" + timestamp + ".png"))
                    .setFullPage(true)
                );
            } catch (Exception e) {
                System.err.println("Screenshot failed: " + e.getMessage());
            }
        }

        // Save trace on failure
        if (context != null) {
            try {
                if (failed) {
                    context.tracing().stop(new Tracing.StopOptions()
                        .setPath(Paths.get("test-output/traces/FAIL_" + testName + "_" + timestamp + ".zip"))
                    );
                } else {
                    context.tracing().stop();
                }
            } catch (Exception e) {
                System.err.println("Trace save failed: " + e.getMessage());
            }
        }

        // Close resources
        if (page != null)       page.close();
        if (context != null)    context.close();
        if (browser != null)    browser.close();
        if (playwright != null) playwright.close();
    }

    protected void navigateTo(String route) {
        page.navigate(BASE_URL + "/#!/" + route);
    }
}
```
