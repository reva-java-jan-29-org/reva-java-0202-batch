# 05 — Assertions

## The assertThat() API

Playwright provides its own assertion library called **`PlaywrightAssertions`**. It is used via the static `assertThat()` method. These assertions are **auto-retrying** — if the condition is not immediately true, Playwright keeps retrying for up to the assertion timeout (default: 5 seconds) before failing.

```java
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
```

There are two overloads of `assertThat`:

| Target | Method | Assertion Class |
|---|---|---|
| A `Page` | `assertThat(page)` | `PageAssertions` |
| A `Locator` | `assertThat(locator)` | `LocatorAssertions` |
| An `APIResponse` | `assertThat(response)` | `APIResponseAssertions` |

---

## Page Assertions

### hasURL()

```java
import com.shopeasy.playwright.base.BaseTest;
import com.microsoft.playwright.Locator;
import org.testng.annotations.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

// Exact URL match
assertThat(page).hasURL("http://localhost:4200/#!/login");

// URL pattern match (** = wildcard)
assertThat(page).hasURL("**/#!/login");
assertThat(page).hasURL("**/#!/admin/dashboard");
assertThat(page).hasURL("**/#!/products");

// After login, assert redirect happened
page.locator("button[type='submit']").click();
assertThat(page).hasURL("**/#!/admin/dashboard");
```

### hasTitle()

```java
// Exact title match
assertThat(page).hasTitle("ShopEasy - E-Commerce");

// Pattern match using regex
assertThat(page).hasTitle(java.util.regex.Pattern.compile("ShopEasy.*"));
```

---

## Locator Assertions

### isVisible()

Element is attached to DOM and visible (not hidden by CSS).

```java
// Assert login form elements are visible
assertThat(page.getByPlaceholder("Enter your username")).isVisible();
assertThat(page.locator("button[type='submit']")).isVisible();

// Assert success alert is visible after adding to cart
page.locator(".product-card").first().locator("button.btn-primary").click();
assertThat(page.locator(".alert-success")).isVisible();

// Assert navbar shows logged-in user
assertThat(page.locator(".navbar .dropdown-toggle")).isVisible();

// Assert admin dashboard stats cards are visible
assertThat(page.locator(".card.shadow-sm").first()).isVisible();
```

### isHidden()

Element is NOT visible (opposite of isVisible).

```java
// Assert error message is NOT shown on page load
navigateTo("login");
assertThat(page.locator(".alert-danger")).isHidden();

// Assert loading spinner is gone after page load
assertThat(page.locator(".spinner-border")).isHidden();

// Assert modal is not visible before opening it
assertThat(page.locator("#productModal")).isHidden();
```

### isEnabled() and isDisabled()

```java
// Assert login button is enabled when form has values
page.getByPlaceholder("Enter your username").fill("admin");
page.getByPlaceholder("Enter your password").fill("admin123");
assertThat(page.locator("button[type='submit']")).isEnabled();

// Assert button is disabled when form is empty
// (if your app disables the button until form is valid)
assertThat(page.locator("button[type='submit']")).isDisabled();
```

### isChecked() and isUnchecked()

```java
// Assert checkbox is checked
page.locator("input[type='checkbox']").check();
assertThat(page.locator("input[type='checkbox']")).isChecked();

// Assert radio button for "Credit Card" is selected
assertThat(page.locator("input[value='credit']")).isChecked();

// Assert checkbox is unchecked
assertThat(page.locator("input#terms")).isUnchecked();
```

### hasText()

Element's text content exactly equals the given string.

```java
// Assert exact heading text
assertThat(page.locator("h1")).hasText("ShopEasy");

// Assert button label
assertThat(page.locator("button[type='submit']")).hasText("Login");

// Assert table cell value
assertThat(page.locator("tbody tr").first().locator("td").nth(1)).hasText("Laptop Pro");

// Assert success message text
assertThat(page.locator(".alert-success")).hasText("Product added to cart successfully!");
```

### containsText()

Element's text content **contains** the given string (partial match).

```java
// Assert success alert contains "added to cart" (partial)
assertThat(page.locator(".alert-success")).containsText("added to cart");

// Assert error alert contains "Invalid" (partial)
assertThat(page.locator(".alert-danger")).containsText("Invalid");

// Assert product price contains "$"
assertThat(page.locator(".product-card").first().locator(".text-success"))
    .containsText("$");

// Assert dashboard welcome message contains admin username
assertThat(page.locator(".welcome-message")).containsText("admin");

// Assert navbar brand
assertThat(page.locator(".navbar-brand")).containsText("ShopEasy");
```

### hasValue()

Checks the current value of a form input.

```java
// After filling username, verify the value
page.getByPlaceholder("Enter your username").fill("admin");
assertThat(page.getByPlaceholder("Enter your username")).hasValue("admin");

// After clearing, verify value is empty
page.getByPlaceholder("Enter your username").clear();
assertThat(page.getByPlaceholder("Enter your username")).hasValue("");

// Verify dropdown selected value
page.locator("select[ng-model='selectedCategory']").selectOption("Electronics");
assertThat(page.locator("select[ng-model='selectedCategory']")).hasValue("Electronics");
```

### hasCount()

Checks the number of matching elements.

```java
// Assert at least some products loaded
// (use when you know exactly how many to expect)
assertThat(page.locator(".product-card")).hasCount(10);

// Assert admin table has a specific number of rows
assertThat(page.locator("#productsTable tbody tr")).hasCount(5);

// After filtering by category, check count changed
page.locator("select[ng-model='selectedCategory']").selectOption("Electronics");
// Assuming 4 electronics products in test data:
assertThat(page.locator(".product-card")).hasCount(4);

// After search returns no results
page.getByPlaceholder("Search products...").fill("zzz_nonexistent");
assertThat(page.locator(".product-card")).hasCount(0);
```

### hasAttribute()

Checks that an element has a specific attribute with a specific value.

```java
// Assert input type is password
assertThat(page.locator("input[ng-model='loginData.password']"))
    .hasAttribute("type", "password");

// Assert a link href
assertThat(page.locator("a.navbar-brand"))
    .hasAttribute("href", "#!/products");

// Assert Bootstrap modal has specific class when open
// (Bootstrap adds "show" class to visible modals)
assertThat(page.locator("#productModal"))
    .hasAttribute("class", java.util.regex.Pattern.compile(".*show.*"));
```

### hasClass()

```java
// Assert element has a specific CSS class
assertThat(page.locator(".navbar")).hasClass(
    java.util.regex.Pattern.compile(".*navbar-expand-lg.*")
);

// Assert active nav item
assertThat(page.locator(".nav-link.active")).hasClass(
    java.util.regex.Pattern.compile(".*active.*")
);
```

---

## Negating Assertions

Every assertion can be negated with `.not()`:

```java
// NOT visible
assertThat(page.locator(".alert-danger")).not().isVisible();

// Does NOT contain text
assertThat(page.locator(".navbar")).not().containsText("Login");

// Is NOT disabled
assertThat(page.locator("button[type='submit']")).not().isDisabled();

// Does NOT have value
assertThat(page.getByPlaceholder("Enter your username")).not().hasValue("admin");

// URL does NOT contain admin
assertThat(page).not().hasURL("**/#!/admin/**");
```

---

## Soft Assertions

Standard assertions fail the test immediately when the first assertion fails. **Soft assertions** collect all failures and report them together at the end.

```java
import com.microsoft.playwright.assertions.PlaywrightAssertions;

// Create a soft assertions context
var softAssertions = PlaywrightAssertions.assertThatSoftly();

navigateTo("login");

// These do NOT throw immediately — they collect failures
softAssertions.assertThat(page.getByPlaceholder("Enter your username")).isVisible();
softAssertions.assertThat(page.getByPlaceholder("Enter your password")).isVisible();
softAssertions.assertThat(page.locator("button[type='submit']")).isVisible();
softAssertions.assertThat(page.locator("a[href='#!/register']")).isVisible();
softAssertions.assertThat(page).hasTitle("ShopEasy - E-Commerce");

// ALL failures are reported here
softAssertions.assertAll();  // Throws if any assertion above failed
```

**When to use soft assertions:**
- Verifying multiple elements on a page at once
- Checking all form field validation errors
- Verifying a complete page layout without stopping at first failure

---

## Custom Timeout for Assertions

By default, assertions retry for up to 5 seconds. Override with `timeout()`:

```java
import java.time.Duration;

// Wait up to 10 seconds for the element to be visible
assertThat(page.locator(".product-card").first())
    .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(10_000));

// Wait up to 20 seconds for URL change (slow server)
assertThat(page)
    .hasURL("**/#!/admin/dashboard",
        new PageAssertions.HasURLOptions().setTimeout(20_000));

// Very short timeout — fail fast if should appear immediately
assertThat(page.locator(".alert-success"))
    .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(500));
```

---

## Playwright assertThat() vs TestNG Assert — Comparison

| Feature | Playwright `assertThat()` | TestNG `Assert` |
|---|---|---|
| Auto-retry on failure | Yes (retries until timeout) | No (evaluates once) |
| Failure message | Descriptive (what was expected vs actual) | Can be customized |
| Soft assertions | `assertThatSoftly()` | `SoftAssert` class |
| Async-aware | Yes | No |
| Locator assertions | Built-in (`isVisible`, `hasText`, etc.) | Must use `isDisplayed()`, `getText()` manually |
| Page assertions | Built-in (`hasURL`, `hasTitle`) | Must call `getCurrentUrl()`, `getTitle()` |
| Use case | Playwright-specific element/page checks | General Java assertions |

### When to use which:

```java
// Use Playwright assertThat() for:
assertThat(page.locator(".alert-success")).isVisible();         // Element state
assertThat(page).hasURL("**/#!/login");                        // Page URL/title
assertThat(page.locator("input")).hasValue("admin");           // Form values

// Use TestNG Assert for:
// General Java logic that has nothing to do with browser state
Assert.assertEquals(productList.size(), 5, "Should have 5 products");
Assert.assertNotNull(responseObject, "Response should not be null");
Assert.assertTrue(price > 0, "Price must be positive");

// Combined usage in the same test
@Test
public void verifyProductCount() {
    navigateTo("products");

    // Playwright assertion: wait for products to appear on page
    assertThat(page.locator(".product-card").first()).isVisible();

    // TestNG assertion: business logic check on Java data
    int count = page.locator(".product-card").count();
    Assert.assertTrue(count >= 1, "Should have at least 1 product");
    Assert.assertTrue(count <= 100, "Should not have more than 100 products");
}
```

---

## Complete Assertion Example: Full Page Verification

```java
package com.shopeasy.playwright.tests;

import com.shopeasy.playwright.base.BaseTest;
import com.microsoft.playwright.Locator;
import org.testng.Assert;
import org.testng.annotations.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class AssertionDemoTest extends BaseTest {

    @Test
    public void verifyLoginPage() {
        navigateTo("login");

        // Page-level assertions
        assertThat(page).hasTitle("ShopEasy - E-Commerce");
        assertThat(page).hasURL("**/#!/login");

        // Element visibility assertions
        assertThat(page.getByPlaceholder("Enter your username")).isVisible();
        assertThat(page.getByPlaceholder("Enter your password")).isVisible();
        assertThat(page.locator("button[type='submit']")).isVisible();

        // Text assertions
        assertThat(page.locator("button[type='submit']")).containsText("Login");

        // Error should NOT be visible on page load
        assertThat(page.locator(".alert-danger")).isHidden();

        System.out.println("Login page verified successfully");
    }

    @Test
    public void verifyProductsPage() {
        navigateTo("products");

        // Wait for products to load
        assertThat(page.locator(".product-card").first()).isVisible();

        // Count products
        int count = page.locator(".product-card").count();
        Assert.assertTrue(count > 0, "At least one product should be visible");
        System.out.println("Products count: " + count);

        // Every product card should have a name, price, and button
        Locator firstCard = page.locator(".product-card").first();
        assertThat(firstCard.locator("h5")).isVisible();
        assertThat(firstCard.locator(".text-success")).isVisible();
        assertThat(firstCard.locator("button.btn-primary")).isVisible();
        assertThat(firstCard.locator(".text-success")).containsText("$");
    }

    @Test
    public void softAssertionsForLoginForm() {
        navigateTo("login");

        var soft = PlaywrightAssertions.assertThatSoftly();

        soft.assertThat(page).hasTitle("ShopEasy - E-Commerce");
        soft.assertThat(page.getByPlaceholder("Enter your username")).isVisible();
        soft.assertThat(page.getByPlaceholder("Enter your password")).isVisible();
        soft.assertThat(page.locator("button[type='submit']")).isVisible();
        soft.assertThat(page.locator("button[type='submit']")).hasText("Login");
        soft.assertThat(page.locator(".alert-danger")).isHidden();

        // Report all failures at once
        soft.assertAll();
    }

    @Test
    public void verifyAdminDashboardAfterLogin() {
        navigateTo("login");
        page.getByPlaceholder("Enter your username").fill("admin");
        page.getByPlaceholder("Enter your password").fill("admin123");
        page.locator("button[type='submit']").click();

        // Wait for redirect
        assertThat(page).hasURL("**/#!/admin/dashboard");

        // Verify dashboard elements
        assertThat(page.locator(".card.shadow-sm").first()).isVisible();

        // Nav should show admin options
        assertThat(page.locator(".navbar")).containsText("Admin");

        // Logout option should be visible
        assertThat(page.getByText("Logout")).isVisible();
    }
}
```
