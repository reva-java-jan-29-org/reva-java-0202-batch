# 02 — TestNG Assertions

## What are Assertions?

An **assertion** is a statement that checks whether a condition is true. If the condition is false, the assertion fails, and TestNG marks the test as FAILED. Without assertions, a test just runs steps but never actually verifies anything.

**Every test must have at least one assertion.**

```
Test without assertion:  Opens page → clicks button → closes browser → PASS (always passes — useless!)
Test with assertion:     Opens page → clicks button → Assert title = "ShopEasy" → PASS or FAIL
```

TestNG provides two assertion styles:
1. **Hard Assertions** — `Assert` class — stops the test immediately on failure
2. **Soft Assertions** — `SoftAssert` class — collects all failures and reports them at the end

---

## Hard Assertions — `org.testng.Assert`

Hard assertions stop the test the moment one fails. No code after the failing assertion runs.

```
@Test
  Assert.assertEquals(actual, expected)  ← if this FAILS → test stops here
  Assert.assertTrue(condition)           ← this line NEVER runs
  Assert.assertNotNull(value)            ← this line NEVER runs
```

**Import:**
```java
import org.testng.Assert;
```

---

## All `Assert` Methods

### `Assert.assertEquals(actual, expected)`

Checks that the actual value equals the expected value. The most commonly used assertion.

```java
package com.shopeasy.tests;

import com.shopeasy.tests.base.BaseTest;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AssertionsExampleTest extends BaseTest {

    @Test(description = "Verify ShopEasy page title")
    public void verifyPageTitle() {
        navigateTo("login");
        String actualTitle = driver.getTitle();
        String expectedTitle = "ShopEasy - E-Commerce";

        Assert.assertEquals(actualTitle, expectedTitle);
        // If actualTitle != expectedTitle:
        // FAILED: expected [ShopEasy - E-Commerce] but found [ShopEasy]
    }

    @Test(description = "Verify login button text")
    public void verifyLoginButtonText() {
        navigateTo("login");
        String actualText = driver.findElement(
            By.cssSelector("button[type='submit']")
        ).getText();

        Assert.assertEquals(actualText, "Login", "Login button text mismatch");
        //                               ^^^^^^^   ^^^^^^^^^^^^^^^^^^^^^^^^^^^
        //                               expected   custom failure message
    }

    @Test(description = "Verify product count on products page")
    public void verifyProductCount() {
        navigateTo("products");
        int actualCount = driver.findElements(By.cssSelector(".card")).size();
        // Verify exactly 6 products are shown
        Assert.assertEquals(actualCount, 6, "Expected exactly 6 product cards");
    }
}
```

> **Argument order matters:** `Assert.assertEquals(actual, expected)` — put the **actual** value first, **expected** second. Getting this wrong makes failure messages confusing.

---

### `Assert.assertNotEquals(actual, unexpected)`

Checks that the actual value does NOT equal the given value.

```java
@Test(description = "Cart count should change after adding a product")
public void verifyCartCountChanges() {
    navigateTo("products");
    String cartCountBefore = driver.findElement(
        By.cssSelector(".badge")
    ).getText();

    // Click "Add to Cart" on first product
    driver.findElement(By.cssSelector(".btn-primary")).click();

    String cartCountAfter = driver.findElement(
        By.cssSelector(".badge")
    ).getText();

    Assert.assertNotEquals(cartCountAfter, cartCountBefore,
        "Cart count should have increased after adding a product");
}

@Test(description = "Current URL should not be login after successful login")
public void verifyRedirectAfterLogin() {
    navigateTo("login");
    driver.findElement(
        By.cssSelector("input[placeholder='Enter your username']")
    ).sendKeys("testuser");
    driver.findElement(
        By.cssSelector("input[type='password']")
    ).sendKeys("password123");
    driver.findElement(
        By.cssSelector("button[type='submit']")
    ).click();

    Assert.assertNotEquals(driver.getCurrentUrl(),
        "http://localhost:4200/#!/login",
        "Should have redirected away from login page");
}
```

---

### `Assert.assertTrue(condition)`

Checks that a boolean condition is true.

```java
@Test(description = "Username input should be visible on login page")
public void verifyUsernameInputVisible() {
    navigateTo("login");
    boolean isVisible = driver.findElement(
        By.cssSelector("input[placeholder='Enter your username']")
    ).isDisplayed();

    Assert.assertTrue(isVisible, "Username input should be visible");
}

@Test(description = "Products page should load at least one product")
public void verifyProductsExist() {
    navigateTo("products");
    int count = driver.findElements(By.cssSelector(".card")).size();
    Assert.assertTrue(count > 0, "Expected at least one product card on page");
}

@Test(description = "Cart page URL should contain 'cart'")
public void verifyCartPageUrl() {
    navigateTo("cart");
    String currentUrl = driver.getCurrentUrl();
    Assert.assertTrue(currentUrl.contains("cart"),
        "URL should contain 'cart' — got: " + currentUrl);
}
```

---

### `Assert.assertFalse(condition)`

Checks that a boolean condition is false.

```java
@Test(description = "Login button should not be disabled on load")
public void verifyLoginButtonEnabled() {
    navigateTo("login");
    boolean isDisabled = !driver.findElement(
        By.cssSelector("button[type='submit']")
    ).isEnabled();

    Assert.assertFalse(isDisabled, "Login button should be enabled");
}

@Test(description = "Error message should not be visible on clean page load")
public void verifyNoErrorOnInitialLoad() {
    navigateTo("login");
    boolean errorVisible = driver.findElements(
        By.cssSelector(".alert-danger")
    ).size() > 0;

    Assert.assertFalse(errorVisible,
        "Error alert should NOT be shown on initial page load");
}
```

---

### `Assert.assertNull(object)`

Checks that an object is `null`. Used when you expect something to be absent.

```java
@Test(description = "Cookie should be null before login")
public void verifyCookieAbsentBeforeLogin() {
    navigateTo("login");
    // Check that auth token cookie does not exist yet
    org.openqa.selenium.Cookie authCookie = driver.manage().getCookieNamed("auth_token");
    Assert.assertNull(authCookie, "Auth token cookie should not exist before login");
}
```

---

### `Assert.assertNotNull(object)`

Checks that an object is NOT `null`. Used when you expect something to be present.

```java
@Test(description = "Page source should not be null")
public void verifyPageSourceNotNull() {
    navigateTo("products");
    String pageSource = driver.getPageSource();
    Assert.assertNotNull(pageSource, "Page source should not be null");
}

@Test(description = "Product modal element should exist in DOM")
public void verifyProductModalExistsInDom() {
    navigateTo("admin/products");
    // Find the modal element — it exists in DOM but is hidden
    org.openqa.selenium.WebElement modal = driver.findElement(By.id("productModal"));
    Assert.assertNotNull(modal, "Product modal element should exist in DOM");
}
```

---

### `Assert.assertSame(actual, expected)`

Checks that two object references point to the **same object in memory** (uses `==`, not `.equals()`). Rarely used in Selenium tests.

```java
@Test(description = "Demonstrate assertSame with same object reference")
public void demonstrateAssertSame() {
    String s1 = "ShopEasy";
    String s2 = s1;           // same reference
    Assert.assertSame(s1, s2); // PASSES — same object in memory
}
```

---

### `Assert.assertNotSame(actual, unexpected)`

Checks that two references point to **different** objects in memory.

```java
@Test(description = "Demonstrate assertNotSame with different string objects")
public void demonstrateAssertNotSame() {
    String s1 = new String("ShopEasy");
    String s2 = new String("ShopEasy"); // different objects, same content
    Assert.assertNotSame(s1, s2);       // PASSES — different objects in memory
    Assert.assertEquals(s1, s2);        // ALSO PASSES — same content
}
```

---

### `Assert.fail(message)`

Unconditionally fails the test with a message. Use when you want to fail a test based on custom logic that cannot be expressed with a simple assertion.

```java
@Test(description = "Fail the test if login error is unexpected")
public void verifyLoginWithInvalidCredentials() {
    navigateTo("login");
    driver.findElement(
        By.cssSelector("input[placeholder='Enter your username']")
    ).sendKeys("wronguser");
    driver.findElement(
        By.cssSelector("input[type='password']")
    ).sendKeys("wrongpassword");
    driver.findElement(
        By.cssSelector("button[type='submit']")
    ).click();

    // We expect to stay on login page OR see an error message
    String url = driver.getCurrentUrl();
    if (url.contains("products")) {
        Assert.fail("Login with wrong credentials should NOT redirect to products page");
    }

    // If we reach here, the URL was correct — now check error message
    boolean errorShown = driver.findElements(
        By.cssSelector(".alert-danger")
    ).size() > 0;
    Assert.assertTrue(errorShown, "Should show error message for invalid credentials");
}
```

---

## Quick Reference Table

| Method | Checks | Fails when |
|---|---|---|
| `assertEquals(a, b)` | `a.equals(b)` | values are different |
| `assertNotEquals(a, b)` | `!a.equals(b)` | values are the same |
| `assertTrue(cond)` | `cond == true` | condition is false |
| `assertFalse(cond)` | `cond == false` | condition is true |
| `assertNull(obj)` | `obj == null` | object is not null |
| `assertNotNull(obj)` | `obj != null` | object is null |
| `assertSame(a, b)` | `a == b` | different references |
| `assertNotSame(a, b)` | `a != b` | same reference |
| `fail(msg)` | — | always (unconditional) |

---

## Hard Assertions — The Problem

Consider testing multiple things on the products page. With hard assertions, if the first assertion fails, you never find out about the second and third failures:

```java
@Test
public void verifyProductsPageHardAssert() {
    navigateTo("products");

    // If this fails, the test stops immediately
    Assert.assertEquals(driver.getTitle(), "ShopEasy - E-Commerce");

    // These NEVER run if the assertion above failed
    Assert.assertTrue(driver.findElements(By.cssSelector(".card")).size() > 0);
    Assert.assertTrue(driver.findElement(By.cssSelector("input[placeholder]")).isDisplayed());
}
```

When multiple assertions might fail in one test, you want to know about all of them — not just the first. This is where **Soft Assertions** help.

---

## Soft Assertions — `SoftAssert`

`SoftAssert` collects all assertion failures during the test and reports them all at the end when you call `softAssert.assertAll()`. The test continues running even after a soft assertion fails.

```
SoftAssert behavior:
  softAssert.assertEquals(...)  ← fails silently — records the failure, continues
  softAssert.assertTrue(...)    ← also fails silently — records the failure, continues
  softAssert.assertNotNull(...) ← passes
  softAssert.assertAll()        ← NOW reports all failures at once
```

**Import:**
```java
import org.testng.asserts.SoftAssert;
```

---

### Basic `SoftAssert` Example

```java
package com.shopeasy.tests;

import com.shopeasy.tests.base.BaseTest;
import org.openqa.selenium.By;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

public class ProductsPageSoftAssertTest extends BaseTest {

    @Test(description = "Verify multiple aspects of the products page using soft assertions")
    public void verifyProductsPageCompletely() {
        navigateTo("products");

        // Create a new SoftAssert instance per test
        SoftAssert softAssert = new SoftAssert();

        // 1. Verify page title
        softAssert.assertEquals(
            driver.getTitle(),
            "ShopEasy - E-Commerce",
            "Page title mismatch"
        );

        // 2. Verify at least one product card exists
        int cardCount = driver.findElements(By.cssSelector(".card")).size();
        softAssert.assertTrue(cardCount > 0, "Should have at least one product");

        // 3. Verify search bar is present
        softAssert.assertTrue(
            driver.findElements(By.cssSelector("input[placeholder]")).size() > 0,
            "Search input should be present"
        );

        // 4. Verify navbar is displayed
        softAssert.assertTrue(
            driver.findElement(By.cssSelector("nav")).isDisplayed(),
            "Navbar should be displayed"
        );

        // MUST call assertAll() — otherwise failures are lost silently!
        softAssert.assertAll();
        // If any of the above failed, assertAll() throws with ALL failure messages
    }
}
```

---

### ShopEasy Cart Page — Soft Assert Example

```java
@Test(description = "Verify cart page elements are present after adding an item")
public void verifyCartPageElements() {
    // Step 1: Add a product to cart from the products page
    navigateTo("products");
    driver.findElement(By.cssSelector(".btn-primary")).click();

    // Step 2: Navigate to cart
    navigateTo("cart");

    SoftAssert softAssert = new SoftAssert();

    // Verify multiple cart page elements — don't stop on first failure
    softAssert.assertEquals(
        driver.getTitle(), "ShopEasy - E-Commerce",
        "Cart page title should match"
    );

    softAssert.assertTrue(
        driver.findElements(By.cssSelector("table")).size() > 0,
        "Cart should show a table of items"
    );

    softAssert.assertTrue(
        driver.findElements(By.cssSelector(".btn-danger")).size() > 0,
        "Cart should have a remove button"
    );

    softAssert.assertTrue(
        driver.getCurrentUrl().contains("cart"),
        "URL should contain 'cart'"
    );

    // Report all failures at once
    softAssert.assertAll();
}
```

---

### `SoftAssert` — One Instance Per Test

**CRITICAL RULE:** Create a **new** `SoftAssert` instance for every `@Test` method. Never share a `SoftAssert` across tests.

```java
// WRONG: shared SoftAssert — failures from test1 leak into test2
public class BadExample extends BaseTest {
    SoftAssert softAssert = new SoftAssert(); // class-level — WRONG

    @Test
    public void test1() {
        softAssert.assertTrue(false, "failure from test1");
        softAssert.assertAll(); // reports test1's failure
    }

    @Test
    public void test2() {
        // softAssert still has failures from test1 — incorrect!
        softAssert.assertAll(); // INCORRECTLY reports test1's failure in test2
    }
}

// CORRECT: new SoftAssert per test
public class GoodExample extends BaseTest {

    @Test
    public void test1() {
        SoftAssert softAssert = new SoftAssert(); // local — CORRECT
        softAssert.assertTrue(false, "failure from test1");
        softAssert.assertAll();
    }

    @Test
    public void test2() {
        SoftAssert softAssert = new SoftAssert(); // fresh instance
        softAssert.assertTrue(true);
        softAssert.assertAll();
    }
}
```

---

## Hard vs Soft Assertions — When to Use Each

| Situation | Use | Reason |
|---|---|---|
| Verifying a single critical condition | Hard (`Assert`) | Fast feedback; if this fails, nothing else matters |
| Verifying page redirected after login | Hard | Navigation must succeed before any other checks |
| Verifying 5+ attributes on one page | Soft (`SoftAssert`) | Get all failures in one run |
| Checking all form validation messages | Soft | All messages appear at once |
| Checking all product card fields | Soft | One product card having a wrong price shouldn't hide other issues |
| Pre-condition check (page must load) | Hard | No point continuing if setup failed |
| Checking email/phone/address in a form | Soft | Want to see which specific fields are wrong |

**Rule of thumb:**
- If the rest of the test cannot proceed after a failure → use **Hard**
- If you want to collect all failures in one test run → use **Soft**

---

## Assertion with Custom Failure Messages

Always provide a custom message as the last argument. It makes failures much easier to debug:

```java
// Without message — not helpful
Assert.assertEquals(actualCount, 6);
// Failure: expected [6] but found [5]

// With message — very helpful
Assert.assertEquals(actualCount, 6,
    "Products page should show exactly 6 products, but showed: " + actualCount);
// Failure: Products page should show exactly 6 products, but showed: 5 — expected [6] but found [5]
```

```java
@Test(description = "Complete login page verification with custom messages")
public void verifyLoginPageCompletely() {
    navigateTo("login");
    SoftAssert sa = new SoftAssert();

    sa.assertEquals(
        driver.getTitle(),
        "ShopEasy - E-Commerce",
        "Browser tab title should be 'ShopEasy - E-Commerce'"
    );

    sa.assertTrue(
        driver.findElement(
            By.cssSelector("input[placeholder='Enter your username']")
        ).isDisplayed(),
        "Username input field should be visible on the login page"
    );

    sa.assertTrue(
        driver.findElement(
            By.cssSelector("input[type='password']")
        ).isDisplayed(),
        "Password input field should be visible on the login page"
    );

    sa.assertTrue(
        driver.findElement(
            By.cssSelector("button[type='submit']")
        ).isDisplayed(),
        "Login submit button should be visible on the login page"
    );

    sa.assertTrue(
        driver.findElement(
            By.cssSelector("a[href*='register']")
        ).isDisplayed(),
        "Register link should be visible on the login page"
    );

    sa.assertAll();
}
```

---

## Complete ShopEasy Assertion Examples

### Verify Products Page

```java
@Test(description = "Verify complete products page state")
public void verifyProductsPage() {
    navigateTo("products");
    SoftAssert sa = new SoftAssert();

    // Title
    sa.assertEquals(driver.getTitle(), "ShopEasy - E-Commerce");

    // URL
    sa.assertTrue(driver.getCurrentUrl().contains("products"));

    // At least one product
    sa.assertTrue(
        driver.findElements(By.cssSelector(".card")).size() > 0,
        "At least one product should be displayed"
    );

    // Category dropdown present
    sa.assertTrue(
        driver.findElements(By.cssSelector("select")).size() > 0,
        "Category filter dropdown should be present"
    );

    sa.assertAll();
}
```

### Verify Cart Count

```java
@Test(description = "Cart badge count should increment after adding product")
public void verifyCartBadgeIncrement() {
    navigateTo("products");

    // Get cart count before adding
    int before = Integer.parseInt(
        driver.findElement(By.cssSelector(".badge")).getText().trim()
    );

    // Add first product
    driver.findElement(By.cssSelector(".btn-primary")).click();

    // Get cart count after adding
    int after = Integer.parseInt(
        driver.findElement(By.cssSelector(".badge")).getText().trim()
    );

    Assert.assertEquals(after, before + 1,
        "Cart badge should increase by 1 after adding a product");
}
```
