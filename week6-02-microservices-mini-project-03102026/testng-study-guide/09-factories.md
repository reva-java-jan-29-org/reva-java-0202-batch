# 09 — `@Factory` — Creating Multiple Test Instances

## What is `@Factory`?

`@Factory` is a TestNG annotation that marks a method as a **test object factory**. A factory method returns an array of test class instances (`Object[]`). TestNG runs all `@Test` methods on every returned instance.

**Core idea:** Instead of writing a test class once and running it with one configuration, `@Factory` creates multiple instances of the same class, each with a different configuration (browser, user role, environment, etc.).

---

## `@Factory` vs `@DataProvider` — Core Difference

| | `@DataProvider` | `@Factory` |
|---|---|---|
| What it creates | Multiple **invocations** of one method | Multiple **instances** of a whole class |
| Scope | One `@Test` method gets multiple data rows | ALL `@Test` methods in the class run for each instance |
| Configuration | Data passed per-method | Configuration set in constructor |
| Use case | Test one scenario with many input values | Test ALL scenarios with different configurations (browsers, users) |

**Analogy:**
- `@DataProvider` → "Run this one test 5 times with different data"
- `@Factory` → "Run this entire test class 3 times — once for Chrome, once for Firefox, once for Edge"

---

## Basic `@Factory` Example

**Step 1: Create a test class with a constructor that accepts configuration:**

```java
package com.shopeasy.tests;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.Duration;

/**
 * LoginTest — designed to be instantiated by a @Factory.
 * Each instance gets a different browser name in its constructor.
 */
public class LoginTest {

    private final String browser;   // set by factory constructor
    private WebDriver driver;
    private static final String BASE_URL = "http://localhost:4200";

    // Constructor receives the browser name
    public LoginTest(String browser) {
        this.browser = browser;
    }

    @BeforeClass
    public void setUp() {
        System.out.println("Setting up browser: " + browser);
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
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (driver != null) driver.quit();
    }

    @Test(description = "Verify login page title")
    public void verifyLoginPageTitle() {
        driver.get(BASE_URL + "/#!/login");
        Assert.assertEquals(driver.getTitle(), "ShopEasy - E-Commerce",
            "Title check on browser: " + browser);
    }

    @Test(description = "Verify login form is present")
    public void verifyLoginForm() {
        driver.get(BASE_URL + "/#!/login");
        Assert.assertTrue(
            driver.findElement(
                By.cssSelector("input[placeholder='Enter your username']")
            ).isDisplayed(),
            "Username field check on browser: " + browser
        );
    }
}
```

**Step 2: Create the factory class:**

```java
package com.shopeasy.tests;

import org.testng.annotations.Factory;

/**
 * Factory that creates one LoginTest instance per browser.
 * All @Test methods in LoginTest will run for each browser.
 */
public class CrossBrowserFactory {

    @Factory
    public Object[] createCrossBrowserTests() {
        return new Object[] {
            new LoginTest("chrome"),    // Instance 1: runs all @Test on Chrome
            new LoginTest("firefox"),   // Instance 2: runs all @Test on Firefox
        };
    }
}
```

**Step 3: Reference the factory in `testng.xml`:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">

<suite name="Cross-Browser Suite">
    <test name="Cross Browser Login Tests">
        <classes>
            <!-- Point to the factory, not the test class -->
            <class name="com.shopeasy.tests.CrossBrowserFactory"/>
        </classes>
    </test>
</suite>
```

**What TestNG runs:**
```
Instance 1 (chrome):
  verifyLoginPageTitle()   ← Chrome
  verifyLoginForm()        ← Chrome

Instance 2 (firefox):
  verifyLoginPageTitle()   ← Firefox
  verifyLoginForm()        ← Firefox

Total: 4 tests (2 test methods × 2 browsers)
```

---

## `@Factory` Inside the Test Class

The factory method can live inside the test class itself:

```java
package com.shopeasy.tests;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.time.Duration;

public class ProductTest {

    private final String browser;
    private WebDriver driver;
    private static final String BASE_URL = "http://localhost:4200";

    public ProductTest(String browser) {
        this.browser = browser;
    }

    // Factory method inside the test class
    @Factory
    public static Object[] factory() {
        return new Object[] {
            new ProductTest("chrome"),
            new ProductTest("firefox"),
        };
    }

    @BeforeClass
    public void setUp() {
        driver = "firefox".equals(browser)
            ? new FirefoxDriver()
            : new ChromeDriver();
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (driver != null) driver.quit();
    }

    @Test(description = "Products page loads with results")
    public void verifyProductsPageLoads() {
        driver.get(BASE_URL + "/#!/products");
        Assert.assertEquals(driver.getTitle(), "ShopEasy - E-Commerce",
            "Title on " + browser);
        Assert.assertTrue(
            driver.findElements(By.cssSelector(".card")).size() > 0,
            "Should show products on " + browser
        );
    }

    @Test(description = "Product search works")
    public void verifyProductSearch() {
        driver.get(BASE_URL + "/#!/products");
        driver.findElement(By.cssSelector("input[placeholder]")).sendKeys("laptop");
        try { Thread.sleep(500); } catch (InterruptedException e) { }
        Assert.assertTrue(
            driver.findElements(By.cssSelector(".card")).size() > 0,
            "Search should return results on " + browser
        );
    }
}
```

---

## `@Factory` with `@DataProvider`

A factory method can use a `@DataProvider` to generate its constructor arguments. This is the most powerful pattern:

```java
package com.shopeasy.tests;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.time.Duration;

public class MultiConfigTest {

    private final String browser;
    private final String username;
    private final String password;
    private WebDriver driver;
    private static final String BASE_URL = "http://localhost:4200";

    // Constructor receives all configuration
    public MultiConfigTest(String browser, String username, String password) {
        this.browser  = browser;
        this.username = username;
        this.password = password;
    }

    // DataProvider for the factory: defines which configurations to create
    @DataProvider(name = "testConfigurations")
    public static Object[][] configurations() {
        return new Object[][] {
            // browser,    username,     password
            { "chrome",   "testuser",   "password123" },
            { "chrome",   "admin",      "admin123"    },
            { "firefox",  "testuser",   "password123" },
        };
    }

    // Factory uses the DataProvider to create instances
    @Factory(dataProvider = "testConfigurations")
    public MultiConfigTest(String browser, String username, String password,
                           // Factory constructor must match DataProvider row
                           String ignored) {
        // This is a duplicate — see the cleaner pattern below
        this.browser  = browser;
        this.username = username;
        this.password = password;
    }

    @BeforeClass
    public void setUp() {
        driver = "firefox".equals(browser) ? new FirefoxDriver() : new ChromeDriver();
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (driver != null) driver.quit();
    }

    @Test
    public void verifyLogin() {
        driver.get(BASE_URL + "/#!/login");
        driver.findElement(
            By.cssSelector("input[placeholder='Enter your username']")
        ).sendKeys(username);
        driver.findElement(
            By.cssSelector("input[type='password']")
        ).sendKeys(password);
        driver.findElement(
            By.cssSelector("button[type='submit']")
        ).click();
        Assert.assertTrue(driver.getCurrentUrl().contains("products")
                       || driver.getCurrentUrl().contains("admin"),
            "Should redirect after login for " + username + " on " + browser);
    }
}
```

**Cleaner pattern — separate factory class with DataProvider:**

```java
package com.shopeasy.tests;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;

public class ShopEasyTestFactory {

    @DataProvider(name = "browserConfigs")
    public static Object[][] browserConfigs() {
        return new Object[][] {
            { "chrome"  },
            { "firefox" },
        };
    }

    // @Factory that uses @DataProvider
    @Factory(dataProvider = "browserConfigs")
    public Object[] createTests(String browser) {
        return new Object[] {
            new LoginTest(browser),
            new ProductTest(browser),
            new CartTest(browser),
        };
    }
}
```

---

## Cross-Browser Testing with `@Factory` — Complete Example

```java
package com.shopeasy.tests;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.time.Duration;

/**
 * Full cross-browser test class using @Factory.
 * All @Test methods run on Chrome, Firefox, and Edge.
 */
public class CrossBrowserLoginTest {

    private final String browser;
    private WebDriver driver;
    private static final String BASE_URL = "http://localhost:4200";

    public CrossBrowserLoginTest(String browser) {
        this.browser = browser;
    }

    @Factory
    public static Object[] createInstances() {
        return new Object[] {
            new CrossBrowserLoginTest("chrome"),
            new CrossBrowserLoginTest("firefox"),
            new CrossBrowserLoginTest("edge"),
        };
    }

    @BeforeClass
    public void setUp() {
        System.out.println(">>> Starting browser: " + browser.toUpperCase());
        switch (browser.toLowerCase()) {
            case "firefox":
                driver = new FirefoxDriver();
                break;
            case "edge":
                driver = new EdgeDriver();
                break;
            case "chrome":
            default:
                driver = new ChromeDriver();
                break;
        }
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        System.out.println(">>> Closing browser: " + browser.toUpperCase());
        if (driver != null) driver.quit();
    }

    @Test(description = "Verify ShopEasy title on all browsers")
    public void verifyPageTitle() {
        driver.get(BASE_URL + "/#!/login");
        Assert.assertEquals(
            driver.getTitle(),
            "ShopEasy - E-Commerce",
            "Title check failed on " + browser
        );
    }

    @Test(description = "Verify login form renders on all browsers")
    public void verifyLoginFormRenders() {
        driver.get(BASE_URL + "/#!/login");
        Assert.assertTrue(
            driver.findElement(
                By.cssSelector("input[placeholder='Enter your username']")
            ).isDisplayed(),
            "Username field on " + browser
        );
        Assert.assertTrue(
            driver.findElement(
                By.cssSelector("input[type='password']")
            ).isDisplayed(),
            "Password field on " + browser
        );
        Assert.assertTrue(
            driver.findElement(
                By.cssSelector("button[type='submit']")
            ).isDisplayed(),
            "Submit button on " + browser
        );
    }

    @Test(description = "Verify products page loads on all browsers")
    public void verifyProductsPage() {
        driver.get(BASE_URL + "/#!/products");
        Assert.assertTrue(
            driver.findElements(By.cssSelector(".card")).size() > 0,
            "Products page should show cards on " + browser
        );
    }
}
```

**`testng.xml` for cross-browser factory:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">

<suite name="ShopEasy Cross-Browser Suite"
       parallel="instances"
       thread-count="3">

    <test name="Cross Browser Tests">
        <classes>
            <class name="com.shopeasy.tests.CrossBrowserLoginTest"/>
        </classes>
    </test>

</suite>
```

With `parallel="instances"` and `thread-count="3"`, all three browser instances run in parallel.

---

## Factories vs DataProvider — Decision Guide

```
Question: Do you need the SAME test on DIFFERENT CONFIGURATIONS (browsers)?
    → YES: Use @Factory
    → Each configuration creates a full instance of the class
    → All @Test methods run per instance

Question: Do you need ONE test method with MULTIPLE INPUT VALUES?
    → YES: Use @DataProvider
    → One instance of the class
    → One method runs multiple times with different data

Question: Do you need MULTIPLE USERS across ALL TESTS?
    → Use @Factory — each user is an instance

Question: Do you need to test LOGIN with multiple username/password pairs?
    → Use @DataProvider — multiple data rows for one test method
```

| Scenario | Best Choice |
|---|---|
| Run all tests on Chrome AND Firefox | `@Factory` |
| Test login with 10 credential combos | `@DataProvider` |
| Run full suite as customer AND admin | `@Factory` |
| Test search with 20 different keywords | `@DataProvider` |
| Run tests against dev AND staging URLs | `@Factory` or `@Parameters` |
| Verify cart with quantities 1, 2, 5, 10 | `@DataProvider` |
