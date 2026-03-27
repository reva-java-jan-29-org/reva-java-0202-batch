# 08 — `@Parameters` — Passing Values from `testng.xml`

## What is `@Parameters`?

`@Parameters` lets you inject values defined in `testng.xml` directly into test methods or configuration methods (`@BeforeMethod`, `@BeforeClass`, etc.). This separates test configuration from test code.

**Common uses:**
- Select which browser to use (Chrome / Firefox / Edge)
- Set the environment base URL (dev / staging / prod)
- Pass test credentials (username / password)
- Set timeout values

---

## How `@Parameters` Works

```
testng.xml:
  <parameter name="browser" value="chrome"/>

         ↓ TestNG injects the value

Test method:
  @Parameters("browser")
  public void setUp(String browser) {
      // browser = "chrome"
  }
```

---

## Basic `@Parameters` Example

**`testng.xml`:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">

<suite name="ShopEasy Suite">

    <parameter name="browser" value="chrome"/>
    <parameter name="baseUrl"  value="http://localhost:4200"/>

    <test name="Login Tests">
        <classes>
            <class name="com.shopeasy.tests.LoginTest"/>
        </classes>
    </test>

</suite>
```

**Test class:**

```java
package com.shopeasy.tests;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.time.Duration;

public class LoginTest {

    private WebDriver driver;

    @BeforeMethod
    @Parameters("browser")
    public void setUp(String browser) {
        System.out.println("Setting up browser: " + browser);
        // browser = "chrome" (from testng.xml)
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) driver.quit();
    }

    @Test
    @Parameters("baseUrl")
    public void verifyLoginPage(String baseUrl) {
        driver.get(baseUrl + "/#!/login");
        Assert.assertEquals(driver.getTitle(), "ShopEasy - E-Commerce");
    }
}
```

---

## `@Optional` — Default Values

If a parameter is not defined in `testng.xml`, TestNG throws an exception. Use `@Optional` to provide a default value:

```java
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

@BeforeMethod
@Parameters("browser")
public void setUp(@Optional("chrome") String browser) {
    // If "browser" is not in testng.xml, defaults to "chrome"
    System.out.println("Browser: " + browser);
}

@Test
@Parameters({"username", "password"})
public void verifyLogin(
    @Optional("testuser") String username,
    @Optional("password123") String password
) {
    // If params not in testng.xml, uses the defaults above
    driver.get("http://localhost:4200/#!/login");
    driver.findElement(
        By.cssSelector("input[placeholder='Enter your username']")
    ).sendKeys(username);
    driver.findElement(
        By.cssSelector("input[type='password']")
    ).sendKeys(password);
    driver.findElement(
        By.cssSelector("button[type='submit']")
    ).click();
    Assert.assertTrue(driver.getCurrentUrl().contains("products"));
}
```

---

## Parameterizing Browser Selection

A complete, real-world example: run the same tests on Chrome, Firefox, or Edge by changing a single value in `testng.xml`.

**`BaseTest.java`:**

```java
package com.shopeasy.tests.base;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import java.time.Duration;

public class BaseTest {

    protected WebDriver driver;
    protected static final String DEFAULT_BASE_URL = "http://localhost:4200";

    @BeforeMethod
    @Parameters({"browser", "baseUrl"})
    public void setUp(
        @Optional("chrome")                  String browser,
        @Optional("http://localhost:4200")   String baseUrl
    ) {
        System.out.println("Launching: " + browser + " → " + baseUrl);

        switch (browser.toLowerCase()) {

            case "firefox":
                FirefoxOptions ffOptions = new FirefoxOptions();
                driver = new FirefoxDriver(ffOptions);
                break;

            case "edge":
                EdgeOptions edgeOptions = new EdgeOptions();
                driver = new EdgeDriver(edgeOptions);
                break;

            case "chrome":
            default:
                ChromeOptions chromeOptions = new ChromeOptions();
                driver = new ChromeDriver(chromeOptions);
                break;
        }

        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    protected void navigateTo(String route) {
        driver.get(DEFAULT_BASE_URL + "/#!/" + route);
    }
}
```

**`testng-chrome.xml`:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">

<suite name="Chrome Suite">
    <parameter name="browser" value="chrome"/>
    <parameter name="baseUrl"  value="http://localhost:4200"/>

    <test name="Login - Chrome">
        <classes>
            <class name="com.shopeasy.tests.LoginTest"/>
        </classes>
    </test>
</suite>
```

**`testng-firefox.xml`:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">

<suite name="Firefox Suite">
    <parameter name="browser" value="firefox"/>
    <parameter name="baseUrl"  value="http://localhost:4200"/>

    <test name="Login - Firefox">
        <classes>
            <class name="com.shopeasy.tests.LoginTest"/>
        </classes>
    </test>
</suite>
```

```bash
# Run on Chrome
mvn test -DsuiteXmlFile=testng-chrome.xml

# Run on Firefox
mvn test -DsuiteXmlFile=testng-firefox.xml
```

---

## Parameterizing Environment (Dev / Staging / Prod)

```xml
<!-- testng-dev.xml -->
<suite name="Dev Environment">
    <parameter name="browser"     value="chrome"/>
    <parameter name="baseUrl"     value="http://localhost:4200"/>
    <parameter name="apiGateway"  value="http://localhost:8080/api"/>

    <test name="Dev Tests">
        <classes>
            <class name="com.shopeasy.tests.LoginTest"/>
        </classes>
    </test>
</suite>
```

```xml
<!-- testng-staging.xml -->
<suite name="Staging Environment">
    <parameter name="browser"     value="chrome"/>
    <parameter name="baseUrl"     value="https://staging.shopeasy.com"/>
    <parameter name="apiGateway"  value="https://staging.shopeasy.com/api"/>

    <test name="Staging Tests">
        <classes>
            <class name="com.shopeasy.tests.LoginTest"/>
        </classes>
    </test>
</suite>
```

**Using environment parameters in tests:**

```java
package com.shopeasy.tests;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.time.Duration;

public class LoginTest {

    private WebDriver driver;
    private String baseUrl;

    @BeforeMethod
    @Parameters({"browser", "baseUrl"})
    public void setUp(
        @Optional("chrome")               String browser,
        @Optional("http://localhost:4200") String baseUrl
    ) {
        this.baseUrl = baseUrl;  // Store for use in @Test methods
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (driver != null) driver.quit();
    }

    @Test(description = "Verify login page title")
    public void verifyLoginPageTitle() {
        driver.get(baseUrl + "/#!/login");
        Assert.assertEquals(driver.getTitle(), "ShopEasy - E-Commerce");
    }

    @Test(description = "Verify login with test credentials")
    public void verifyLogin() {
        driver.get(baseUrl + "/#!/login");

        driver.findElement(
            By.cssSelector("input[placeholder='Enter your username']")
        ).sendKeys("testuser");
        driver.findElement(
            By.cssSelector("input[type='password']")
        ).sendKeys("password123");
        driver.findElement(
            By.cssSelector("button[type='submit']")
        ).click();

        Assert.assertTrue(driver.getCurrentUrl().contains("products"),
            "Should redirect to products after login on: " + baseUrl);
    }
}
```

---

## Parameters at Suite Level vs Test Level

Parameters can be defined at the `<suite>` level (shared by all tests) or at the `<test>` level (specific to that block). Test-level parameters override suite-level ones:

```xml
<suite name="ShopEasy Suite">

    <!-- Suite-level: applies to ALL <test> blocks -->
    <parameter name="browser" value="chrome"/>
    <parameter name="timeout" value="10"/>

    <!-- Test 1: uses suite-level browser (chrome) -->
    <test name="Customer Tests">
        <parameter name="username" value="customer1"/>
        <parameter name="password" value="pass123"/>
        <classes>
            <class name="com.shopeasy.tests.CartTest"/>
        </classes>
    </test>

    <!-- Test 2: overrides browser to firefox for this block -->
    <test name="Admin Tests (Firefox)">
        <parameter name="browser"  value="firefox"/>   <!-- overrides suite-level -->
        <parameter name="username" value="admin"/>
        <parameter name="password" value="admin123"/>
        <classes>
            <class name="com.shopeasy.tests.AdminTest"/>
        </classes>
    </test>

</suite>
```

---

## `@Parameters` vs `@DataProvider` — Key Differences

| Aspect | `@Parameters` | `@DataProvider` |
|---|---|---|
| Source of data | `testng.xml` `<parameter>` tags | Java method returning `Object[][]` |
| How many runs | Test runs **once** with injected values | Test runs **once per row** |
| Change without code | Yes — edit testng.xml | No — requires code change |
| Multiple values | One value per parameter name | Multiple rows of data |
| Use case | Browser, environment, base URL, credentials | Login combos, search terms, quantities |
| Runtime override | Via `testng.xml` | Not directly (need code) |
| Parameterize setup | Yes (`@BeforeMethod`, `@BeforeClass`) | No — only for `@Test` |

**Rule of thumb:**
- Use `@Parameters` when you want to configure the test **run environment** (which browser, which URL)
- Use `@DataProvider` when you want to test the same scenario with **multiple data inputs**

---

## Combined: Parameters + DataProvider

You can use both in the same class:

```java
package com.shopeasy.tests;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.time.Duration;

public class CombinedTest {

    private WebDriver driver;
    private String baseUrl;

    // @Parameters configures the environment
    @BeforeMethod
    @Parameters({"browser", "baseUrl"})
    public void setUp(
        @Optional("chrome")                String browser,
        @Optional("http://localhost:4200") String baseUrl
    ) {
        this.baseUrl = baseUrl;
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (driver != null) driver.quit();
    }

    // @DataProvider provides test data (multiple login scenarios)
    @DataProvider(name = "loginData")
    public Object[][] loginScenarios() {
        return new Object[][] {
            { "testuser",  "password123",  true  },
            { "wronguser", "wrongpass",    false },
        };
    }

    // @Test uses DataProvider for data AND relies on @Parameters for environment
    @Test(dataProvider = "loginData",
          description = "Login test: multiple credentials on configured environment")
    public void verifyLogin(String username, String password, boolean expectSuccess) {
        driver.get(baseUrl + "/#!/login");

        driver.findElement(
            By.cssSelector("input[placeholder='Enter your username']")
        ).sendKeys(username);
        driver.findElement(
            By.cssSelector("input[type='password']")
        ).sendKeys(password);
        driver.findElement(
            By.cssSelector("button[type='submit']")
        ).click();

        boolean redirected = driver.getCurrentUrl().contains("products");
        Assert.assertEquals(redirected, expectSuccess,
            "Login result for '" + username + "' on " + baseUrl);
    }
}
```
