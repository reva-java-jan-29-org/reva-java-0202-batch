# 10 — TestNG Interview Questions & Answers

## Category 1: TestNG Basics

**Q1. What is TestNG and why is it preferred over JUnit for Selenium automation?**

TestNG (Test Next Generation) is a Java testing framework inspired by JUnit and NUnit. It is preferred for Selenium automation because:
- `@DataProvider` makes data-driven tests easy and clean
- Built-in parallel execution via `testng.xml` (no extra libraries needed)
- `testng.xml` gives fine-grained control over what runs without code changes
- Grouping (`@Test(groups=...)`) lets you run smoke vs regression suites
- `dependsOnMethods` / `dependsOnGroups` for test ordering
- `SoftAssert` for collecting multiple failures in one test
- Richer lifecycle annotations (`@BeforeSuite`, `@BeforeTest`, `@BeforeGroups`)
- Listeners (`ITestListener`) for screenshots on failure

---

**Q2. What is the difference between TestNG and JUnit 5?**

| Feature | TestNG | JUnit 5 |
|---|---|---|
| Setup | `@BeforeMethod` | `@BeforeEach` |
| Teardown | `@AfterMethod` | `@AfterEach` |
| Class setup | `@BeforeClass` | `@BeforeAll` (must be static) |
| Suite setup | `@BeforeSuite` | No built-in equivalent |
| Groups | `@Test(groups={...})` | `@Tag` |
| Data provider | `@DataProvider` | `@MethodSource`, `@CsvSource` |
| Dependency | `dependsOnMethods` | No built-in equivalent |
| Parallel | Built-in via testng.xml | JUnit Platform |
| XML config | `testng.xml` | No equivalent |
| Soft assertions | `SoftAssert` class | Needs AssertJ or similar |
| Skip | `@Test(enabled=false)` | `@Disabled` |
| Industry use | Dominant for Selenium | Growing |

---

**Q3. List all TestNG annotations in the order they execute.**

```
@BeforeSuite
  @BeforeTest
    @BeforeGroups
      @BeforeClass
        @BeforeMethod → @Test → @AfterMethod  (repeats per test)
      @AfterClass
    @AfterGroups
  @AfterTest
@AfterSuite
```

---

**Q4. What is the difference between `@BeforeMethod` and `@BeforeClass`?**

- `@BeforeMethod` runs before **every** `@Test` method. If there are 5 tests, it runs 5 times. This creates a fresh state per test — safer but slower.
- `@BeforeClass` runs **once** before the first `@Test` in the class. All test methods share the setup. Faster, but tests are not isolated — one test's side effects can affect another.

In Selenium: use `@BeforeMethod` to open a fresh browser per test (safer). Use `@BeforeClass` when browser setup is expensive and tests are read-only (e.g., navigating to different pages on an already-logged-in session).

---

**Q5. What is the difference between `@BeforeSuite` and `@BeforeTest`?**

- `@BeforeSuite` runs **once** for the entire test run — before all classes, all `<test>` blocks.
- `@BeforeTest` runs once per `<test>` block in `testng.xml`. If you have 3 `<test>` blocks, `@BeforeTest` runs 3 times.

`@BeforeSuite` is used for one-time global setup (initialize reporting, verify app is running). `@BeforeTest` is used for per-block configuration (set different parameters for each test block).

---

## Category 2: Assertions

**Q6. What is the difference between hard assertions and soft assertions?**

- **Hard assertion (`Assert`):** Stops the test immediately when an assertion fails. Code after the failing assertion never executes. Use when the test cannot continue if one condition fails.
- **Soft assertion (`SoftAssert`):** Records the failure but continues executing. All failures are reported when `softAssert.assertAll()` is called at the end. Use when you want to check multiple independent conditions in one test.

```java
// Hard — stops on first failure
Assert.assertEquals(title, "ShopEasy - E-Commerce");
Assert.assertTrue(isVisible);  // never runs if above fails

// Soft — collects all failures
SoftAssert sa = new SoftAssert();
sa.assertEquals(title, "ShopEasy - E-Commerce");
sa.assertTrue(isVisible);
sa.assertAll();  // reports BOTH failures if both failed
```

---

**Q7. What happens if you forget to call `softAssert.assertAll()`?**

The test **passes silently** even if assertions failed. `SoftAssert` only reports failures when `assertAll()` is called. Forgetting it is a common bug — the test gives a false green result.

---

**Q8. What is the argument order in `Assert.assertEquals()`?**

`Assert.assertEquals(actual, expected)` — actual value first, expected value second.

Getting this wrong produces confusing failure messages:
```java
// CORRECT
Assert.assertEquals(driver.getTitle(), "ShopEasy - E-Commerce");
// Failure: expected [ShopEasy - E-Commerce] but found [wrong title]

// WRONG (reversed)
Assert.assertEquals("ShopEasy - E-Commerce", driver.getTitle());
// Failure: expected [wrong title] but found [ShopEasy - E-Commerce]  ← misleading
```

---

**Q9. How would you verify that a test fails with a specific exception?**

Use `@Test(expectedExceptions = ExceptionClass.class)`. The test passes only if that exception is thrown:

```java
@Test(expectedExceptions = NoSuchElementException.class)
public void verifyMissingElementThrows() {
    driver.findElement(By.id("does-not-exist"));  // must throw
}
```

If the exception is NOT thrown, the test fails. If a different exception is thrown, the test also fails.

---

**Q10. What is `Assert.fail()` used for?**

`Assert.fail(message)` unconditionally fails the test with a custom message. Use it when your failure condition requires custom logic that cannot be expressed with a simple assertion:

```java
if (driver.getCurrentUrl().contains("products")) {
    Assert.fail("Login with wrong credentials should NOT redirect to products page");
}
```

---

## Category 3: `testng.xml`

**Q11. What is `testng.xml` and why is it important?**

`testng.xml` is a configuration file that controls which tests run, with what parameters, in what order, and with how many threads. It is important because:
- Run smoke vs regression suites without code changes
- Pass browser/environment values without hardcoding
- Enable parallel execution
- Include/exclude groups
- Register listeners globally

Without `testng.xml`, you have to change Java code to control what runs.

---

**Q12. How do you run only the "smoke" group from `testng.xml`?**

```xml
<suite name="Smoke Suite">
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

Run with: `mvn test -DsuiteXmlFile=smoke.xml`

---

**Q13. How do you run specific methods from a class in `testng.xml`?**

```xml
<class name="com.shopeasy.tests.LoginTest">
    <methods>
        <include name="verifyLoginPageTitle"/>
        <include name="verifySuccessfulLogin"/>
    </methods>
</class>
```

---

**Q14. What is the `verbose` attribute in `testng.xml`?**

Controls the amount of console output. `verbose="0"` is silent, `verbose="1"` shows test names (default), `verbose="2"` shows pass/fail, `verbose="10"` is maximum debug output.

---

**Q15. What is the difference between `<packages>` and `<classes>` in `testng.xml`?**

- `<packages>` — scans all classes in the package automatically. New test classes are picked up without changing `testng.xml`.
- `<classes>` — explicitly lists each class. You must add each new class manually, but you have exact control.

For large projects, `<packages>` is easier to maintain. For selective test execution, `<classes>` is more precise.

---

## Category 4: `@DataProvider`

**Q16. What is `@DataProvider` and what does it return?**

`@DataProvider` marks a method that supplies test data. It returns `Object[][]` — a 2D array where each inner array is one row of data, and each element in the row is one argument to the test method.

TestNG calls the `@Test` method once per row. A data provider returning 5 rows = 5 separate test executions.

---

**Q17. How do you use a `@DataProvider` from a different class?**

Use `dataProviderClass` in the `@Test` annotation:

```java
@Test(dataProvider = "loginData", dataProviderClass = ShopEasyTestData.class)
public void verifyLogin(String username, String password) { ... }
```

The method in `ShopEasyTestData` must be annotated with `@DataProvider(name = "loginData")` and should be `static`.

---

**Q18. What is the difference between `Object[][]` and `Iterator<Object[]>` in a DataProvider?**

- `Object[][]` — All data is loaded into memory at once. Simple and common for small to medium datasets.
- `Iterator<Object[]>` — Data is provided lazily, one row at a time. Use for large datasets (thousands of rows) to avoid loading everything into memory at once. Useful when data comes from a database or large CSV.

---

**Q19. How do you run data provider tests in parallel?**

Add `parallel = true` to the `@DataProvider` annotation:

```java
@DataProvider(name = "searchTerms", parallel = true)
public Object[][] data() { ... }
```

Also set `data-provider-thread-count` in `testng.xml`. Requires `ThreadLocal<WebDriver>` for thread safety.

---

**Q20. What happens if the argument count in the `@Test` method doesn't match the `Object[]` row length?**

TestNG throws a `TestNGException` at runtime: `Wrong number of arguments`. The method signature must exactly match the row structure of the data provider.

---

## Category 5: Groups and Priority

**Q21. What are TestNG groups and how do you define them?**

Groups tag test methods with category names. Define them in `@Test(groups={"smoke", "login"})`. Run specific groups via `testng.xml`'s `<include>` / `<exclude>` tags without changing test code.

---

**Q22. Can a test method belong to multiple groups?**

Yes. `@Test(groups = {"smoke", "regression", "login"})` — the test is included when ANY of these groups is included in the run.

---

**Q23. What does `@Test(priority = n)` do? What is the default priority?**

`priority` controls the execution order of test methods within a class. Lower values run first. Default priority is `0`. Negative values are allowed (e.g., `priority = -1` runs before `priority = 0`). Two methods with the same priority run in an undefined order.

---

**Q24. What is `dependsOnMethods` and what happens when a dependency fails?**

`dependsOnMethods = "methodName"` tells TestNG not to run the test unless the named method passed. If the dependency FAILS, the dependent test is **SKIPPED** (not failed). If the dependency PASSES, the dependent test runs normally.

```java
@Test
public void loginStep() { ... }

@Test(dependsOnMethods = "loginStep")
public void addToCartStep() {
    // SKIPPED if loginStep failed — not marked as FAILED
}
```

---

**Q25. What is `alwaysRun = true` in TestNG?**

When `alwaysRun = true` is set on a `@Test` method, it runs even if its `dependsOnMethods` dependencies failed or were skipped. Commonly used on `@AfterMethod` to ensure cleanup always runs:

```java
@AfterMethod(alwaysRun = true)
public void tearDown() {
    // Runs even if @Test or @BeforeMethod threw an exception
    if (driver != null) driver.quit();
}
```

---

## Category 6: Listeners

**Q26. What is `ITestListener` and what are its key methods?**

`ITestListener` is an interface that lets you hook into TestNG's test lifecycle. Key methods:
- `onTestStart(ITestResult)` — called when a test is about to start
- `onTestSuccess(ITestResult)` — called when a test passes
- `onTestFailure(ITestResult)` — called when a test fails
- `onTestSkipped(ITestResult)` — called when a test is skipped
- `onFinish(ITestContext)` — called when the test block ends

---

**Q27. How do you take a screenshot on test failure using a listener?**

```java
@Override
public void onTestFailure(ITestResult result) {
    WebDriver driver = ((BaseTest) result.getInstance()).driver;
    File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
    Files.copy(screenshot.toPath(), Paths.get("target/screenshots/" +
        result.getName() + ".png"), StandardCopyOption.REPLACE_EXISTING);
}
```

Register the listener via `@Listeners(ScreenshotListener.class)` on `BaseTest` or via `testng.xml`'s `<listeners>` block.

---

**Q28. What is `TestListenerAdapter` and why use it instead of implementing `ITestListener`?**

`TestListenerAdapter` is an abstract class that implements `ITestListener` with empty method bodies. Extending it means you only override the methods you need — no empty boilerplate. Cleaner and more maintainable than implementing the full interface.

---

**Q29. What are the two ways to register a TestNG listener?**

1. **`@Listeners` annotation** — applied to a test class (or `BaseTest` for all subclasses):
   ```java
   @Listeners(ScreenshotListener.class)
   public class BaseTest { }
   ```

2. **`testng.xml`** — registers globally for all tests in the suite:
   ```xml
   <listeners>
       <listener class-name="com.shopeasy.tests.listeners.ScreenshotListener"/>
   </listeners>
   ```

`testng.xml` registration is preferred for global listeners like screenshot-on-failure since it applies everywhere without touching code.

---

**Q30. How do you get the test method name inside a listener?**

```java
result.getName()                          // method name only
result.getTestClass().getName()           // fully qualified class name
result.getThrowable().getMessage()        // failure message
result.getParameters()                    // DataProvider parameters
result.getStartMillis()                   // start time in ms
```

---

## Category 7: Parallel Execution

**Q31. What are the four parallel execution modes in TestNG?**

| Mode | Meaning |
|---|---|
| `parallel="methods"` | Each `@Test` method runs in its own thread |
| `parallel="tests"` | Each `<test>` block in testng.xml runs in its own thread |
| `parallel="classes"` | Each test class runs in its own thread |
| `parallel="instances"` | Each test class instance runs in its own thread |

---

**Q32. Why is a shared `WebDriver` field unsafe in parallel tests? How do you fix it?**

In parallel execution, multiple threads call `@BeforeMethod` simultaneously and write to the same `driver` field. Thread 2 overwrites Thread 1's driver, causing `NullPointerException` or the wrong browser being used.

**Fix:** Use `ThreadLocal<WebDriver>`:

```java
private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

@BeforeMethod
public void setUp() {
    DRIVER.set(new ChromeDriver());
}

protected WebDriver getDriver() {
    return DRIVER.get();
}

@AfterMethod
public void tearDown() {
    DRIVER.get().quit();
    DRIVER.remove();  // Prevent memory leaks
}
```

---

**Q33. Why must you call `DRIVER.remove()` in `@AfterMethod` with ThreadLocal?**

Without `remove()`, the `ThreadLocal` value persists in the thread's storage even after the test finishes. In thread pools (like Surefire's), threads are reused. Old driver references can leak into future tests, causing stale driver access or memory leaks. Always call `remove()` in `@AfterMethod`.

---

**Q34. How do you run `@DataProvider` tests in parallel?**

Set `parallel = true` on the `@DataProvider`:
```java
@DataProvider(name = "searchTerms", parallel = true)
public Object[][] data() { ... }
```

Control thread count in testng.xml:
```xml
<suite data-provider-thread-count="5" ...>
```

Requires `ThreadLocal<WebDriver>` in `BaseTest`.

---

**Q35. What is a race condition in test automation?**

A race condition occurs when two threads access and modify shared state simultaneously, producing unpredictable results. In Selenium:
- Shared `WebDriver` field → both threads write to it → one thread loses its driver
- Shared screenshot file name → both threads write the same file → one overwrites the other

Fix: Use `ThreadLocal`, `synchronized` blocks, `AtomicInteger`, or design tests to have no shared mutable state.

---

## Category 8: `@Parameters`

**Q36. What is `@Parameters` in TestNG?**

`@Parameters` injects values from `testng.xml`'s `<parameter>` tags into test methods or configuration methods. The string in `@Parameters("browser")` must match the `name` in `<parameter name="browser" value="chrome"/>`.

---

**Q37. What is `@Optional` and when do you use it?**

`@Optional("defaultValue")` provides a fallback value when the parameter is not defined in `testng.xml`. Without `@Optional`, TestNG throws an exception if the parameter is missing.

```java
@Parameters("browser")
public void setUp(@Optional("chrome") String browser) { ... }
```

---

**Q38. What is the difference between `@Parameters` and `@DataProvider`?**

| | `@Parameters` | `@DataProvider` |
|---|---|---|
| Source | `testng.xml` `<parameter>` | Java `Object[][]` method |
| Test runs | Once per parameter set | Once per data row |
| Use case | Environment config (browser, URL) | Test data (credentials, search terms) |
| Runtime change | Edit XML only | Requires code change |

---

## Category 9: `@Factory`

**Q39. What is `@Factory` in TestNG?**

`@Factory` marks a method that returns `Object[]` — an array of test class instances. TestNG runs all `@Test` methods on each instance. Used for cross-browser testing (one instance per browser) or multi-user testing (one instance per user role).

---

**Q40. What is the difference between `@Factory` and `@DataProvider`?**

- `@DataProvider` provides multiple data rows to ONE test method — it runs multiple times.
- `@Factory` creates multiple INSTANCES of the test class — ALL test methods run for each instance.

Use `@Factory` when the entire test class needs to run in different configurations. Use `@DataProvider` when one test method needs multiple data inputs.

---

**Q41. How do you run `@Factory` tests in parallel?**

Use `parallel="instances"` in `testng.xml`:

```xml
<suite name="Cross Browser" parallel="instances" thread-count="3">
    <test name="Tests">
        <classes>
            <class name="com.shopeasy.tests.CrossBrowserLoginTest"/>
        </classes>
    </test>
</suite>
```

Each factory instance runs in its own thread.

---

## Category 10: Miscellaneous

**Q42. How do you disable a test in TestNG without deleting it?**

Use `@Test(enabled = false)`. The test appears as SKIPPED in reports.

---

**Q43. How do you limit test execution time in TestNG?**

Use `@Test(timeOut = 5000)` — the test fails if it takes more than 5000 milliseconds. Useful for detecting hung browser tests.

---

**Q44. How do you run the same test multiple times in TestNG?**

Use `@Test(invocationCount = 3)` — the test runs 3 times. Combine with `threadPoolSize` for parallel repeated runs:

```java
@Test(invocationCount = 5, threadPoolSize = 3)
public void stressTest() { ... }
```

---

**Q45. Where does TestNG generate reports?**

After running `mvn test`, TestNG reports are at:
```
target/surefire-reports/
├── index.html          ← Open in browser for visual report
├── testng-results.xml  ← Machine-readable XML
└── TEST-*.xml          ← JUnit-compatible XML for CI tools
```

---

**Q46. What is the `configfailurepolicy` attribute in `<suite>`?**

Controls what happens when a configuration method (`@BeforeMethod`, `@BeforeClass`) fails.
- `skip` (default) — All tests that depend on the failed configuration are skipped
- `continue` — Tests continue to run even after configuration failure

```xml
<suite name="Suite" configfailurepolicy="continue">
```

---

**Q47. How do you run TestNG from the command line with Maven?**

```bash
mvn test                              # runs testng.xml (default)
mvn test -DsuiteXmlFile=smoke.xml     # runs specific suite file
mvn test -Dtest=LoginTest             # runs specific test class
mvn test -Dgroups=smoke               # runs specific group
```
