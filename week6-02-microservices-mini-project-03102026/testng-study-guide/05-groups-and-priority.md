# 05 — Groups and Priority

## What are Groups?

Groups let you tag test methods with one or more category names (e.g., `"smoke"`, `"regression"`, `"login"`, `"admin"`). You can then selectively run only certain groups — without changing your test code — by configuring `testng.xml`.

**Why use groups?**
- Run a fast "smoke" check in 2 minutes instead of the full 30-minute regression
- Run only "login" tests when fixing login bugs
- Exclude "admin" tests in environments where admin isn't set up
- Exclude "slow" or "destructive" tests from CI pipelines

---

## Defining Groups

Add groups to any `@Test` method using the `groups` attribute:

```java
package com.shopeasy.tests;

import com.shopeasy.tests.base.BaseTest;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {

    // Single group
    @Test(groups = "smoke",
          description = "Quick check: login page loads correctly")
    public void verifyLoginPageLoads() {
        navigateTo("login");
        Assert.assertEquals(driver.getTitle(), "ShopEasy - E-Commerce");
    }

    // Multiple groups — this test belongs to BOTH smoke AND login
    @Test(groups = {"smoke", "login"},
          description = "Verify login form has username and password fields")
    public void verifyLoginFormElements() {
        navigateTo("login");
        Assert.assertTrue(
            driver.findElement(
                By.cssSelector("input[placeholder='Enter your username']")
            ).isDisplayed()
        );
        Assert.assertTrue(
            driver.findElement(
                By.cssSelector("input[type='password']")
            ).isDisplayed()
        );
    }

    // Regression group — slower, more detailed test
    @Test(groups = {"regression", "login"},
          description = "Verify login with valid credentials redirects to products")
    public void verifySuccessfulLogin() {
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
        Assert.assertTrue(driver.getCurrentUrl().contains("products"));
    }

    // Regression only
    @Test(groups = {"regression", "login"},
          description = "Verify login fails with incorrect password")
    public void verifyFailedLoginInvalidPassword() {
        navigateTo("login");
        driver.findElement(
            By.cssSelector("input[placeholder='Enter your username']")
        ).sendKeys("testuser");
        driver.findElement(
            By.cssSelector("input[type='password']")
        ).sendKeys("WRONGPASSWORD");
        driver.findElement(
            By.cssSelector("button[type='submit']")
        ).click();
        Assert.assertFalse(driver.getCurrentUrl().contains("products"),
            "Should not redirect with wrong password");
    }
}
```

---

## ShopEasy Test Group Taxonomy

A recommended grouping strategy for ShopEasy tests:

| Group Name | Purpose | Typical Run Time | Tests Included |
|---|---|---|---|
| `smoke` | Quick sanity check — does the app load? | ~2 min | Page loads, titles, basic nav |
| `regression` | Full test coverage | ~30 min | All test scenarios |
| `login` | Login and auth flows | ~5 min | Login, register, logout |
| `products` | Product catalog features | ~5 min | Search, filter, view details |
| `cart` | Cart and checkout flows | ~8 min | Add to cart, checkout, payment |
| `orders` | Order management | ~5 min | View orders, order details |
| `admin` | Admin panel operations | ~10 min | Dashboard, CRUD, manage users |
| `slow` | Tests that take >15 seconds | variable | Large data loads, reports |
| `destructive` | Tests that modify/delete data | careful | Delete products, cancel orders |

---

## Running Groups via `testng.xml`

### Include Specific Groups

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">

<suite name="ShopEasy Smoke Suite" verbose="1">
    <test name="Smoke Tests">
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

### Exclude Groups

```xml
<suite name="ShopEasy Safe Regression">
    <test name="Regression Without Destructive Tests">
        <groups>
            <run>
                <include name="regression"/>
                <exclude name="destructive"/>   <!-- skip delete/modify tests -->
                <exclude name="slow"/>          <!-- skip time-consuming tests -->
            </run>
        </groups>
        <packages>
            <package name="com.shopeasy.tests"/>
        </packages>
    </test>
</suite>
```

### Include Multiple Groups

```xml
<suite name="Login and Smoke Suite">
    <test name="Login + Smoke">
        <groups>
            <run>
                <include name="smoke"/>
                <include name="login"/>
                <!-- test belongs to EITHER smoke OR login → runs -->
            </run>
        </groups>
        <packages>
            <package name="com.shopeasy.tests"/>
        </packages>
    </test>
</suite>
```

---

## Full ShopEasy Groups Example

```java
package com.shopeasy.tests;

import com.shopeasy.tests.base.BaseTest;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ProductTest extends BaseTest {

    @Test(groups = {"smoke", "products"},
          description = "Smoke: Products page loads")
    public void verifyProductsPageLoads() {
        navigateTo("products");
        Assert.assertEquals(driver.getTitle(), "ShopEasy - E-Commerce");
        Assert.assertTrue(
            driver.findElements(By.cssSelector(".card")).size() > 0,
            "Should display product cards"
        );
    }

    @Test(groups = {"regression", "products"},
          description = "Regression: Product search by keyword")
    public void verifyProductSearch() {
        navigateTo("products");
        driver.findElement(By.cssSelector("input[placeholder]")).sendKeys("laptop");
        try { Thread.sleep(500); } catch (InterruptedException e) { }
        int count = driver.findElements(By.cssSelector(".card")).size();
        Assert.assertTrue(count > 0, "Search for 'laptop' should return results");
    }

    @Test(groups = {"regression", "products"},
          description = "Regression: Category filter works")
    public void verifyCategoryFilter() {
        navigateTo("products");
        new org.openqa.selenium.support.ui.Select(
            driver.findElement(By.cssSelector("select"))
        ).selectByVisibleText("Electronics");
        try { Thread.sleep(500); } catch (InterruptedException e) { }
        Assert.assertTrue(
            driver.findElements(By.cssSelector(".card")).size() > 0
        );
    }
}

// ─────────────────────────────────────────────────────────────────────────

public class CartTest extends BaseTest {

    @Test(groups = {"smoke", "cart"},
          description = "Smoke: Cart page is accessible")
    public void verifyCartPageLoads() {
        navigateTo("cart");
        Assert.assertEquals(driver.getTitle(), "ShopEasy - E-Commerce");
        Assert.assertTrue(driver.getCurrentUrl().contains("cart"));
    }

    @Test(groups = {"regression", "cart"},
          description = "Regression: Adding item updates cart badge")
    public void verifyAddToCartUpdatesBadge() {
        navigateTo("products");
        String beforeCount = driver.findElement(
            By.cssSelector(".badge")
        ).getText();
        driver.findElement(By.cssSelector(".btn-primary")).click();
        String afterCount = driver.findElement(
            By.cssSelector(".badge")
        ).getText();
        Assert.assertNotEquals(afterCount, beforeCount,
            "Badge count should change after adding item");
    }

    @Test(groups = {"destructive", "cart"},
          description = "Destructive: Checkout clears the cart")
    public void verifyCheckoutClearsCart() {
        // This test modifies app state — only run in dedicated test environments
        navigateTo("products");
        driver.findElement(By.cssSelector(".btn-primary")).click();
        navigateTo("cart");
        driver.findElement(By.cssSelector("button.btn-success")).click();
        // ... complete checkout
    }
}
```

---

## `@Test(priority = n)` — Execution Order

By default, TestNG runs test methods in an unpredictable order (alphabetical within a class, in practice). Use `priority` to control the order explicitly.

**Rules:**
- Lower priority number runs first
- Default priority is `0`
- Negative priorities are allowed (e.g., `-1` runs before `0`)
- Two methods with the same priority run in unpredictable order
- Priority applies within a class

```java
package com.shopeasy.tests;

import com.shopeasy.tests.base.BaseTest;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CheckoutFlowTest extends BaseTest {

    // Priority 1: Navigate to products (first step)
    @Test(priority = 1, description = "Step 1: Verify products page loads")
    public void verifyProductsPage() {
        navigateTo("products");
        Assert.assertEquals(driver.getTitle(), "ShopEasy - E-Commerce");
        System.out.println("Step 1 complete: products page loaded");
    }

    // Priority 2: Add item to cart (second step)
    @Test(priority = 2, description = "Step 2: Add product to cart")
    public void addProductToCart() {
        navigateTo("products");
        driver.findElement(By.cssSelector(".btn-primary")).click();
        System.out.println("Step 2 complete: product added to cart");
    }

    // Priority 3: View cart (third step)
    @Test(priority = 3, description = "Step 3: Verify cart has the product")
    public void verifyCartHasProduct() {
        navigateTo("cart");
        int cartRows = driver.findElements(By.cssSelector("table tbody tr")).size();
        Assert.assertTrue(cartRows > 0, "Cart should have at least one item");
        System.out.println("Step 3 complete: cart verified");
    }

    // Priority 4: Proceed to checkout (fourth step)
    @Test(priority = 4, description = "Step 4: Verify checkout form appears")
    public void verifyCheckoutForm() {
        navigateTo("cart");
        // Checkout button should be visible
        Assert.assertTrue(
            driver.findElement(By.cssSelector("button.btn-success")).isDisplayed()
        );
        System.out.println("Step 4 complete: checkout form verified");
    }
}
```

**Execution order:**
```
1. verifyProductsPage()    (priority=1)
2. addProductToCart()      (priority=2)
3. verifyCartHasProduct()  (priority=3)
4. verifyCheckoutForm()    (priority=4)
```

---

## Priority with Groups Together

```java
public class AdminTest extends BaseTest {

    @Test(groups = {"smoke", "admin"}, priority = 1,
          description = "Admin dashboard loads first")
    public void verifyDashboard() {
        navigateTo("admin/dashboard");
        Assert.assertEquals(driver.getTitle(), "ShopEasy - E-Commerce");
    }

    @Test(groups = {"regression", "admin"}, priority = 2,
          description = "Admin products table loads second")
    public void verifyProductsTable() {
        navigateTo("admin/products");
        Assert.assertTrue(
            driver.findElement(By.cssSelector("table")).isDisplayed()
        );
    }

    @Test(groups = {"regression", "admin"}, priority = 3,
          description = "Admin customers table loads third")
    public void verifyCustomersTable() {
        navigateTo("admin/customers");
        Assert.assertTrue(
            driver.findElement(By.cssSelector("table")).isDisplayed()
        );
    }

    @Test(groups = {"destructive", "admin"}, priority = 10,
          description = "Admin can add a product — run last")
    public void verifyAddProduct() {
        navigateTo("admin/products");
        driver.findElement(By.cssSelector("button.btn-primary")).click();
        // ... fill modal form
    }
}
```

---

## `dependsOnMethods` — Method Dependencies

`dependsOnMethods` tells TestNG: "don't run this test unless the listed test(s) passed first." If the dependency fails, the dependent test is **skipped** (not failed).

```java
package com.shopeasy.tests;

import com.shopeasy.tests.base.BaseTest;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

public class OrderFlowTest extends BaseTest {

    @Test(description = "Step 1: User must be logged in")
    public void loginAsCustomer() {
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
        Assert.assertTrue(driver.getCurrentUrl().contains("products"),
            "Must be logged in before proceeding");
    }

    // This test depends on loginAsCustomer passing
    @Test(dependsOnMethods = "loginAsCustomer",
          description = "Step 2: Add product to cart (requires login)")
    public void addProductToCart() {
        navigateTo("products");
        driver.findElement(By.cssSelector(".btn-primary")).click();
        // verify badge changed
        Assert.assertTrue(
            Integer.parseInt(
                driver.findElement(By.cssSelector(".badge")).getText()
            ) > 0
        );
    }

    // This test depends on addProductToCart passing
    @Test(dependsOnMethods = "addProductToCart",
          description = "Step 3: Place order from cart (requires item in cart)")
    public void placeOrder() {
        navigateTo("cart");
        Assert.assertTrue(
            driver.findElements(By.cssSelector("table tbody tr")).size() > 0,
            "Cart must have an item before placing order"
        );
        driver.findElement(By.cssSelector("button.btn-success")).click();
        // ... complete order
    }

    // Depend on multiple methods
    @Test(dependsOnMethods = {"loginAsCustomer", "placeOrder"},
          description = "Step 4: Verify order appears in order history")
    public void verifyOrderHistory() {
        navigateTo("orders");
        Assert.assertTrue(
            driver.findElements(By.cssSelector("table tbody tr")).size() > 0,
            "Order history should show the placed order"
        );
    }
}
```

**Behavior when dependency fails:**
```
loginAsCustomer  → FAILED
addProductToCart → SKIPPED (dependency failed)
placeOrder       → SKIPPED (addProductToCart was skipped)
verifyOrderHistory → SKIPPED (loginAsCustomer and placeOrder both skipped)
```

---

## `dependsOnGroups` — Group Dependencies

Similar to `dependsOnMethods` but depends on all tests in a group passing:

```java
public class PaymentTest extends BaseTest {

    // These tests belong to the "cart" group
    @Test(groups = "cart", description = "Add product to cart")
    public void addToCart() {
        navigateTo("products");
        driver.findElement(By.cssSelector(".btn-primary")).click();
        Assert.assertTrue(
            Integer.parseInt(
                driver.findElement(By.cssSelector(".badge")).getText()
            ) > 0
        );
    }

    @Test(groups = "cart", description = "Verify cart has item")
    public void verifyCartNotEmpty() {
        navigateTo("cart");
        Assert.assertTrue(
            driver.findElements(By.cssSelector("table tbody tr")).size() > 0
        );
    }

    // This test only runs if ALL tests in "cart" group passed
    @Test(dependsOnGroups = "cart",
          description = "Process payment — requires cart to be fully working")
    public void processPayment() {
        navigateTo("cart");
        driver.findElement(By.cssSelector("button.btn-success")).click();

        // Fill payment form
        driver.findElement(
            By.cssSelector("input[placeholder='Card Number']")
        ).sendKeys("4242424242424242");

        driver.findElement(
            By.cssSelector("input[placeholder='Expiry Date']")
        ).sendKeys("12/26");

        driver.findElement(
            By.cssSelector("input[placeholder='CVV']")
        ).sendKeys("123");

        driver.findElement(
            By.cssSelector("button[type='submit']")
        ).click();

        Assert.assertTrue(driver.getCurrentUrl().contains("orders"),
            "Should redirect to orders after payment");
    }
}
```

---

## `alwaysRun` — Override Dependencies

Mark a test with `alwaysRun = true` to run even if its dependencies failed or were skipped. Useful for cleanup tests:

```java
@Test(alwaysRun = true,
      description = "Always verify logout works, regardless of test outcome")
public void verifyLogout() {
    // This test runs even if earlier tests failed
    navigateTo("login");
    Assert.assertEquals(driver.getTitle(), "ShopEasy - E-Commerce");
}
```

---

## Practical ShopEasy Test Suite Organization

```
com.shopeasy.tests/
├── LoginTest.java         groups: smoke, regression, login
│   ├── verifyLoginPageLoads        → smoke, login
│   ├── verifyLoginFormElements     → smoke, login
│   ├── verifySuccessfulLogin       → regression, login
│   └── verifyInvalidLogin          → regression, login
│
├── ProductTest.java       groups: smoke, regression, products
│   ├── verifyProductsPageLoads     → smoke, products
│   ├── verifyProductSearch         → regression, products
│   └── verifyCategoryFilter        → regression, products
│
├── CartTest.java          groups: smoke, regression, cart
│   ├── verifyCartPageLoads         → smoke, cart
│   ├── verifyAddToCart             → regression, cart
│   └── verifyRemoveFromCart        → regression, cart
│
├── OrderTest.java         groups: regression, orders
│   ├── verifyOrderHistory          → regression, orders
│   └── verifyOrderDetails          → regression, orders
│
└── AdminTest.java         groups: regression, admin
    ├── verifyDashboard             → smoke, admin
    ├── verifyProductCRUD           → regression, admin, destructive
    └── verifyCustomerManagement    → regression, admin
```

**Suite files:**

| File | Groups Included | Groups Excluded | Approx Run Time |
|---|---|---|---|
| `smoke.xml` | `smoke` | — | ~3 min |
| `regression.xml` | `regression` | `destructive` | ~20 min |
| `full.xml` | all | — | ~30 min |
| `admin.xml` | `admin` | `destructive` | ~10 min |
| `login.xml` | `login` | — | ~5 min |
