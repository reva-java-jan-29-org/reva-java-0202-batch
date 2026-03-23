# 02 — Selenium Locators: ID, Name, LinkText, Partial LinkText, TagName

## What Are Locators?

A **locator** tells Selenium how to find an HTML element on the page. Selenium provides the `By` class with multiple strategies. Choosing the right locator is the most critical skill in Selenium — a bad locator makes tests brittle and slow.

**The `By` class strategies:**

| Strategy | Method | Best For |
|---|---|---|
| ID | `By.id("value")` | Elements with unique `id` attribute |
| Name | `By.name("value")` | Form inputs with `name` attribute |
| Link Text | `By.linkText("Exact Text")` | Anchor `<a>` tags with exact visible text |
| Partial Link Text | `By.partialLinkText("Partial")` | Anchor `<a>` tags with partial visible text |
| Tag Name | `By.tagName("input")` | Finding by HTML element type |
| CSS Selector | `By.cssSelector("...")` | Powerful, recommended primary strategy |
| XPath | `By.xpath("...")` | Most flexible, traverse DOM tree |

---

## 1. `By.id()` — Fastest Locator

IDs are unique per page. Selenium finds them instantly using the browser's built-in `getElementById`.

**Syntax:**
```java
driver.findElement(By.id("elementId"));
```

**In ShopEasy — where IDs exist:**

The ShopEasy app has few IDs on inputs (AngularJS pattern), but some structural elements do have IDs:

```html
<!-- Navbar collapse — in index.html -->
<div class="collapse navbar-collapse" id="navbarNav">

<!-- Product modal — in admin-products.html -->
<div class="modal fade" id="productModal" tabindex="-1">
```

```java
// Example: find the navbar collapse div
WebElement navbar = driver.findElement(By.id("navbarNav"));

// Example: find the product modal
WebElement productModal = driver.findElement(By.id("productModal"));
```

**Why ShopEasy inputs don't have IDs:**

AngularJS 1.x uses `ng-model` for data binding and relies on class-based styling from Bootstrap. Developers often skip explicit IDs when the framework handles the binding. This is why you'll use CSS and XPath selectors primarily.

---

## 2. `By.name()` — Form-Level Locator

The `name` attribute is common on HTML form elements (especially traditional server-side forms). AngularJS uses `name` on the `<form>` element itself, not on individual inputs.

**Syntax:**
```java
driver.findElement(By.name("elementName"));
```

**In ShopEasy:**

```html
<!-- Login form has name="loginForm" -->
<form name="loginForm" ng-submit="login()" novalidate>

<!-- Register form has name="registerForm" -->
<form name="registerForm" ng-submit="register()" novalidate>
```

```java
// Find the login form by its name attribute
WebElement loginForm = driver.findElement(By.name("loginForm"));

// Find the register form
WebElement registerForm = driver.findElement(By.name("registerForm"));

// You can then find elements WITHIN the form using findElement on the parent
WebElement usernameInput = loginForm.findElement(By.cssSelector("input[type='text']"));
```

> **Tip:** Scoped search — call `findElement()` on a `WebElement` (not just `driver`) to search within that element's subtree. This narrows the search and improves reliability.

---

## 3. `By.linkText()` — Exact Anchor Text

Finds `<a>` (anchor) elements by their **exact visible text**. Case-sensitive. Fails if there's a leading/trailing space or icon text included.

**Syntax:**
```java
driver.findElement(By.linkText("Exact Text Here"));
```

**In ShopEasy:**

```html
<!-- Register link on login page -->
<a href="#!/register" class="text-primary fw-semibold">Register here</a>

<!-- Login link on register page -->
<a href="#!/login" class="text-primary fw-semibold">Login here</a>
```

```java
// Navigate to login page
navigateTo("login");

// Click the "Register here" link — exact text match
driver.findElement(By.linkText("Register here")).click();

// Verify we are now on the register page
Assert.assertTrue(driver.getCurrentUrl().contains("/register"));
```

**Common mistake — when linkText FAILS:**

```html
<!-- This link has an icon inside it -->
<a class="nav-link" href="#!/products">
    <i class="bi bi-grid"></i> Products
</a>
```

`By.linkText("Products")` will FAIL because the actual text includes the icon's text content.
Use `By.partialLinkText` or CSS selector instead for these.

---

## 4. `By.partialLinkText()` — Partial Anchor Text

Finds `<a>` elements whose visible text **contains** the given substring. Case-sensitive.

**Syntax:**
```java
driver.findElement(By.partialLinkText("Partial"));
```

**In ShopEasy:**

```html
<!-- Navbar brand -->
<a class="navbar-brand fw-bold" href="#!/products">
    <i class="bi bi-shop"></i> ShopEasy
</a>

<!-- Guest nav links -->
<a class="nav-link" href="#!/login">
    <i class="bi bi-box-arrow-in-right"></i> Login
</a>

<!-- Footer or nav links with partial text -->
<a href="#!/register">Register here</a>
```

```java
navigateTo("login");

// Find using partial text — "Register" is part of "Register here"
WebElement registerLink = driver.findElement(By.partialLinkText("Register"));
registerLink.click();

// Find the ShopEasy brand link using partial text
WebElement brandLink = driver.findElement(By.partialLinkText("ShopEasy"));
brandLink.click();
```

**Difference between linkText and partialLinkText:**

| | `linkText` | `partialLinkText` |
|---|---|---|
| Match type | Exact full match | Substring match |
| Text: "Register here" | `linkText("Register here")` ✓ | `partialLinkText("Register")` ✓ |
| Text: "Register" | `linkText("Register here")` ✗ | `partialLinkText("Register")` ✓ |

---

## 5. `By.tagName()` — Find by HTML Tag

Finds elements by their HTML tag name. Useful for finding collections of similar elements (all links, all inputs, all rows).

**Syntax:**
```java
driver.findElement(By.tagName("input"));         // first input on page
driver.findElements(By.tagName("a"));            // ALL links on page (returns List)
```

**In ShopEasy:**

```java
navigateTo("login");

// Get ALL input elements on the login page
List<WebElement> inputs = driver.findElements(By.tagName("input"));
System.out.println("Number of inputs on login page: " + inputs.size()); // 2

// Get ALL links on the login page
List<WebElement> links = driver.findElements(By.tagName("a"));
System.out.println("Number of links: " + links.size());

// Get the page's single <form> element
WebElement form = driver.findElement(By.tagName("form"));
System.out.println("Form name: " + form.getAttribute("name")); // "loginForm"

// Navigate to admin products page and count table rows
navigateTo("admin/products");
List<WebElement> rows = driver.findElements(By.tagName("tr"));
System.out.println("Total rows (including header): " + rows.size());
```

**When to use `findElement` vs `findElements`:**

| Method | Returns | Throws if not found |
|---|---|---|
| `findElement(By)` | Single `WebElement` | `NoSuchElementException` |
| `findElements(By)` | `List<WebElement>` (empty if none) | Never throws — returns empty list |

---

## Complete Example: Login Test Using Basic Locators

```java
package com.shopeasy.tests;

import com.shopeasy.tests.base.BaseTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class BasicLocatorsTest extends BaseTest {

    @Test
    public void testLoginPageElements() {
        navigateTo("login");

        // ── By.name (form level) ──────────────────────────────────────
        WebElement loginForm = driver.findElement(By.name("loginForm"));
        Assert.assertTrue(loginForm.isDisplayed(), "Login form not found by name");

        // ── By.tagName (scoped within form) ──────────────────────────
        List<WebElement> inputs = loginForm.findElements(By.tagName("input"));
        Assert.assertEquals(inputs.size(), 2, "Expected 2 inputs in login form");

        // ── By.linkText ───────────────────────────────────────────────
        WebElement registerLink = driver.findElement(By.linkText("Register here"));
        Assert.assertTrue(registerLink.isDisplayed());

        // ── By.partialLinkText ────────────────────────────────────────
        WebElement partialLink = driver.findElement(By.partialLinkText("Register"));
        Assert.assertEquals(partialLink.getText(), "Register here");

        // ── By.tagName (all links on page) ───────────────────────────
        List<WebElement> allLinks = driver.findElements(By.tagName("a"));
        System.out.println("Total links on login page: " + allLinks.size());

        // Click Register here → navigates to register page
        registerLink.click();
        Assert.assertTrue(driver.getCurrentUrl().contains("register"));

        // ── By.id (navbar has an id) ──────────────────────────────────
        // (navbar is present in the SPA, doesn't require a specific route)
        WebElement navbar = driver.findElement(By.id("navbarNav"));
        Assert.assertTrue(navbar.isDisplayed());
    }
}
```

---

## Locator Selection Priority (Best Practice)

```
1. By.id          — fastest, most reliable (when available)
2. By.name        — good for form elements
3. By.cssSelector — fast, readable, handles complex cases
4. By.linkText    — only for anchor text (exact)
5. By.xpath       — most powerful, use when CSS is insufficient
6. By.tagName     — use for collecting groups of elements
7. By.partialLinkText — only when exact text is unpredictable
```

> **Never use absolute XPath** like `/html/body/div[3]/div/form/div[1]/input` — it breaks with any page change.
