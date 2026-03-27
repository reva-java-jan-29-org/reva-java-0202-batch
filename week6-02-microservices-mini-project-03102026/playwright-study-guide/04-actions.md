# 04 — Actions

## What are Actions?

Actions in Playwright are methods that **interact with elements** on the page — clicking, typing, selecting, hovering, and more. Every action automatically:

1. Waits for the element to exist in the DOM
2. Waits for the element to be visible
3. Waits for the element to be enabled
4. Scrolls the element into view if needed
5. Performs the action

This built-in **actionability check** means you rarely need manual `wait` calls before actions — unlike Selenium.

---

## click()

Clicks an element. Simulates a real mouse click including mouse-down, mouse-up events.

```java
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;

// Simple click
page.locator("button[type='submit']").click();

// Click a navigation link
page.locator("a[href='#!/products']").click();

// Click using getByRole
page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

// Click using getByText
page.getByText("Add to Cart").click();

// Click with options
page.locator("button.btn-primary").click(
    new Locator.ClickOptions()
        .setButton(MouseButton.RIGHT)   // Right-click
        .setDelay(100)                  // Hold click for 100ms
        .setPosition(10, 5)             // Click at specific coordinates within element
);

// Force click — bypasses actionability checks (use sparingly)
page.locator(".hidden-button").click(new Locator.ClickOptions().setForce(true));
```

---

## fill()

Clears any existing value and types new text into an input. This is the **primary way to type** in Playwright.

```java
// Fill username field
page.getByPlaceholder("Enter your username").fill("admin");

// Fill password field
page.locator("input[type='password']").fill("admin123");

// Fill search field
page.getByPlaceholder("Search products...").fill("laptop");

// Fill email
page.getByPlaceholder("Enter your email").fill("test@example.com");

// Clear a field (fill with empty string)
page.getByPlaceholder("Enter your username").fill("");

// fill() vs type() difference:
// fill() — sets the entire value at once (fast, recommended for most inputs)
// type() — simulates key presses one at a time (use for autocomplete/suggestion boxes)
```

---

## type()

Simulates real key-by-key typing. Use when the app listens to individual `keydown`/`keyup` events (like autocomplete inputs).

```java
// Type character by character — triggers keydown/keyup/keypress events
page.getByPlaceholder("Search products...").type("laptop");

// Type with a delay between characters (visible typing effect)
page.getByPlaceholder("Search products...").type("laptop",
    new Locator.TypeOptions().setDelay(50)  // 50ms between each character
);

// For most ShopEasy inputs, fill() is sufficient
// Use type() only if fill() doesn't trigger the AngularJS ng-model update
```

---

## clear()

Removes the current value from an input field.

```java
// Clear the search field
page.getByPlaceholder("Search products...").clear();

// After clearing, fill with new value
page.getByPlaceholder("Enter your username").clear();
page.getByPlaceholder("Enter your username").fill("newusername");

// Alternative: fill("") also clears the field
page.getByPlaceholder("Enter your username").fill("");
```

---

## selectOption()

Selects an option from a `<select>` dropdown.

```java
// Select by visible text
page.locator("select[ng-model='selectedCategory']").selectOption("Electronics");

// Select by value attribute
page.locator("select[ng-model='selectedCategory']").selectOption(
    new SelectOption().setValue("electronics")
);

// Select by index (0-based)
page.locator("select[ng-model='selectedCategory']").selectOption(
    new SelectOption().setIndex(1)
);

// Select multiple options (for multi-select)
page.locator("select[multiple]").selectOption(new String[]{"option1", "option2"});

// Read the currently selected option text
String selectedCategory = page.locator("select[ng-model='selectedCategory']")
    .inputValue();
System.out.println("Selected category: " + selectedCategory);
```

---

## check() and uncheck()

For checkboxes and radio buttons.

```java
// Check a checkbox
page.locator("input[type='checkbox']").check();

// Uncheck a checkbox
page.locator("input[type='checkbox']").uncheck();

// Select a radio button (check works for radio buttons too)
page.locator("input[type='radio'][value='credit']").check();

// Verify state
boolean isChecked = page.locator("input[type='checkbox']").isChecked();
System.out.println("Checkbox checked: " + isChecked);
```

---

## hover()

Moves the mouse over an element without clicking — useful for dropdown menus or tooltips.

```java
// Hover over the user menu to reveal dropdown
page.locator(".navbar-nav .dropdown-toggle").hover();

// After hover, dropdown menu appears — click an option
page.locator(".dropdown-menu .dropdown-item").first().click();

// Hover over a product card to reveal quick-view button
page.locator(".product-card").first().hover();
```

---

## dblclick()

Double-clicks an element.

```java
// Double-click a cell in an admin table to inline-edit
page.locator("tbody tr").first().locator("td.editable").dblclick();

// Double-click with position
page.locator(".canvas-area").dblclick(
    new Locator.DblclickOptions().setPosition(100, 200)
);
```

---

## press()

Presses a keyboard key while the element has focus.

```java
// Press Enter to submit the login form (instead of clicking submit button)
page.getByPlaceholder("Enter your password").press("Enter");

// Press Tab to move to the next field
page.getByPlaceholder("Enter your username").press("Tab");

// Press Escape to close a modal
page.locator("body").press("Escape");

// Press Ctrl+A to select all text
page.getByPlaceholder("Search products...").press("Control+a");

// Press Delete or Backspace
page.locator("input").press("Delete");
page.locator("input").press("Backspace");

// Common key names:
// "Enter", "Tab", "Escape", "Backspace", "Delete"
// "ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight"
// "Control+a", "Control+c", "Control+v", "Control+z"
// "Shift+Tab", "Alt+F4"
// "F5" (refresh), "F12" (devtools)
```

---

## keyboard.press() — Global Keyboard Shortcuts

```java
// Press a key without focusing a specific element
page.keyboard().press("Escape");
page.keyboard().press("Control+Shift+I");  // Open DevTools (in non-headless)

// Type text programmatically at current focus
page.keyboard().type("Hello ShopEasy");

// Hold modifier key
page.keyboard().down("Shift");
page.keyboard().press("ArrowDown");  // Shift+ArrowDown
page.keyboard().up("Shift");
```

---

## dragTo()

Drags an element and drops it onto another element.

```java
// Drag source element to target element
Locator source = page.locator("#drag-item");
Locator target = page.locator("#drop-zone");
source.dragTo(target);

// Drag to specific coordinates
page.locator(".draggable-item").dragTo(page.locator(".droppable-zone"),
    new Locator.DragToOptions()
        .setSourcePosition(10, 10)  // Grab point within source element
        .setTargetPosition(50, 50)  // Drop point within target element
);
```

---

## scrollIntoViewIfNeeded()

Scrolls the element into the visible area of the viewport.

```java
// Scroll to the footer
page.locator("footer").scrollIntoViewIfNeeded();

// Scroll to the last product card
page.locator(".product-card").last().scrollIntoViewIfNeeded();

// Note: Most Playwright actions auto-scroll anyway.
// Use this explicitly when you want to verify element is visible before a screenshot.
```

---

## Full Example: Login Flow on ShopEasy

```java
package com.shopeasy.playwright.tests;

import com.shopeasy.playwright.base.BaseTest;
import com.microsoft.playwright.Locator;
import org.testng.annotations.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class LoginTest extends BaseTest {

    @Test
    public void successfulAdminLogin() {
        // Step 1: Navigate to login page
        navigateTo("login");

        // Step 2: Fill username — Playwright auto-waits for element
        page.getByPlaceholder("Enter your username").fill("admin");

        // Step 3: Fill password
        page.getByPlaceholder("Enter your password").fill("admin123");

        // Step 4: Click login button
        page.locator("button[type='submit']").click();

        // Step 5: Wait for redirect and verify URL contains admin/dashboard
        page.waitForURL("**/#!/admin/dashboard");

        // Step 6: Assert we are on admin dashboard
        assertThat(page).hasURL(
            org.testng.Assert.class,  // not needed — just assertThat
            "http://localhost:4200/#!/admin/dashboard"
        );
        // Simpler form:
        assertThat(page).hasURL("http://localhost:4200/#!/admin/dashboard");
    }

    @Test
    public void loginWithEnterKey() {
        navigateTo("login");

        page.getByPlaceholder("Enter your username").fill("admin");
        page.getByPlaceholder("Enter your password").fill("admin123");

        // Press Enter instead of clicking the button
        page.getByPlaceholder("Enter your password").press("Enter");

        // Verify navigation happened
        page.waitForURL("**/#!/admin/dashboard");
        assertThat(page).hasURL("http://localhost:4200/#!/admin/dashboard");
    }

    @Test
    public void invalidLoginShowsError() {
        navigateTo("login");

        page.getByPlaceholder("Enter your username").fill("wronguser");
        page.getByPlaceholder("Enter your password").fill("wrongpass");
        page.locator("button[type='submit']").click();

        // Error alert should appear — Playwright auto-waits for it to be visible
        Locator errorAlert = page.locator(".alert-danger");
        assertThat(errorAlert).isVisible();
        assertThat(errorAlert).containsText("Invalid");
    }

    @Test
    public void loginFormFieldsAreClearable() {
        navigateTo("login");

        Locator username = page.getByPlaceholder("Enter your username");
        username.fill("testuser");

        // Verify text was typed
        assertThat(username).hasValue("testuser");

        // Clear and verify
        username.clear();
        assertThat(username).hasValue("");
    }
}
```

---

## Full Example: Add Product to Cart Flow

```java
package com.shopeasy.playwright.tests;

import com.shopeasy.playwright.base.BaseTest;
import com.microsoft.playwright.Locator;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class CartTest extends BaseTest {

    @BeforeMethod
    public void loginAsCustomer() {
        // Navigate to login and authenticate
        navigateTo("login");
        page.getByPlaceholder("Enter your username").fill("customer1");
        page.getByPlaceholder("Enter your password").fill("pass123");
        page.locator("button[type='submit']").click();
        page.waitForURL("**/#!/products");
    }

    @Test
    public void addFirstProductToCart() {
        // Products page should already be visible after login
        navigateTo("products");

        // Wait for product cards to load
        Locator firstCard = page.locator(".product-card").first();
        assertThat(firstCard).isVisible();

        // Get product name before adding to cart
        String productName = firstCard.locator("h5").textContent().trim();
        System.out.println("Adding to cart: " + productName);

        // Click "Add to Cart" on the first product
        firstCard.locator("button.btn-primary").click();

        // Verify success notification appears
        Locator successAlert = page.locator(".alert-success");
        assertThat(successAlert).isVisible();
        assertThat(successAlert).containsText("added to cart");
    }

    @Test
    public void addSpecificProductToCart() {
        navigateTo("products");

        // Wait for products to load
        assertThat(page.locator(".product-card").first()).isVisible();

        // Find "Laptop Pro" card specifically and add to cart
        page.locator(".product-card")
            .filter(new Locator.FilterOptions().setHasText("Laptop Pro"))
            .locator("button.btn-primary")
            .click();

        // Verify success
        assertThat(page.locator(".alert-success")).isVisible();
    }

    @Test
    public void verifyCartContents() {
        // Add a product first
        navigateTo("products");
        assertThat(page.locator(".product-card").first()).isVisible();
        page.locator(".product-card").first().locator("button.btn-primary").click();
        assertThat(page.locator(".alert-success")).isVisible();

        // Navigate to cart
        navigateTo("cart");

        // Cart should have at least one item
        Locator cartItems = page.locator(".cart-item");
        assertThat(cartItems.first()).isVisible();
        System.out.println("Items in cart: " + cartItems.count());
    }

    @Test
    public void searchAndFilterProducts() {
        navigateTo("products");

        // Step 1: Wait for products to load
        assertThat(page.locator(".product-card").first()).isVisible();
        int totalProducts = page.locator(".product-card").count();

        // Step 2: Search for "laptop"
        page.getByPlaceholder("Search products...").fill("laptop");

        // Step 3: Verify filtered results (less than or equal to total)
        Locator filteredCards = page.locator(".product-card");
        assertThat(filteredCards.first()).isVisible();
        int filteredCount = filteredCards.count();
        System.out.println("Filtered count: " + filteredCount + " of " + totalProducts);

        // Step 4: Filter by category
        page.getByPlaceholder("Search products...").clear();
        page.locator("select[ng-model='selectedCategory']").selectOption("Electronics");

        // Verify category filtered results
        assertThat(page.locator(".product-card").first()).isVisible();
    }
}
```

---

## Actions Quick Reference

| Action | Method | Notes |
|---|---|---|
| Navigate | `page.navigate(url)` | Waits for load event |
| Click | `.click()` | Auto-waits for actionability |
| Double-click | `.dblclick()` | Auto-waits |
| Right-click | `.click(new ClickOptions().setButton(RIGHT))` | |
| Fill input | `.fill("text")` | Clears first, then fills |
| Type character by character | `.type("text")` | For autocomplete inputs |
| Clear input | `.clear()` | Removes all text |
| Press key | `.press("Enter")` | Focus must be on element |
| Select dropdown | `.selectOption("value")` | `<select>` elements only |
| Check checkbox | `.check()` | |
| Uncheck checkbox | `.uncheck()` | |
| Hover | `.hover()` | Triggers mouseover events |
| Drag to element | `.dragTo(target)` | |
| Scroll into view | `.scrollIntoViewIfNeeded()` | Usually automatic |
| Get text | `.textContent()` | All text including hidden |
| Get visible text | `.innerText()` | Only visible text |
| Get input value | `.inputValue()` | Current value of input |
| Get attribute | `.getAttribute("attr")` | Any HTML attribute |
