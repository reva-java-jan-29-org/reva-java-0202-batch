# 04 — `@DataProvider` — Data-Driven Testing

## What is `@DataProvider`?

`@DataProvider` is a TestNG mechanism that supplies multiple sets of input data to a single `@Test` method. Instead of writing ten separate test methods with different values, you write one test method and provide ten rows of data.

**Why use it?**
- One test method, many data sets
- Test logic is not duplicated
- Adding new test cases is just adding a new row of data
- Reports show each data set as a separate test execution

---

## How `@DataProvider` Works

```
@DataProvider method returns Object[][]
      │
      │   { {"user1", "pass1"}, {"user2", "pass2"}, {"admin", "admin123"} }
      │
      ▼
@Test method runs ONCE PER ROW
      │
      ├── run 1: username="user1",  password="pass1"
      ├── run 2: username="user2",  password="pass2"
      └── run 3: username="admin",  password="admin123"
```

Each row in `Object[][]` becomes one test invocation. A 3-row data provider = 3 separate test executions = 3 rows in the report.

---

## Basic `@DataProvider` Example

```java
package com.shopeasy.tests;

import com.shopeasy.tests.base.BaseTest;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class LoginDataProviderTest extends BaseTest {

    // ── Step 1: Define the data provider ─────────────────────────────
    @DataProvider(name = "loginCredentials")
    public Object[][] loginData() {
        return new Object[][] {
            // { username,     password,    expectedOutcome }
            { "testuser",   "password123",  true  },   // valid   → should succeed
            { "admin",      "admin123",     true  },   // valid   → should succeed
            { "wronguser",  "wrongpass",    false },   // invalid → should fail
            { "",           "password123",  false },   // empty   → should fail
            { "testuser",   "",             false },   // empty   → should fail
        };
    }

    // ── Step 2: Reference the data provider in @Test ──────────────────
    @Test(dataProvider = "loginCredentials",
          description = "Test login with multiple credential combinations")
    public void verifyLogin(String username, String password, boolean shouldSucceed) {
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

        boolean didRedirect = driver.getCurrentUrl().contains("products");

        Assert.assertEquals(didRedirect, shouldSucceed,
            "Login result mismatch for user: '" + username + "'");
    }
}
```

**What TestNG runs:**
```
Test 1: verifyLogin("testuser",  "password123", true)  → PASS
Test 2: verifyLogin("admin",     "admin123",    true)  → PASS
Test 3: verifyLogin("wronguser", "wrongpass",   false) → PASS
Test 4: verifyLogin("",          "password123", false) → PASS
Test 5: verifyLogin("testuser",  "",            false) → PASS
```

Five separate test executions from one method.

---

## Naming the Data Provider

The `name` attribute in `@DataProvider` must match the `dataProvider` attribute in `@Test`. If you omit the name, TestNG uses the method name.

```java
// DataProvider named "searchKeywords"
@DataProvider(name = "searchKeywords")
public Object[][] productSearchData() {
    return new Object[][] {
        { "laptop",   true  },   // should find results
        { "phone",    true  },   // should find results
        { "xyz123",   false },   // no results expected
        { "shirt",    true  },   // should find results
        { "",         true  },   // empty search shows all products
    };
}

// Test references the same name
@Test(dataProvider = "searchKeywords",
      description = "Verify product search returns correct results")
public void verifyProductSearch(String keyword, boolean expectResults) {
    navigateTo("products");

    // Clear and type the search keyword
    org.openqa.selenium.WebElement searchInput = driver.findElement(
        By.cssSelector("input[placeholder]")
    );
    searchInput.clear();
    searchInput.sendKeys(keyword);

    // Wait a moment for AngularJS to filter
    try { Thread.sleep(500); } catch (InterruptedException e) { }

    int resultCount = driver.findElements(By.cssSelector(".card")).size();

    if (expectResults) {
        Assert.assertTrue(resultCount > 0,
            "Search for '" + keyword + "' should return results");
    } else {
        Assert.assertEquals(resultCount, 0,
            "Search for '" + keyword + "' should return no results");
    }
}
```

---

## Data Provider for Checkout Quantities

```java
@DataProvider(name = "checkoutQuantities")
public Object[][] checkoutQuantityData() {
    return new Object[][] {
        // { quantity, expectedTotal scenario }
        { 1,  "minimum order quantity" },
        { 3,  "standard order"         },
        { 5,  "bulk order"             },
        { 10, "large order"            },
    };
}

@Test(dataProvider = "checkoutQuantities",
      description = "Verify checkout works with different product quantities")
public void verifyCheckoutWithQuantity(int quantity, String scenario) {
    // Navigate to products and add item to cart
    navigateTo("products");
    driver.findElement(By.cssSelector(".btn-primary")).click();

    // Navigate to cart
    navigateTo("cart");

    // Find the quantity input and set it
    org.openqa.selenium.WebElement quantityInput = driver.findElement(
        By.cssSelector("input[type='number']")
    );
    quantityInput.clear();
    quantityInput.sendKeys(String.valueOf(quantity));

    // Verify cart updated (quantity field reflects input)
    String actualQuantity = quantityInput.getAttribute("value");
    Assert.assertEquals(actualQuantity, String.valueOf(quantity),
        "Quantity field should show " + quantity + " for scenario: " + scenario);
}
```

---

## Data Provider with Registration Scenarios

```java
@DataProvider(name = "registrationScenarios")
public Object[][] registrationData() {
    return new Object[][] {
        // { username,    email,                 phone,       password,    valid? }
        { "user001", "user001@test.com",     "9876543210", "Pass@123",  true  },
        { "user002", "user002@test.com",     "9876543211", "Pass@123",  true  },
        { "",        "invalid@test.com",     "9876543212", "Pass@123",  false }, // no username
        { "user003", "not-an-email",         "9876543213", "Pass@123",  false }, // bad email
        { "user004", "user004@test.com",     "9876543214", "123",       false }, // weak password
    };
}

@Test(dataProvider = "registrationScenarios",
      description = "Verify registration form handles various input combinations")
public void verifyRegistration(String username, String email, String phone,
                               String password, boolean shouldSucceed) {
    navigateTo("register");

    driver.findElement(
        By.cssSelector("input[placeholder='Choose a username']")
    ).sendKeys(username);

    driver.findElement(
        By.cssSelector("input[placeholder='Enter your email']")
    ).sendKeys(email);

    driver.findElement(
        By.cssSelector("input[placeholder='Enter your phone number']")
    ).sendKeys(phone);

    driver.findElement(
        By.cssSelector("input[type='password']")
    ).sendKeys(password);

    driver.findElement(
        By.cssSelector("button[type='submit']")
    ).click();

    boolean redirected = driver.getCurrentUrl().contains("login")
                      || driver.getCurrentUrl().contains("products");

    Assert.assertEquals(redirected, shouldSucceed,
        "Registration result mismatch for username: " + username);
}
```

---

## Data Provider from a Separate Class

For large projects, keep data providers in a dedicated class. Use the `dataProviderClass` attribute in `@Test`:

**`src/test/java/com/shopeasy/tests/data/ShopEasyTestData.java`**

```java
package com.shopeasy.tests.data;

import org.testng.annotations.DataProvider;

/**
 * Centralized test data for ShopEasy tests.
 * All @DataProvider methods live here — keeps test classes clean.
 */
public class ShopEasyTestData {

    @DataProvider(name = "validLogins")
    public static Object[][] validLoginData() {
        return new Object[][] {
            { "testuser",  "password123" },
            { "customer2", "pass456"     },
        };
    }

    @DataProvider(name = "invalidLogins")
    public static Object[][] invalidLoginData() {
        return new Object[][] {
            { "wronguser",  "wrongpass"   },
            { "testuser",   "wrongpass"   },
            { "",           "password123" },
            { "testuser",   ""            },
        };
    }

    @DataProvider(name = "searchTerms")
    public static Object[][] searchTermData() {
        return new Object[][] {
            { "laptop",   true  },
            { "phone",    true  },
            { "zzz999",   false },
        };
    }

    @DataProvider(name = "productCategories")
    public static Object[][] categoryData() {
        return new Object[][] {
            { "Electronics" },
            { "Clothing"    },
            { "Books"       },
        };
    }
}
```

**Using `dataProviderClass` in the test:**

```java
package com.shopeasy.tests;

import com.shopeasy.tests.base.BaseTest;
import com.shopeasy.tests.data.ShopEasyTestData;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {

    // dataProviderClass tells TestNG which class to look in
    @Test(dataProvider = "validLogins",
          dataProviderClass = ShopEasyTestData.class,
          description = "Verify successful login with valid credentials")
    public void verifySuccessfulLogin(String username, String password) {
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

        Assert.assertTrue(
            driver.getCurrentUrl().contains("products"),
            "Should redirect to products after valid login"
        );
    }

    @Test(dataProvider = "invalidLogins",
          dataProviderClass = ShopEasyTestData.class,
          description = "Verify failed login with invalid credentials")
    public void verifyFailedLogin(String username, String password) {
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

        Assert.assertFalse(
            driver.getCurrentUrl().contains("products"),
            "Should NOT redirect to products with invalid credentials"
        );
    }
}
```

---

## `Iterator<Object[]>` — Lazy Loading for Large Data Sets

When you have hundreds or thousands of test cases, loading all data into memory at once is wasteful. Return an `Iterator<Object[]>` instead of `Object[][]` for lazy loading:

```java
package com.shopeasy.tests;

import com.shopeasy.tests.base.BaseTest;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ProductSearchIteratorTest extends BaseTest {

    // Iterator version: TestNG pulls the next item only when needed
    @DataProvider(name = "searchTermsIterator")
    public Iterator<Object[]> searchTermsLazy() {
        List<Object[]> data = new ArrayList<>();

        // Imagine these come from a database or CSV file
        data.add(new Object[]{ "laptop",   true  });
        data.add(new Object[]{ "phone",    true  });
        data.add(new Object[]{ "tablet",   true  });
        data.add(new Object[]{ "earphone", true  });
        data.add(new Object[]{ "xyz999",   false });

        // Return an iterator — TestNG calls next() for each test run
        return data.iterator();
    }

    @Test(dataProvider = "searchTermsIterator",
          description = "Search products using lazy-loaded iterator data")
    public void verifySearchWithIterator(String keyword, boolean expectResults) {
        navigateTo("products");

        driver.findElement(By.cssSelector("input[placeholder]")).sendKeys(keyword);

        try { Thread.sleep(500); } catch (InterruptedException e) { }

        int count = driver.findElements(By.cssSelector(".card")).size();
        if (expectResults) {
            Assert.assertTrue(count > 0,
                "Expected results for keyword: " + keyword);
        } else {
            Assert.assertEquals(count, 0,
                "Expected no results for keyword: " + keyword);
        }
    }
}
```

**Object[][] vs Iterator<Object[]>:**

| | `Object[][]` | `Iterator<Object[]>` |
|---|---|---|
| Loading | All data loaded upfront | Data loaded lazily, one row at a time |
| Memory | More memory (all rows in memory) | Less memory |
| Use case | Small to medium data sets | Large data sets (1000+ rows) |
| Simpler syntax | Yes | Slightly more verbose |
| From DB/file | You read all rows first | Can read row by row as iterator |

---

## `@DataProvider(parallel = true)` — Parallel Data Tests

Run each data row in a separate thread:

```java
@DataProvider(name = "parallelSearchTerms", parallel = true)
public Object[][] parallelSearchData() {
    return new Object[][] {
        { "laptop"   },
        { "phone"    },
        { "tablet"   },
        { "shirt"    },
    };
}

// Each row runs in a separate thread (requires ThreadLocal WebDriver)
@Test(dataProvider = "parallelSearchTerms",
      description = "Search tests run in parallel via data provider")
public void verifySearchInParallel(String keyword) {
    navigateTo("products");
    driver.findElement(By.cssSelector("input[placeholder]")).sendKeys(keyword);
    try { Thread.sleep(500); } catch (InterruptedException e) { }
    int count = driver.findElements(By.cssSelector(".card")).size();
    Assert.assertTrue(count >= 0, "Page should load for keyword: " + keyword);
}
```

> When using parallel data providers, use `ThreadLocal<WebDriver>` in `BaseTest`. See `07-parallel-execution.md`.

---

## Complete Example: Full Login Data-Driven Suite

```java
package com.shopeasy.tests;

import com.shopeasy.tests.base.BaseTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class FullLoginDataTest extends BaseTest {

    @DataProvider(name = "allLoginScenarios")
    public Object[][] allLoginScenarios() {
        return new Object[][] {
            // username       password         shouldRedirect  scenario
            { "testuser",   "password123",     true,           "valid customer login"     },
            { "admin",      "admin123",        true,           "valid admin login"         },
            { "wronguser",  "password123",     false,          "wrong username"            },
            { "testuser",   "wrongpassword",   false,          "wrong password"            },
            { "",           "password123",     false,          "empty username"            },
            { "testuser",   "",               false,           "empty password"            },
            { "TESTUSER",   "password123",     false,          "case sensitive username"   },
        };
    }

    @Test(dataProvider = "allLoginScenarios",
          description = "Comprehensive login validation with all scenarios")
    public void verifyAllLoginScenarios(String username, String password,
                                        boolean expectSuccess, String scenario) {
        navigateTo("login");

        WebElement usernameInput = driver.findElement(
            By.cssSelector("input[placeholder='Enter your username']")
        );
        WebElement passwordInput = driver.findElement(
            By.cssSelector("input[type='password']")
        );
        WebElement loginButton = driver.findElement(
            By.cssSelector("button[type='submit']")
        );

        usernameInput.sendKeys(username);
        passwordInput.sendKeys(password);
        loginButton.click();

        // Give AngularJS time to process
        try { Thread.sleep(1000); } catch (InterruptedException e) { }

        boolean redirected = driver.getCurrentUrl().contains("products")
                          || driver.getCurrentUrl().contains("admin");

        Assert.assertEquals(redirected, expectSuccess,
            "Scenario '" + scenario + "' failed: " +
            "expected redirect=" + expectSuccess +
            " but got URL: " + driver.getCurrentUrl());
    }
}
```

---

## Common Mistakes with `@DataProvider`

| Mistake | Problem | Fix |
|---|---|---|
| `name` mismatch between `@DataProvider` and `@Test(dataProvider=...)` | `TestNGException: No data provider named X found` | Make sure the strings match exactly |
| Method signature mismatch | `TestNGException: Wrong number of arguments` | Ensure `@Test` method parameters match `Object[]` row length |
| Returning `null` from `@DataProvider` | NPE at runtime | Always return at least one row |
| `static Object[][]` without `dataProviderClass` | Not found if in another class | Use `dataProviderClass` attribute |
| Forgetting `@DataProvider` annotation | TestNG ignores it | Always annotate the data method |
