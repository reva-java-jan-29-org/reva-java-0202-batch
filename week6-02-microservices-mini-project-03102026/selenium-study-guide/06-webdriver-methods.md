# 06 — WebDriver Methods: Get, Conditional & Browser Methods

## Overview

After finding elements with locators, you interact with them using WebDriver methods. These fall into three categories:

1. **Get methods** — Read information from the browser or elements
2. **Conditional methods** — Check state of elements (boolean returns)
3. **Browser/manage methods** — Control the browser window, cookies, timeouts

---

## 1. Get Methods — Reading Information

### `driver.getTitle()`

Returns the current page title (what appears in the browser tab).

```java
driver.get(BASE_URL + "/#!/login");
String title = driver.getTitle();
System.out.println(title);  // "ShopEasy - E-Commerce"

Assert.assertEquals(title, "ShopEasy - E-Commerce");
```

> ShopEasy is a SPA — the title stays the same across all routes (`#!/login`, `#!/products`, etc.). Title verification confirms the app loaded.

---

### `driver.getCurrentUrl()`

Returns the full URL currently in the browser's address bar.

```java
driver.get(BASE_URL + "/#!/login");
String url = driver.getCurrentUrl();
System.out.println(url);
// "http://localhost:4200/#!/login"

// After login as admin, verify redirect
driver.findElement(By.cssSelector("[ng-model='loginData.username']")).sendKeys("admin");
driver.findElement(By.cssSelector("[ng-model='loginData.password']")).sendKeys("admin123");
driver.findElement(By.cssSelector("button[type='submit']")).click();

// Wait for redirect (with explicit wait — see 07-waits.md)
Assert.assertTrue(driver.getCurrentUrl().contains("admin/dashboard"),
    "Should redirect to admin dashboard after login");
```

---

### `driver.getPageSource()`

Returns the full HTML source of the current page as a String. Useful for verifying content or debugging.

```java
driver.get(BASE_URL + "/#!/products");
String pageSource = driver.getPageSource();

// Check if "ShopEasy" appears in the source
Assert.assertTrue(pageSource.contains("ShopEasy"));

// Check if a product category text appears in the rendered page
Assert.assertTrue(pageSource.contains("Electronics"));
```

> **AngularJS note:** `getPageSource()` returns the HTML including AngularJS-rendered content (since the page is already rendered in the DOM). It does NOT return the original static template — it returns what Chrome sees after Angular processes `ng-repeat`, `ng-show`, etc.

---

### `element.getText()`

Returns the visible text content of an element.

```java
navigateTo("login");

// Get text of the Sign In card header
WebElement cardHeader = driver.findElement(
    By.cssSelector(".card-header h4")
);
System.out.println(cardHeader.getText()); // "Sign In"

// Get text of the register link
WebElement registerLink = driver.findElement(By.linkText("Register here"));
System.out.println(registerLink.getText()); // "Register here"

// Get all product names on the products page
navigateTo("products");
List<WebElement> titles = driver.findElements(
    By.cssSelector("h6.card-title.fw-bold")
);
for (WebElement title : titles) {
    System.out.println(title.getText());
}
```

**`getText()` vs `getAttribute("innerText")`:**
- `getText()` — returns visible text only (respects CSS `display:none`)
- `getAttribute("innerText")` — similar but may include hidden text
- `getAttribute("textContent")` — returns ALL text including hidden

---

### `element.getAttribute(attributeName)`

Returns the value of any HTML attribute of an element.

```java
navigateTo("login");

WebElement usernameInput = driver.findElement(
    By.cssSelector("[ng-model='loginData.username']")
);

// Get attribute values
System.out.println(usernameInput.getAttribute("placeholder")); // "Enter your username"
System.out.println(usernameInput.getAttribute("type"));        // "text"
System.out.println(usernameInput.getAttribute("class"));       // "form-control"
System.out.println(usernameInput.getAttribute("ng-model"));    // "loginData.username"
System.out.println(usernameInput.getAttribute("required"));    // "true"

// For href on a link
WebElement registerLink = driver.findElement(By.linkText("Register here"));
System.out.println(registerLink.getAttribute("href")); // "http://localhost:4200/#!/register"

// Get value of an input field (what was typed in)
usernameInput.sendKeys("john");
System.out.println(usernameInput.getAttribute("value")); // "john"
```

---

### `element.getTagName()`

Returns the HTML tag name in lowercase.

```java
WebElement form = driver.findElement(By.name("loginForm"));
System.out.println(form.getTagName()); // "form"

WebElement input = driver.findElement(By.cssSelector("[ng-model='loginData.username']"));
System.out.println(input.getTagName()); // "input"

WebElement btn = driver.findElement(By.cssSelector("button[type='submit']"));
System.out.println(btn.getTagName()); // "button"
```

---

### `element.getCssValue(propertyName)`

Returns the computed CSS value of a CSS property.

```java
WebElement loginBtn = driver.findElement(By.cssSelector("button[type='submit']"));

// Get computed CSS properties
System.out.println(loginBtn.getCssValue("background-color")); // "rgba(13, 110, 253, 1)" (Bootstrap primary blue)
System.out.println(loginBtn.getCssValue("color"));            // "rgba(255, 255, 255, 1)"
System.out.println(loginBtn.getCssValue("font-size"));        // "16px"

// Useful for verifying styling:
String bgColor = loginBtn.getCssValue("background-color");
Assert.assertTrue(bgColor.contains("13, 110, 253"), "Login button should be blue");
```

---

### `element.getSize()` and `element.getLocation()`

```java
WebElement card = driver.findElement(By.cssSelector(".card.shadow-sm"));

// Size: width and height in pixels
org.openqa.selenium.Dimension size = card.getSize();
System.out.println("Width: " + size.getWidth());
System.out.println("Height: " + size.getHeight());

// Location: x, y coordinates from top-left of viewport
org.openqa.selenium.Point location = card.getLocation();
System.out.println("X: " + location.getX());
System.out.println("Y: " + location.getY());
```

---

## 2. Conditional Methods — Checking State

These return `boolean` and never throw an exception when the element is in the wrong state — they return `false` instead.

### `element.isDisplayed()`

Returns `true` if the element is visible on screen (not hidden via CSS `display:none` or `visibility:hidden`).

```java
navigateTo("login");

// Error alert — hidden initially (ng-show="error")
WebElement errorAlert = driver.findElement(By.cssSelector(".alert-danger"));
Assert.assertFalse(errorAlert.isDisplayed(), "Error should not be visible before attempting login");

// Login button should be visible
WebElement loginBtn = driver.findElement(By.cssSelector("button[type='submit']"));
Assert.assertTrue(loginBtn.isDisplayed(), "Login button should be visible");

// Loading spinner — hidden initially (ng-show="loading")
WebElement spinner = driver.findElement(By.cssSelector(".spinner-border"));
Assert.assertFalse(spinner.isDisplayed(), "Spinner should not show before clicking login");
```

> **AngularJS + isDisplayed():** AngularJS uses `ng-show` which toggles CSS `display:none`. Elements with `ng-show="false"` are in the DOM but `isDisplayed()` returns `false`. Elements with `ng-if="false"` are NOT in the DOM at all — `findElement` throws `NoSuchElementException`.

---

### `element.isEnabled()`

Returns `true` if the element is interactable (not disabled).

```java
navigateTo("login");

// Login button starts disabled (ng-disabled="loading || loginForm.$invalid")
// When form is empty and required fields are blank, the button is disabled
WebElement loginBtn = driver.findElement(By.cssSelector("button[type='submit']"));
// The button's ng-disabled evaluates to true initially
// In AngularJS, ng-disabled adds the HTML "disabled" attribute
System.out.println("isEnabled before filling form: " + loginBtn.isEnabled());
// Usually false when form is $invalid

// Fill in required fields
driver.findElement(By.cssSelector("[ng-model='loginData.username']")).sendKeys("test");
driver.findElement(By.cssSelector("[ng-model='loginData.password']")).sendKeys("pass");

// Now the form is valid, button should be enabled
Assert.assertTrue(loginBtn.isEnabled(), "Login button should be enabled after filling form");
```

---

### `element.isSelected()`

Returns `true` if a checkbox, radio button, or dropdown option is selected/checked.

```java
// Example with a checkbox (generic — not present in ShopEasy by default)
// WebElement checkbox = driver.findElement(By.cssSelector("input[type='checkbox']"));
// boolean isChecked = checkbox.isSelected();

// Category dropdown option — check if "All Categories" is selected by default
navigateTo("products");
WebElement categorySelect = driver.findElement(By.cssSelector("[ng-model='selectedCategory']"));
// Note: Use Select class for dropdown interaction — see 11-dropdowns.md
```

---

## 3. Browser Management Methods

### `driver.manage().window().maximize()`

```java
driver.manage().window().maximize();       // Full screen
driver.manage().window().minimize();       // Minimize
driver.manage().window().fullscreen();     // True fullscreen (hides OS taskbar)

// Set specific size
driver.manage().window().setSize(new org.openqa.selenium.Dimension(1366, 768));
```

---

### `driver.manage().timeouts()`

```java
// Implicit wait — applies to all findElement calls
driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

// Page load timeout — max time for a page to load
driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));

// Script timeout — max time for JavascriptExecutor to finish
driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(15));
```

---

### `driver.manage().getCookies()` — Cookie Management

```java
// ShopEasy uses localStorage for JWT, but cookies work similarly conceptually

// Get all cookies
Set<Cookie> cookies = driver.manage().getCookies();
for (Cookie cookie : cookies) {
    System.out.println(cookie.getName() + " = " + cookie.getValue());
}

// Get a specific cookie by name
Cookie sessionCookie = driver.manage().getCookieNamed("JSESSIONID");

// Add a cookie
Cookie myCookie = new Cookie("testCookie", "testValue");
driver.manage().addCookie(myCookie);

// Delete a specific cookie
driver.manage().deleteCookieNamed("testCookie");

// Delete all cookies (useful for logout simulation in tests)
driver.manage().deleteAllCookies();
```

---

## Complete Example: ShopEasy Page State Verification Test

```java
package com.shopeasy.tests;

import com.shopeasy.tests.base.BaseTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class WebDriverMethodsTest extends BaseTest {

    @Test
    public void verifyLoginPageState() {
        navigateTo("login");

        // ── Get Methods ──────────────────────────────────────────────
        Assert.assertEquals(driver.getTitle(), "ShopEasy - E-Commerce");
        Assert.assertTrue(driver.getCurrentUrl().contains("#!/login"));
        Assert.assertTrue(driver.getPageSource().contains("Sign In"));

        WebElement cardHeader = driver.findElement(By.cssSelector(".card-header h4"));
        Assert.assertEquals(cardHeader.getText().trim(), "Sign In");

        WebElement usernameInput = driver.findElement(
            By.cssSelector("[ng-model='loginData.username']")
        );
        Assert.assertEquals(usernameInput.getAttribute("placeholder"), "Enter your username");
        Assert.assertEquals(usernameInput.getTagName(), "input");

        // ── Conditional Methods ──────────────────────────────────────
        Assert.assertTrue(usernameInput.isDisplayed(), "Username input should be visible");
        Assert.assertTrue(usernameInput.isEnabled(), "Username input should be editable");

        // Error alert is hidden before any attempt
        WebElement errorDiv = driver.findElement(By.cssSelector(".alert-danger"));
        Assert.assertFalse(errorDiv.isDisplayed(), "Error alert should be hidden initially");

        // ── CSS Value ────────────────────────────────────────────────
        WebElement cardHeaderDiv = driver.findElement(By.cssSelector(".card-header"));
        String bgColor = cardHeaderDiv.getCssValue("background-color");
        System.out.println("Card header background: " + bgColor);

        // ── All links on page ────────────────────────────────────────
        List<WebElement> links = driver.findElements(By.tagName("a"));
        System.out.println("Links on login page: " + links.size());
        for (WebElement link : links) {
            System.out.println("  " + link.getText() + " → " + link.getAttribute("href"));
        }
    }

    @Test
    public void verifyProductsPageLoaded() {
        navigateTo("products");

        // Wait for AngularJS to render products (implicit wait handles this)
        List<WebElement> productCards = driver.findElements(
            By.cssSelector(".product-card")
        );

        System.out.println("Product count: " + productCards.size());
        Assert.assertTrue(productCards.size() > 0, "Products page should show products");

        // Verify first product card has a title
        WebElement firstTitle = productCards.get(0).findElement(
            By.cssSelector("h6.card-title")
        );
        String titleText = firstTitle.getText();
        System.out.println("First product: " + titleText);
        Assert.assertFalse(titleText.isEmpty(), "Product title should not be empty");
    }
}
```
