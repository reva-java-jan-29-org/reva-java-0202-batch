# 03 — Locators

## What is a Locator?

A `Locator` in Playwright is a **lazy reference** to one or more elements on the page. It is NOT evaluated immediately when you create it — it is evaluated at the moment you call an action or assertion on it.

```java
import com.microsoft.playwright.Locator;

// This does NOT find the element yet — just defines where to look
Locator loginButton = page.locator("button[type='submit']");

// The element is found NOW when you call an action
loginButton.click();              // Finds element, auto-waits for clickability, clicks
loginButton.isVisible();          // Finds element, returns boolean
```

**Benefit:** If the DOM re-renders (as AngularJS does frequently), the locator re-queries the DOM fresh on every use — no StaleElementReferenceException like in Selenium.

---

## Locator Strategies Overview

| Strategy | Method | Best For |
|---|---|---|
| CSS Selector | `page.locator("css=...")` or `page.locator("...")` | Most cases |
| XPath | `page.locator("xpath=...")` or `page.locator("//...")` | Complex traversal |
| Text content | `page.getByText("...")` | Buttons, labels, links with visible text |
| Role (ARIA) | `page.getByRole(AriaRole.BUTTON, ...)` | Semantic HTML elements |
| Label | `page.getByLabel("...")` | Form inputs with `<label>` |
| Placeholder | `page.getByPlaceholder("...")` | Inputs with placeholder text |
| Test ID | `page.getByTestId("...")` | Elements with `data-testid` attribute |
| Alt text | `page.getByAltText("...")` | Images |
| Title | `page.getByTitle("...")` | Elements with `title` attribute |

---

## 1. CSS Selectors

CSS selectors are the most common locating strategy. Playwright accepts CSS selectors directly without a prefix.

### Basic CSS selectors

```java
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;

// ── Tag selector ──────────────────────────────────────────────────
Locator allButtons = page.locator("button");

// ── Class selector ────────────────────────────────────────────────
Locator productCards = page.locator(".product-card");

// ── ID selector ───────────────────────────────────────────────────
Locator productModal = page.locator("#productModal");

// ── Attribute selector ────────────────────────────────────────────
// ShopEasy login: username input has no id — use placeholder attribute
Locator usernameInput = page.locator("input[placeholder='Enter your username']");

// Input by type
Locator passwordInput = page.locator("input[type='password']");

// Submit button
Locator loginButton   = page.locator("button[type='submit']");

// ── Attribute contains ────────────────────────────────────────────
// Select with ng-model containing 'Category'
Locator categoryDropdown = page.locator("select[ng-model*='Category']");

// ── Attribute starts with ─────────────────────────────────────────
Locator ngRepeat = page.locator("[ng-repeat^='product']");
```

### Combining CSS selectors — ShopEasy examples

```java
// Product name inside a product card
// .product-card h5 — h5 descendant of .product-card
Locator productName = page.locator(".product-card h5");

// Login form submit button — button inside form[name='loginForm']
Locator loginFormSubmit = page.locator("form[name='loginForm'] button[type='submit']");

// Navigation links in navbar
Locator navLinks = page.locator(".navbar-nav .nav-link");

// Admin table — all td cells in the products table body
Locator tableCells = page.locator("#productsTable tbody td");

// Alert message text
Locator successAlert = page.locator(".alert.alert-success");
Locator errorAlert   = page.locator(".alert.alert-danger");

// Spinner element
Locator spinner = page.locator(".spinner-border");

// Price text inside product card
Locator priceText = page.locator(".product-card .text-success");

// Add to cart button inside a product card
Locator addToCartBtn = page.locator(".product-card button.btn-primary");

// Search input on products page
Locator searchInput = page.locator("input[ng-model='searchQuery']");
```

### Pseudo-selectors

```java
// :nth-child — first product card
Locator firstCard = page.locator(".product-card:nth-child(1)");

// :last-child — last row in a table
Locator lastRow = page.locator("tbody tr:last-child");

// :not — all buttons that are NOT submit
Locator nonSubmitButtons = page.locator("button:not([type='submit'])");

// :has — product card that HAS a specific text (CSS :has() — modern browsers)
// Preferred Playwright approach: use filter() instead (see below)
```

---

## 2. XPath

Use XPath when CSS cannot express the needed relationship (e.g., find a parent given a child's text).

```java
// XPath prefix is "xpath=" or starts with "//"
// Playwright auto-detects "//" as XPath

// ── Basic XPath ────────────────────────────────────────────────────
// All input elements
Locator allInputs = page.locator("//input");

// ── Predicates ────────────────────────────────────────────────────
// Input with placeholder attribute
Locator usernameXPath = page.locator("//input[@placeholder='Enter your username']");

// Button with specific text
Locator loginBtnXPath = page.locator("//button[@type='submit']");

// ── contains() function ───────────────────────────────────────────
// Any element containing text "Login"
Locator loginText = page.locator("//button[contains(text(),'Login')]");

// ── Text exact match ──────────────────────────────────────────────
Locator exactLogin = page.locator("//button[text()='Login']");

// ── Find table row containing a product name ──────────────────────
// tr that has a descendant strong element with text "Laptop Pro"
Locator laptopRow = page.locator("//tr[.//strong[contains(text(),'Laptop Pro')]]");

// ── Parent traversal ──────────────────────────────────────────────
// Given a product price text, navigate up to its card container
Locator cardFromPrice = page.locator("//span[contains(@class,'text-success')]/ancestor::div[contains(@class,'product-card')]");

// ── Following sibling ─────────────────────────────────────────────
// td after a td with text "Laptop Pro"
Locator priceCell = page.locator("//td[contains(text(),'Laptop Pro')]/following-sibling::td[1]");
```

> **Tip:** Prefer CSS selectors when possible — they are easier to read and slightly faster. Use XPath when you need to navigate upward (ancestor/parent) or use complex text conditions.

---

## 3. getByText()

Finds elements by their **visible text content**.

```java
// Exact text match (case-sensitive by default)
Locator loginLink = page.getByText("Login");

// Exact text (explicit)
Locator registerLink = page.getByText("Register here", new Page.GetByTextOptions().setExact(true));

// Partial text match
Locator cartText = page.getByText("Shopping Cart");

// ShopEasy examples
page.getByText("Add to Cart").click();          // Click first "Add to Cart" button
page.getByText("Proceed to Checkout").click();  // Click checkout button
page.getByText("Logout").click();               // Click logout in navbar

// getByText returns the FIRST matching element
// Use .nth() or .filter() for multiple matches (see below)
```

---

## 4. getByRole()

Finds elements by their **ARIA role** — the semantic meaning of the element. This is the most recommended approach because it tests from the user's perspective.

```java
import com.microsoft.playwright.options.AriaRole;

// ── Buttons ───────────────────────────────────────────────────────
// Button with accessible name "Login"
Locator loginBtn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login"));

// Any submit button
Locator submitBtn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit"));

// ── Links ─────────────────────────────────────────────────────────
// Navigation link to Products
Locator productsLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Products"));

// ── Headings ──────────────────────────────────────────────────────
// h1 or any heading with text "ShopEasy"
Locator heading = page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("ShopEasy"));

// ── Textboxes ─────────────────────────────────────────────────────
// Input with label or accessible name "Username"
Locator usernameField = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username"));

// ── Checkbox ──────────────────────────────────────────────────────
Locator rememberMe = page.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName("Remember me"));

// ── Table navigation ──────────────────────────────────────────────
// Find a table row
Locator row = page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName("Laptop Pro"));
```

**Common ARIA roles:**

| Role | HTML Elements |
|---|---|
| `BUTTON` | `<button>`, `<input type="button">` |
| `LINK` | `<a>` |
| `TEXTBOX` | `<input type="text">`, `<textarea>` |
| `CHECKBOX` | `<input type="checkbox">` |
| `COMBOBOX` | `<select>` |
| `HEADING` | `<h1>` through `<h6>` |
| `TABLE` | `<table>` |
| `ROW` | `<tr>` |
| `CELL` | `<td>` |
| `LIST` | `<ul>`, `<ol>` |
| `LISTITEM` | `<li>` |
| `ALERT` | `<div role="alert">` |

---

## 5. getByLabel()

Finds form inputs associated with a `<label>` element (by `for` attribute or wrapping).

```java
// <label for="email">Email Address</label>
// <input id="email" type="email">
Locator emailInput = page.getByLabel("Email Address");

// ShopEasy register form (if labels are present)
Locator usernameField = page.getByLabel("Username");
Locator emailField    = page.getByLabel("Email");
Locator passwordField = page.getByLabel("Password");

emailField.fill("test@example.com");
```

> **Note:** ShopEasy's AngularJS forms may not have explicit `<label>` elements. Use `getByPlaceholder()` instead as a primary strategy for ShopEasy.

---

## 6. getByPlaceholder()

Finds form inputs by their `placeholder` attribute. **This is the most useful locator for ShopEasy** because AngularJS inputs rely on placeholder text rather than labels or IDs.

```java
// Login form
Locator usernameInput = page.getByPlaceholder("Enter your username");
Locator passwordInput = page.getByPlaceholder("Enter your password");

// Register form
Locator firstNameInput  = page.getByPlaceholder("Enter your first name");
Locator lastNameInput   = page.getByPlaceholder("Enter your last name");
Locator emailInput      = page.getByPlaceholder("Enter your email");
Locator regPasswordInput = page.getByPlaceholder("Create a password");

// Products search bar
Locator searchInput = page.getByPlaceholder("Search products...");

// Admin product modal
Locator productNameInput = page.getByPlaceholder("Product name");
Locator productPriceInput = page.getByPlaceholder("Price");
Locator productStockInput = page.getByPlaceholder("Stock quantity");

// Usage
usernameInput.fill("admin");
passwordInput.fill("admin123");
```

---

## 7. getByTestId()

Finds elements with a `data-testid` attribute — typically added by developers specifically for testing.

```java
// Element: <button data-testid="add-to-cart-btn">Add to Cart</button>
Locator addToCart = page.getByTestId("add-to-cart-btn");

// Element: <div data-testid="product-price">$999.99</div>
Locator price = page.getByTestId("product-price");
```

> ShopEasy does not use `data-testid` attributes by default. If you control the app source, adding them is best practice for test stability.

---

## 8. Chaining Locators

Narrow down a locator by searching within another locator's scope.

```java
// Get the first product card
Locator firstProductCard = page.locator(".product-card").first();

// Search for elements WITHIN that card only
Locator productTitle    = firstProductCard.locator("h5");
Locator productPrice    = firstProductCard.locator(".text-success");
Locator addToCartButton = firstProductCard.locator("button.btn-primary");

// Multi-level chaining
Locator adminTable = page.locator("#productsTable");
Locator tableBody  = adminTable.locator("tbody");
Locator firstRow   = tableBody.locator("tr").first();
Locator nameCell   = firstRow.locator("td").nth(1);  // Second cell (0-indexed)

System.out.println("First product name: " + nameCell.textContent());
```

---

## 9. nth(), first(), last()

Select a specific element from a list.

```java
// All product cards on the page
Locator allCards = page.locator(".product-card");

// First card (index 0)
Locator firstCard = allCards.first();
// Same as:
Locator firstCardAlt = allCards.nth(0);

// Last card
Locator lastCard = allCards.last();

// Third card (index 2)
Locator thirdCard = allCards.nth(2);

// Count how many there are
int totalProducts = allCards.count();
System.out.println("Total products: " + totalProducts);

// Admin orders table — click the first row's "View" button
page.locator("tbody tr").first().locator("button.btn-info").click();
```

---

## 10. filter()

Filter a list of locators by additional conditions — text content, visibility, or nested elements.

```java
// All product cards
Locator allCards = page.locator(".product-card");

// Filter to cards that contain "Electronics" category text
Locator electronicsCards = allCards.filter(
    new Locator.FilterOptions().setHasText("Electronics")
);

// Filter to cards that have an "Add to Cart" button (not "Out of Stock")
Locator availableCards = allCards.filter(
    new Locator.FilterOptions().setHas(page.locator("button:has-text('Add to Cart')"))
);

// Click "Add to Cart" on the product named "Laptop Pro"
page.locator(".product-card")
    .filter(new Locator.FilterOptions().setHasText("Laptop Pro"))
    .locator("button.btn-primary")
    .click();

// Get price of a specific product
String price = page.locator(".product-card")
    .filter(new Locator.FilterOptions().setHasText("Laptop Pro"))
    .locator(".text-success")
    .textContent();

System.out.println("Laptop Pro price: " + price);

// Admin table — find and click the edit button for a specific product
page.locator("tbody tr")
    .filter(new Locator.FilterOptions().setHasText("Wireless Mouse"))
    .locator("button.btn-warning")   // Edit button
    .click();
```

---

## 11. Locator.all()

Convert a locator to a `List<Locator>` to iterate over matching elements.

```java
import java.util.List;

// Get all product names
List<Locator> productCards = page.locator(".product-card").all();
System.out.println("Total products: " + productCards.size());

for (Locator card : productCards) {
    String name  = card.locator("h5").textContent();
    String price = card.locator(".text-success").textContent();
    System.out.println(name + " — " + price);
}

// Get all navigation links
List<Locator> navLinks = page.locator(".navbar-nav .nav-link").all();
for (Locator link : navLinks) {
    System.out.println("Nav link: " + link.textContent());
}
```

---

## 12. Locator Strict Mode

By default, if a locator matches **more than one element** and you call an action on it (like `.click()`), Playwright throws an error. This is called **strict mode** — it prevents accidentally acting on the wrong element.

```java
// This WILL throw if more than one button[type='submit'] exists on the page
page.locator("button[type='submit']").click();  // Error: strict mode violation

// Solutions:
// 1. Use .first()
page.locator("button[type='submit']").first().click();

// 2. Use .nth()
page.locator("button[type='submit']").nth(0).click();

// 3. Use a more specific selector
page.locator("form#loginForm button[type='submit']").click();

// 4. Use filter()
page.locator("button[type='submit']")
    .filter(new Locator.FilterOptions().setHasText("Login"))
    .click();
```

---

## Complete Example: Locating All ShopEasy Elements

```java
package com.shopeasy.playwright.tests;

import com.shopeasy.playwright.base.BaseTest;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import org.testng.annotations.Test;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class LocatorDemoTest extends BaseTest {

    @Test
    public void loginPageLocators() {
        navigateTo("login");

        // Using getByPlaceholder — best for ShopEasy AngularJS inputs
        Locator username = page.getByPlaceholder("Enter your username");
        Locator password = page.getByPlaceholder("Enter your password");

        // Using CSS attribute selector
        Locator loginBtn = page.locator("button[type='submit']");

        // Using getByRole
        Locator registerLink = page.getByRole(AriaRole.LINK,
            new Page.GetByRoleOptions().setName("Register here"));

        // Assert all are visible (Playwright auto-waits)
        assertThat(username).isVisible();
        assertThat(password).isVisible();
        assertThat(loginBtn).isVisible();
        assertThat(registerLink).isVisible();
    }

    @Test
    public void productsPageLocators() {
        navigateTo("products");

        // Search input
        Locator searchInput = page.getByPlaceholder("Search products...");

        // Category dropdown using CSS ng-model attribute
        Locator categoryDropdown = page.locator("select[ng-model='selectedCategory']");

        // All product cards — wait for at least 1 to appear
        Locator productCards = page.locator(".product-card");
        assertThat(productCards.first()).isVisible();

        int count = productCards.count();
        System.out.println("Products visible: " + count);

        // Get all product names
        List<Locator> cards = productCards.all();
        for (Locator card : cards) {
            String name  = card.locator("h5").textContent().trim();
            String price = card.locator(".text-success").textContent().trim();
            System.out.printf("%-30s %s%n", name, price);
        }
    }

    @Test
    public void filterProductByName() {
        navigateTo("products");

        // Wait for products to load
        assertThat(page.locator(".product-card").first()).isVisible();

        // Find the specific product card for "Laptop Pro"
        Locator laptopCard = page.locator(".product-card")
            .filter(new Locator.FilterOptions().setHasText("Laptop Pro"));

        assertThat(laptopCard).isVisible();

        // Get price
        String price = laptopCard.locator(".text-success").textContent();
        System.out.println("Laptop Pro price: " + price);

        // Click its Add to Cart button
        laptopCard.locator("button.btn-primary").click();
    }
}
```
