# TestNG Study Guide — ShopEasy E-Commerce Application

## What You Will Learn

This guide teaches TestNG — the most widely used Java testing framework for Selenium automation — using the **ShopEasy** AngularJS 1.x frontend as the application under test. Every concept is demonstrated with real test scenarios from the app, not toy examples.

---

## Application Reference

| Detail | Value |
|---|---|
| App Name | ShopEasy |
| Page Title | `ShopEasy - E-Commerce` |
| Frontend Tech | AngularJS 1.x (Hash routing: `#!/route`) |
| Frontend Port | `http://localhost:4200` (or as configured in docker-compose) |
| API Gateway | `http://localhost:8080/api` |
| CSS Framework | Bootstrap 5.3.0 |

### Pages in the Application

| URL | Description | Role |
|---|---|---|
| `http://localhost:4200/#!/products` | Product catalog with search & filter | Public |
| `http://localhost:4200/#!/login` | Username + Password login form | Public |
| `http://localhost:4200/#!/register` | New user registration form | Public |
| `http://localhost:4200/#!/cart` | Shopping cart + checkout + payment | Customer |
| `http://localhost:4200/#!/orders` | Customer order history + details modal | Customer |
| `http://localhost:4200/#!/payments` | Payment history table | Customer |
| `http://localhost:4200/#!/admin/dashboard` | Admin dashboard with stats | Admin |
| `http://localhost:4200/#!/admin/products` | Product CRUD table + modal | Admin |
| `http://localhost:4200/#!/admin/customers` | Customer management table | Admin |
| `http://localhost:4200/#!/admin/orders` | All orders view | Admin |
| `http://localhost:4200/#!/admin/admins` | Admin account management | Admin |

---

## Study Guide Navigation

| # | File | Topics Covered |
|---|---|---|
| 00 | [00-overview.md](00-overview.md) | What is TestNG, vs JUnit, Maven setup, project structure |
| 01 | [01-annotations.md](01-annotations.md) | All TestNG annotations, execution order, `@Test` attributes |
| 02 | [02-assertions.md](02-assertions.md) | Hard assertions, Soft assertions, all Assert methods |
| 03 | [03-testng-xml.md](03-testng-xml.md) | `testng.xml` suite file — suites, tests, groups, packages |
| 04 | [04-data-provider.md](04-data-provider.md) | `@DataProvider` — multi-data tests, external data, Iterator |
| 05 | [05-groups-and-priority.md](05-groups-and-priority.md) | Groups, priority, `dependsOnMethods`, `dependsOnGroups` |
| 06 | [06-listeners.md](06-listeners.md) | `ITestListener`, screenshot on failure, registering listeners |
| 07 | [07-parallel-execution.md](07-parallel-execution.md) | Parallel methods/tests/classes, `ThreadLocal<WebDriver>` |
| 08 | [08-parameters.md](08-parameters.md) | `@Parameters`, `@Optional`, browser/environment selection |
| 09 | [09-factories.md](09-factories.md) | `@Factory`, cross-browser testing, Factory vs DataProvider |
| 10 | [10-interview-questions.md](10-interview-questions.md) | 40+ interview questions with answers |

---

## What is TestNG?

**TestNG** (Test Next Generation) is a testing framework for Java, inspired by JUnit and NUnit but designed with additional features for enterprise-scale test automation. It was created by **Cédric Beust** and is pronounced "Testing".

**TestNG is not just for unit tests.** It excels at:
- Functional testing (what we do with Selenium)
- Integration testing
- End-to-end testing
- Data-driven testing

**Core capabilities:**
- Annotations to control test lifecycle
- Grouping tests by category (smoke, regression, login)
- Running the same test with multiple data sets (`@DataProvider`)
- Parallel execution across multiple threads
- Dependency between tests (`dependsOnMethods`)
- Rich configuration via XML (`testng.xml`)
- HTML and XML reports generated automatically
- Listeners for custom behavior (e.g., screenshot on failure)

---

## TestNG vs JUnit Comparison

| Feature | TestNG 7.x | JUnit 5 |
|---|---|---|
| Setup annotation | `@BeforeMethod` | `@BeforeEach` |
| Teardown annotation | `@AfterMethod` | `@AfterEach` |
| Class-level setup | `@BeforeClass` | `@BeforeAll` (static) |
| Suite-level setup | `@BeforeSuite` | No direct equivalent |
| Test annotation | `@Test` | `@Test` |
| Data-driven tests | `@DataProvider` | `@MethodSource`, `@CsvSource` |
| Grouping | `@Test(groups={...})` | `@Tag` |
| Dependency | `dependsOnMethods` | No built-in equivalent |
| Parallel execution | Built-in via testng.xml | Via JUnit Platform |
| XML configuration | `testng.xml` | No equivalent |
| Soft assertions | `SoftAssert` class | AssertJ or custom |
| Priority control | `@Test(priority=n)` | `@TestMethodOrder` |
| Skip test | `@Test(enabled=false)` | `@Disabled` |
| Expected exception | `@Test(expectedExceptions=...)` | `assertThrows(...)` |
| Timeout | `@Test(timeOut=5000)` | `@Timeout(5)` |
| Repeat test | `@Test(invocationCount=n)` | `@RepeatedTest(n)` |
| Industry adoption (Selenium) | **Very widely used** | Growing |

> **Industry note:** In Selenium test automation, TestNG is significantly more popular than JUnit because of `@DataProvider`, built-in parallel support, `testng.xml`, and rich listener support. Most job descriptions for QA engineers mention TestNG explicitly.

---

## How TestNG Integrates with Selenium

```
Your Test Class (extends BaseTest)
          │
          │ @BeforeMethod → setUp() creates WebDriver
          │ @Test         → test logic uses driver + TestNG assertions
          │ @AfterMethod  → tearDown() calls driver.quit()
          ▼
     TestNG Runner
          │ reads testng.xml (suite, groups, data providers)
          │ manages thread pool for parallel execution
          │ fires listener events (onTestFailure → screenshot)
          ▼
     Reports (target/surefire-reports/)
          │ testng-results.xml
          │ index.html (HTML report)
```

**The integration chain:**

```
testng.xml
    └── defines which test classes to run
           └── BaseTest.java (@BeforeMethod / @AfterMethod)
                  └── creates/destroys WebDriver per test
                         └── Test classes (@Test methods)
                                └── TestNG Assertions (Assert.assertEquals...)
                                       └── Listeners (screenshot on failure)
```

---

## Maven Setup — `pom.xml`

Create a new Maven project with the following configuration:

- **GroupId:** `com.shopeasy`
- **ArtifactId:** `shopeasy-testng-tests`
- **Java Version:** 17

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.shopeasy</groupId>
    <artifactId>shopeasy-testng-tests</artifactId>
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

        <!-- Selenium WebDriver — controls the browser -->
        <dependency>
            <groupId>org.seleniumhq.selenium</groupId>
            <artifactId>selenium-java</artifactId>
            <version>${selenium.version}</version>
        </dependency>

        <!-- TestNG — test framework (annotations, assertions, runner) -->
        <dependency>
            <groupId>org.testng</groupId>
            <artifactId>testng</artifactId>
            <version>${testng.version}</version>
            <scope>test</scope>
        </dependency>

    </dependencies>

    <build>
        <plugins>

            <!-- Maven Surefire Plugin — runs TestNG tests via mvn test -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
                <configuration>
                    <!-- Point to your testng.xml suite file -->
                    <suiteXmlFiles>
                        <suiteXmlFile>testng.xml</suiteXmlFile>
                    </suiteXmlFiles>
                </configuration>
            </plugin>

        </plugins>
    </build>

</project>
```

> **Selenium Manager:** Selenium 4.6+ includes Selenium Manager built-in. It automatically downloads the correct ChromeDriver matching your installed Chrome version. No manual driver setup needed.

---

## Project Structure

```
shopeasy-testng-tests/
├── pom.xml
├── testng.xml                          ← Suite configuration file
└── src/
    └── test/
        └── java/
            └── com/
                └── shopeasy/
                    └── tests/
                        ├── base/
                        │   └── BaseTest.java         ← WebDriver setup/teardown
                        ├── pages/                    ← Page Object Model classes
                        │   ├── LoginPage.java
                        │   ├── ProductsPage.java
                        │   ├── CartPage.java
                        │   └── AdminProductsPage.java
                        ├── listeners/                ← Custom TestNG listeners
                        │   └── ScreenshotListener.java
                        └── tests/                    ← Actual test classes
                            ├── LoginTest.java
                            ├── ProductTest.java
                            ├── CartTest.java
                            └── AdminTest.java
```

---

## Base Test Class

All test classes extend `BaseTest`, which handles browser setup and teardown using TestNG lifecycle annotations:

```java
package com.shopeasy.tests.base;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.time.Duration;

public class BaseTest {

    protected WebDriver driver;
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

---

## Running Tests

| Command | What It Does |
|---|---|
| `mvn test` | Runs all tests (uses `testng.xml` if configured in surefire) |
| `mvn test -Dtest=LoginTest` | Runs a specific test class |
| `mvn test -DsuiteXmlFile=testng.xml` | Runs the specified TestNG suite file |
| `mvn test -DsuiteXmlFile=smoke.xml` | Runs only smoke tests |
| `mvn test -Dgroups=smoke` | Runs tests in the "smoke" group |

---

## Test Credentials

| Role | Username | Password |
|---|---|---|
| Admin | Create via admin panel | — |
| Customer | Register via register page | — |

> Test card numbers: `4242 4242 4242 4242` (success), `4000 0000 0000 0002` (declined)
