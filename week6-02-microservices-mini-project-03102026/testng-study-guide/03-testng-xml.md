# 03 — The `testng.xml` Suite File

## What is `testng.xml`?

`testng.xml` is an XML configuration file that tells TestNG:
- Which test classes to run
- In what order to run them
- How many threads to use
- Which groups to include or exclude
- What parameters to pass to tests
- How verbose the console output should be

Without `testng.xml`, TestNG discovers tests by scanning packages. With `testng.xml`, you have fine-grained control over exactly what runs.

> Place `testng.xml` in the **root of the project** (same level as `pom.xml`).

---

## The Structure of `testng.xml`

```
<suite>                    ← The entire test run (one per file)
  <test>                   ← A logical test block (can have multiple)
    <groups>               ← Include/exclude test groups
    <classes>              ← Specific classes to run
      <class>              ← One test class
        <methods>          ← Specific methods to include/exclude
    <packages>             ← Run all classes in a package
  <listeners>              ← Register custom listeners
  <parameter>              ← Global parameters available to all tests
```

---

## Minimal `testng.xml`

The simplest possible suite file — runs all tests in listed classes:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">

<suite name="ShopEasy Test Suite">

    <test name="All Tests">
        <classes>
            <class name="com.shopeasy.tests.LoginTest"/>
            <class name="com.shopeasy.tests.ProductTest"/>
            <class name="com.shopeasy.tests.CartTest"/>
        </classes>
    </test>

</suite>
```

---

## `<suite>` Attributes

| Attribute | Default | Description |
|---|---|---|
| `name` | required | Name of the suite — appears in reports |
| `verbose` | `1` | Console output level (0–10) |
| `parallel` | `none` | Parallel execution mode |
| `thread-count` | `5` | Number of threads for parallel execution |
| `configfailurepolicy` | `skip` | What to do when `@BeforeMethod` fails: `skip` or `continue` |
| `time-out` | none | Global timeout for all test methods (milliseconds) |

```xml
<suite name="ShopEasy Regression Suite"
       verbose="2"
       parallel="methods"
       thread-count="4"
       time-out="30000">
```

---

## `verbose` Levels

| Level | Output |
|---|---|
| `0` | Silent — no output |
| `1` | Test names only (default) |
| `2` | Test names + pass/fail status |
| `5` | Full detail including configuration methods |
| `10` | Maximum debug output |

```xml
<suite name="ShopEasy Suite" verbose="2">
```

---

## `<test>` Element

A `<test>` element groups related classes. You can have multiple `<test>` blocks in one suite:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">

<suite name="ShopEasy Full Suite" verbose="2">

    <!-- Group 1: Public pages tests -->
    <test name="Public Pages">
        <classes>
            <class name="com.shopeasy.tests.LoginTest"/>
            <class name="com.shopeasy.tests.RegisterTest"/>
            <class name="com.shopeasy.tests.ProductsTest"/>
        </classes>
    </test>

    <!-- Group 2: Customer workflow tests -->
    <test name="Customer Workflows">
        <classes>
            <class name="com.shopeasy.tests.CartTest"/>
            <class name="com.shopeasy.tests.OrderTest"/>
            <class name="com.shopeasy.tests.PaymentTest"/>
        </classes>
    </test>

    <!-- Group 3: Admin panel tests -->
    <test name="Admin Panel">
        <classes>
            <class name="com.shopeasy.tests.AdminDashboardTest"/>
            <class name="com.shopeasy.tests.AdminProductsTest"/>
            <class name="com.shopeasy.tests.AdminCustomersTest"/>
        </classes>
    </test>

</suite>
```

**Each `<test>` block:**
- Triggers `@BeforeTest` once before its classes
- Triggers `@AfterTest` once after its classes
- Runs independently from other `<test>` blocks

---

## `<classes>` and `<class>`

List specific test classes to include:

```xml
<test name="Login Tests">
    <classes>
        <class name="com.shopeasy.tests.LoginTest"/>
        <class name="com.shopeasy.tests.RegisterTest"/>
    </classes>
</test>
```

---

## `<methods>` — Include or Exclude Specific Methods

Within a class, you can select only specific test methods:

```xml
<test name="Selected Login Tests">
    <classes>
        <class name="com.shopeasy.tests.LoginTest">
            <methods>
                <!-- Only run these two methods from LoginTest -->
                <include name="verifyLoginPageTitle"/>
                <include name="verifyLoginWithValidCredentials"/>
                <!-- All other methods in LoginTest are skipped -->
            </methods>
        </class>
    </classes>
</test>
```

Excluding specific methods:

```xml
<test name="Login Tests Without Admin">
    <classes>
        <class name="com.shopeasy.tests.LoginTest">
            <methods>
                <!-- Run ALL methods EXCEPT this one -->
                <exclude name="verifyAdminLoginRedirect"/>
            </methods>
        </class>
    </classes>
</test>
```

---

## `<packages>` — Run All Classes in a Package

Instead of listing each class individually, point to a package and TestNG finds all test classes automatically:

```xml
<test name="All Tests">
    <packages>
        <package name="com.shopeasy.tests"/>
        <!-- This will find and run LoginTest, ProductTest, CartTest, etc. -->
    </packages>
</test>
```

You can also include sub-packages:

```xml
<test name="All Tests Including Admin">
    <packages>
        <package name="com.shopeasy.tests"/>
        <package name="com.shopeasy.tests.admin"/>
    </packages>
</test>
```

---

## `<groups>` — Include and Exclude Groups

Groups let you run a subset of tests without changing your test code. The `@Test(groups={...})` annotation marks which group a method belongs to.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">

<suite name="ShopEasy Smoke Suite">

    <test name="Smoke Tests Only">
        <groups>
            <run>
                <!-- Only run tests in the "smoke" group -->
                <include name="smoke"/>
            </run>
        </groups>
        <classes>
            <class name="com.shopeasy.tests.LoginTest"/>
            <class name="com.shopeasy.tests.ProductTest"/>
            <class name="com.shopeasy.tests.CartTest"/>
        </classes>
    </test>

</suite>
```

Excluding groups:

```xml
<test name="Regression Without Admin">
    <groups>
        <run>
            <include name="regression"/>
            <!-- Exclude slow or destructive tests -->
            <exclude name="admin"/>
        </run>
    </groups>
    <packages>
        <package name="com.shopeasy.tests"/>
    </packages>
</test>
```

Combining multiple groups (include tests belonging to ANY of these groups):

```xml
<test name="Smoke and Login Tests">
    <groups>
        <run>
            <include name="smoke"/>
            <include name="login"/>
        </run>
    </groups>
    <packages>
        <package name="com.shopeasy.tests"/>
    </packages>
</test>
```

---

## `<parameter>` — Passing Values to Tests

Define parameters at the suite or test level, then read them in test classes with `@Parameters`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">

<suite name="ShopEasy Suite">

    <!-- Suite-level parameters: available to ALL tests -->
    <parameter name="browser" value="chrome"/>
    <parameter name="baseUrl" value="http://localhost:4200"/>
    <parameter name="timeout" value="10"/>

    <test name="Login Tests">
        <!-- Test-level parameters: override suite-level for this test block -->
        <parameter name="username" value="testuser"/>
        <parameter name="password" value="password123"/>
        <classes>
            <class name="com.shopeasy.tests.LoginTest"/>
        </classes>
    </test>

</suite>
```

Reading parameters in the test class:

```java
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {

    @Test
    @Parameters({"username", "password"})
    public void verifyLoginWithCredentials(String username, String password) {
        navigateTo("login");
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
}
```

> See `08-parameters.md` for a full guide on `@Parameters`.

---

## `<listeners>` — Registering Custom Listeners

Register listeners globally in `testng.xml` so they apply to all tests:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">

<suite name="ShopEasy Suite">

    <listeners>
        <!-- Custom listener that takes a screenshot on failure -->
        <listener class-name="com.shopeasy.tests.listeners.ScreenshotListener"/>
        <!-- Custom listener that logs test results -->
        <listener class-name="com.shopeasy.tests.listeners.TestLogger"/>
    </listeners>

    <test name="All Tests">
        <packages>
            <package name="com.shopeasy.tests"/>
        </packages>
    </test>

</suite>
```

> See `06-listeners.md` for a full guide on custom listeners.

---

## Multiple Suite Files

For large projects, use multiple XML files for different contexts:

**`testng.xml`** — Full regression suite:
```xml
<suite name="ShopEasy Full Regression">
    <test name="All Tests">
        <packages>
            <package name="com.shopeasy.tests"/>
        </packages>
    </test>
</suite>
```

**`smoke.xml`** — Fast smoke suite:
```xml
<suite name="ShopEasy Smoke Suite" verbose="1">
    <test name="Smoke">
        <groups>
            <run>
                <include name="smoke"/>
            </run>
        </groups>
        <packages>
            <package name="com.shopeasy.tests"/>
        </packages>
    </test>
</suite>
```

**`parallel.xml`** — Parallel execution suite:
```xml
<suite name="ShopEasy Parallel Suite" parallel="methods" thread-count="4">
    <test name="Parallel Login Tests">
        <classes>
            <class name="com.shopeasy.tests.LoginTest"/>
        </classes>
    </test>
</suite>
```

---

## Running a Specific `testng.xml` File

```bash
# Run the default testng.xml (configured in pom.xml)
mvn test

# Run a specific suite file
mvn test -DsuiteXmlFile=testng.xml
mvn test -DsuiteXmlFile=smoke.xml
mvn test -DsuiteXmlFile=parallel.xml

# Run from IntelliJ: Right-click testng.xml → Run
```

---

## Configuring Maven Surefire Plugin for `testng.xml`

In `pom.xml`, configure the Surefire plugin to always use your suite file:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.5</version>
            <configuration>
                <suiteXmlFiles>
                    <suiteXmlFile>testng.xml</suiteXmlFile>
                </suiteXmlFiles>
            </configuration>
        </plugin>
    </plugins>
</build>
```

To support overriding from command line:

```xml
<configuration>
    <suiteXmlFiles>
        <!-- Default to testng.xml, override with -DsuiteXmlFile=smoke.xml -->
        <suiteXmlFile>${suiteXmlFile}</suiteXmlFile>
    </suiteXmlFiles>
</configuration>
```

```bash
# Override suite file at runtime
mvn test -DsuiteXmlFile=smoke.xml
```

---

## Complete `testng.xml` — ShopEasy Full Example

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">

<suite name="ShopEasy E-Commerce Test Suite" verbose="2">

    <!-- Global parameters available to all tests -->
    <parameter name="browser"  value="chrome"/>
    <parameter name="baseUrl"  value="http://localhost:4200"/>
    <parameter name="timeout"  value="10"/>

    <!-- Custom listeners (screenshot on failure, custom reporting) -->
    <listeners>
        <listener class-name="com.shopeasy.tests.listeners.ScreenshotListener"/>
    </listeners>

    <!-- ── Test Block 1: Public Pages ───────────────────────────── -->
    <test name="Public Pages">
        <classes>
            <class name="com.shopeasy.tests.LoginTest"/>
            <class name="com.shopeasy.tests.RegisterTest"/>
        </classes>
    </test>

    <!-- ── Test Block 2: Product Catalog ───────────────────────── -->
    <test name="Product Catalog">
        <classes>
            <class name="com.shopeasy.tests.ProductTest">
                <methods>
                    <include name="verifyProductsPageLoads"/>
                    <include name="verifyProductSearch"/>
                    <include name="verifyCategoryFilter"/>
                    <!-- Excludes any other methods in ProductTest -->
                </methods>
            </class>
        </classes>
    </test>

    <!-- ── Test Block 3: Customer Workflows ────────────────────── -->
    <test name="Customer Workflows">
        <parameter name="testUsername" value="customer1"/>
        <parameter name="testPassword" value="pass123"/>
        <classes>
            <class name="com.shopeasy.tests.CartTest"/>
            <class name="com.shopeasy.tests.OrderTest"/>
        </classes>
    </test>

    <!-- ── Test Block 4: Admin Panel ───────────────────────────── -->
    <test name="Admin Panel">
        <parameter name="testUsername" value="admin"/>
        <parameter name="testPassword" value="admin123"/>
        <groups>
            <run>
                <include name="admin"/>
                <exclude name="destructive"/>
            </run>
        </groups>
        <packages>
            <package name="com.shopeasy.tests.admin"/>
        </packages>
    </test>

</suite>
```

---

## `testng.xml` for Smoke Tests Only

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">

<suite name="ShopEasy Smoke Tests" verbose="1">

    <test name="Smoke">
        <groups>
            <run>
                <include name="smoke"/>
            </run>
        </groups>
        <packages>
            <package name="com.shopeasy.tests"/>
        </packages>
    </test>

</suite>
```

And the test classes with groups:

```java
public class LoginTest extends BaseTest {

    @Test(groups = {"smoke", "login"},
          description = "Verify login page loads — smoke check")
    public void verifyLoginPageLoads() {
        navigateTo("login");
        Assert.assertEquals(driver.getTitle(), "ShopEasy - E-Commerce");
    }

    @Test(groups = {"regression", "login"},
          description = "Verify login with valid credentials")
    public void verifyLoginWithValidCredentials() {
        // ... detailed login test
    }
}
```

---

## Reports

After running tests, TestNG generates reports automatically:

```
target/
└── surefire-reports/
    ├── index.html          ← HTML report — open in browser
    ├── testng-results.xml  ← XML for CI tools (Jenkins, GitHub Actions)
    └── TEST-TestSuite.xml  ← JUnit-compatible XML
```

Open `target/surefire-reports/index.html` in a browser to see the full test report with pass/fail counts, test duration, and stack traces.
