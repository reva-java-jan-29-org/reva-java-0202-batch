# 09 — Handle Checkboxes & Radio Buttons

## Concepts

Checkboxes and radio buttons are special HTML input elements handled differently from regular text inputs:

- **Checkbox** `<input type="checkbox">`: Can be checked or unchecked independently. Multiple can be selected.
- **Radio button** `<input type="radio">`: Belong to a group (same `name`). Only one in a group can be selected.

---

## Checkbox Interaction Methods

| Method | Description |
|---|---|
| `element.click()` | Toggle the checkbox (check if unchecked, uncheck if checked) |
| `element.isSelected()` | Returns `true` if checked |
| `element.isEnabled()` | Returns `true` if not disabled |
| `element.isDisplayed()` | Returns `true` if visible |

> **Important:** Always check `isSelected()` BEFORE clicking to avoid toggling the wrong state.

---

## Checkbox — Basic Example (Generic HTML)

```html
<input type="checkbox" id="rememberMe" name="rememberMe">
<label for="rememberMe">Remember Me</label>

<input type="checkbox" id="terms" name="terms" checked>
<label for="terms">Accept Terms</label>
```

```java
// Find checkbox
WebElement rememberMe = driver.findElement(By.id("rememberMe"));
WebElement terms = driver.findElement(By.id("terms"));

// Check current state
System.out.println("rememberMe checked: " + rememberMe.isSelected()); // false
System.out.println("terms checked: " + terms.isSelected());            // true

// ── CHECK a checkbox ─────────────────────────────────────────────
if (!rememberMe.isSelected()) {
    rememberMe.click();   // Now it's checked
}
Assert.assertTrue(rememberMe.isSelected(), "rememberMe should be checked");

// ── UNCHECK a checkbox ───────────────────────────────────────────
if (terms.isSelected()) {
    terms.click();        // Now it's unchecked
}
Assert.assertFalse(terms.isSelected(), "terms should be unchecked");
```

---

## Radio Button — Basic Example (Generic HTML)

```html
<!-- Only one can be selected within the same name group -->
<input type="radio" id="roleCustomer" name="role" value="CUSTOMER" checked>
<label for="roleCustomer">Customer</label>

<input type="radio" id="roleAdmin" name="role" value="ADMIN">
<label for="roleAdmin">Admin</label>
```

```java
// Find radio buttons by value attribute
WebElement customerRadio = driver.findElement(By.cssSelector("input[value='CUSTOMER']"));
WebElement adminRadio    = driver.findElement(By.cssSelector("input[value='ADMIN']"));

// Check state
System.out.println("Customer selected: " + customerRadio.isSelected()); // true (default)
System.out.println("Admin selected: " + adminRadio.isSelected());        // false

// Select Admin radio
adminRadio.click();
Assert.assertTrue(adminRadio.isSelected(), "Admin radio should now be selected");
Assert.assertFalse(customerRadio.isSelected(), "Customer radio should be deselected");

// Get all radio buttons in a group
List<WebElement> radioGroup = driver.findElements(By.cssSelector("input[name='role']"));
System.out.println("Radio options: " + radioGroup.size()); // 2

// Find which one is currently selected
for (WebElement radio : radioGroup) {
    if (radio.isSelected()) {
        System.out.println("Selected role: " + radio.getAttribute("value"));
    }
}
```

---

## ShopEasy Context

ShopEasy's current frontend does not have explicit checkbox or radio inputs in its main flows. However, the patterns appear in:

1. **Potential future admin features** — Enable/disable customer accounts (currently done via buttons but could be checkboxes)
2. **Filter panels** — Category filter could be implemented as checkboxes in an e-commerce context
3. **Role selection** — During admin creation, a role selection could use radio buttons

The skills you practice here apply directly when these UI patterns exist.

---

## Simulating Checkbox Patterns in ShopEasy

### Scenario 1: "Enable/Disable" Toggle Buttons as Checkbox Equivalents

ShopEasy's admin customer management uses **buttons** as toggles (Enable/Disable) instead of checkboxes. You handle these like regular button interactions:

```html
<!-- admin-customers.html -->
<button class="btn btn-sm btn-warning" ng-click="disableCustomer(c.id)"
        ng-show="c.enabled">Disable</button>
<button class="btn btn-sm btn-success" ng-click="enableCustomer(c.id)"
        ng-show="!c.enabled">Enable</button>
```

```java
navigateTo("admin/customers");
WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
wait.until(ExpectedConditions.visibilityOfElementLocated(
    By.cssSelector("table tbody tr")
));

// Find the first customer row
WebElement firstRow = driver.findElement(By.cssSelector("table tbody tr:first-child"));

// Check if "Disable" button is visible (customer is currently enabled)
List<WebElement> disableBtns = firstRow.findElements(
    By.cssSelector("button.btn-warning")
);
boolean isCurrentlyEnabled = !disableBtns.isEmpty() && disableBtns.get(0).isDisplayed();

if (isCurrentlyEnabled) {
    disableBtns.get(0).click();     // Disable the customer
    // Wait for UI to update
    wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.cssSelector("button.btn-success")
    ));
    System.out.println("Customer disabled successfully");
}
```

---

## Advanced Checkbox Handling Patterns

### Select All Checkboxes in a Table

```java
// Generic pattern for a table with "select all" checkbox
WebElement selectAll = driver.findElement(By.cssSelector("input#selectAll"));

// Check all
if (!selectAll.isSelected()) {
    selectAll.click();
}

// Verify all individual checkboxes are now selected
List<WebElement> rowCheckboxes = driver.findElements(
    By.cssSelector("tbody input[type='checkbox']")
);
for (WebElement cb : rowCheckboxes) {
    Assert.assertTrue(cb.isSelected(), "All row checkboxes should be selected");
}
```

### Deselect All Checkboxes

```java
List<WebElement> allCheckboxes = driver.findElements(
    By.cssSelector("input[type='checkbox']")
);
for (WebElement cb : allCheckboxes) {
    if (cb.isSelected()) {
        cb.click(); // Uncheck
    }
}
System.out.println("All checkboxes unchecked");
```

### Count Checked Checkboxes

```java
List<WebElement> allCheckboxes = driver.findElements(
    By.cssSelector("input[type='checkbox']")
);
long checkedCount = allCheckboxes.stream()
    .filter(WebElement::isSelected)
    .count();
System.out.println("Checked: " + checkedCount + " / " + allCheckboxes.size());
```

---

## Common Mistakes and Fixes

| Mistake | Problem | Fix |
|---|---|---|
| Always clicking without checking `isSelected()` | Double-click toggles back | Check state first: `if (!cb.isSelected()) cb.click()` |
| Using `sendKeys(" ")` for checkbox | Unreliable | Use `click()` instead |
| Clicking label instead of input | May work or fail depending on `for` attribute | Click `input[type='checkbox']` directly |
| `isSelected()` always returns false | Wrong element located | Ensure you're on the `<input>` element, not `<label>` or `<div>` |

---

## Complete Test — Checkbox/Radio Pattern

```java
@Test
public void checkboxAndRadioHandlingDemo() {
    // This test uses a generic page with checkboxes + radio buttons
    // Apply same patterns to ShopEasy when checkboxes are added

    driver.get("https://the-internet.herokuapp.com/checkboxes");

    // Find both checkboxes
    List<WebElement> checkboxes = driver.findElements(By.cssSelector("input[type='checkbox']"));
    Assert.assertEquals(checkboxes.size(), 2, "Should find 2 checkboxes");

    WebElement checkbox1 = checkboxes.get(0);
    WebElement checkbox2 = checkboxes.get(1);

    // Print initial states
    System.out.println("Checkbox 1 initial: " + checkbox1.isSelected());
    System.out.println("Checkbox 2 initial: " + checkbox2.isSelected());

    // Check checkbox 1 (it starts unchecked)
    if (!checkbox1.isSelected()) {
        checkbox1.click();
    }
    Assert.assertTrue(checkbox1.isSelected());

    // Uncheck checkbox 2 (it starts checked)
    if (checkbox2.isSelected()) {
        checkbox2.click();
    }
    Assert.assertFalse(checkbox2.isSelected());

    System.out.println("Checkbox test complete");
}
```

> **Practice site for checkboxes & radio buttons:**
> - https://the-internet.herokuapp.com/checkboxes
> - https://demoqa.com/radio-button
> - https://demoqa.com/checkbox
