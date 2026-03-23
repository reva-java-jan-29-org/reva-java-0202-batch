# 10 — Handle Different Types of Alerts & Frames

## Part A: Alerts

## What Are JavaScript Alerts?

JavaScript alerts are browser-native dialog boxes that overlay the web page. Selenium cannot interact with them using `findElement()` — they require `driver.switchTo().alert()`.

### Three Types of Alerts

| Type | JS Function | Has Input? | Buttons |
|---|---|---|---|
| **Alert** | `window.alert("message")` | No | OK |
| **Confirm** | `window.confirm("message")` | No | OK, Cancel |
| **Prompt** | `window.prompt("message", "default")` | Yes (text input) | OK, Cancel |

---

## Alert API Methods

```java
import org.openqa.selenium.Alert;

// Switch to the alert
Alert alert = driver.switchTo().alert();

// Read the alert text
String alertText = alert.getText();
System.out.println("Alert message: " + alertText);

// For Alert and Confirm: Accept (OK button)
alert.accept();

// For Confirm: Dismiss (Cancel button)
alert.dismiss();

// For Prompt: Type text then accept
alert.sendKeys("my input text");
alert.accept();
```

---

## ShopEasy Context — Where Alerts Appear

ShopEasy uses JavaScript's native `window.confirm()` for destructive actions:

1. **Delete product** — Admin clicks Delete → `confirm("Are you sure?")` → OK or Cancel
2. **Delete customer** — Admin deletes a customer
3. **Delete admin** — Admin account deletion

```html
<!-- admin-products.html — delete triggers confirm -->
<button class="btn btn-sm btn-outline-danger"
        ng-click="deleteProduct(p.id)">
    <i class="bi bi-trash"></i>
</button>
```

In AngularJS controller (`adminController.js`), `deleteProduct` calls `window.confirm(...)` before the API call.

---

## Handling Confirm Alerts in ShopEasy

### Case 1: Accept (OK) — Confirm Deletion

```java
@Test
public void deleteProductWithConfirmation() {
    // Login as admin first (assume helper method)
    loginAsAdmin();
    navigateTo("admin/products");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.cssSelector("table tbody tr")
    ));

    // Count rows before deletion
    int rowsBefore = driver.findElements(By.cssSelector("table tbody tr")).size();

    // Click delete on the first product
    WebElement deleteBtn = driver.findElement(
        By.cssSelector("table tbody tr:first-child .btn-outline-danger")
    );
    deleteBtn.click();

    // A confirm dialog appears — switch to it
    Alert confirmAlert = driver.switchTo().alert();
    String alertMessage = confirmAlert.getText();
    System.out.println("Alert says: " + alertMessage);
    // Expected: "Are you sure you want to delete this product?"

    // Accept — click OK to confirm deletion
    confirmAlert.accept();

    // Wait for the row to disappear
    wait.until(ExpectedConditions.numberOfElementsToBeLessThan(
        By.cssSelector("table tbody tr"), rowsBefore
    ));
    System.out.println("Product deleted successfully");
}
```

### Case 2: Dismiss (Cancel) — Abort Deletion

```java
@Test
public void cancelProductDeletion() {
    loginAsAdmin();
    navigateTo("admin/products");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.cssSelector("table tbody tr")
    ));

    int rowsBefore = driver.findElements(By.cssSelector("table tbody tr")).size();

    // Click delete
    driver.findElement(By.cssSelector("table tbody tr:first-child .btn-outline-danger"))
          .click();

    // Switch to alert and dismiss (Cancel)
    Alert alert = driver.switchTo().alert();
    alert.dismiss();

    // Row count should remain the same
    int rowsAfter = driver.findElements(By.cssSelector("table tbody tr")).size();
    Assert.assertEquals(rowsAfter, rowsBefore, "Row count should not change after cancel");
}
```

---

## Handling Alerts — Complete Pattern

```java
// Safe alert handling with wait
private void handleAlert(boolean accept) {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
    try {
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        System.out.println("Alert text: " + alert.getText());
        if (accept) {
            alert.accept();
        } else {
            alert.dismiss();
        }
    } catch (TimeoutException e) {
        System.out.println("No alert appeared within timeout");
    }
}

// Usage
deleteBtn.click();
handleAlert(true);  // accept deletion
```

---

## Alert Edge Cases

```java
// Prompt — type text before accepting
Alert prompt = driver.switchTo().alert();
prompt.sendKeys("My custom input");
prompt.accept();
String enteredValue = prompt.getText(); // read before accepting

// Check if alert is present (don't throw if no alert)
try {
    Alert alert = driver.switchTo().alert();
    alert.accept();
} catch (NoAlertPresentException e) {
    System.out.println("No alert was present");
}
```

---

## Part B: Frames & iFrames

## What Are Frames?

Frames embed one HTML document inside another. Selenium cannot interact with content inside a frame without first switching to it — the frame acts as a separate document context.

```html
<!-- Types of frames -->
<frame src="page.html">               <!-- old <frameset> tag -->
<iframe src="page.html" id="myFrame"> <!-- inline frame, modern usage -->
```

---

## switchTo().frame() — Three Ways to Switch

```java
// 1. By index (0-based, order of appearance in DOM)
driver.switchTo().frame(0);

// 2. By name or id attribute
driver.switchTo().frame("myFrame");
driver.switchTo().frame("frameId");

// 3. By WebElement
WebElement frameElement = driver.findElement(By.id("myFrame"));
driver.switchTo().frame(frameElement);  // Most reliable
```

## Returning to Main Document

```java
driver.switchTo().defaultContent();    // Return to main document from any frame
driver.switchTo().parentFrame();       // Return to immediate parent frame (for nested frames)
```

---

## Frame Handling Pattern

```java
// Step 1: Locate and switch to frame
WebElement iFrame = driver.findElement(By.cssSelector("iframe#myFrame"));
driver.switchTo().frame(iFrame);

// Step 2: Interact with content inside the frame
WebElement inputInsideFrame = driver.findElement(By.id("inputInFrame"));
inputInsideFrame.sendKeys("text inside frame");

// Step 3: Switch back to main document
driver.switchTo().defaultContent();

// Step 4: Interact with main document again
driver.findElement(By.id("mainButton")).click();
```

---

## ShopEasy Context — Bootstrap Modal as Frame-like Context

ShopEasy uses **Bootstrap modals** (`<div class="modal">`) which are NOT iFrames — they are regular DOM elements. However, they require waiting for the modal to appear.

```html
<div class="modal fade" id="productModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <input ng-model="productForm.name" class="form-control">
        </div>
    </div>
</div>
```

```java
// Open the product modal
driver.findElement(By.cssSelector("button[ng-click='newProduct()']")).click();

// Wait for Bootstrap modal to open (class "show" is added)
WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("productModal")));

// Interact with modal content (NO switchTo needed — it's a regular div)
WebElement nameField = driver.findElement(
    By.cssSelector("#productModal input[ng-model='productForm.name']")
);
nameField.sendKeys("New Laptop");

WebElement priceField = driver.findElement(
    By.cssSelector("#productModal input[ng-model='productForm.price']")
);
priceField.sendKeys("999.99");

// Click Save button in modal
driver.findElement(By.cssSelector("#productModal button[ng-click='saveProduct()']"))
      .click();

// Wait for modal to close
wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("productModal")));
```

---

## Nested Frames

When a frame contains another frame inside it:

```java
// Switch to outer frame first
driver.switchTo().frame("outerFrame");

// Now switch to inner frame
driver.switchTo().frame("innerFrame");

// Interact inside inner frame
driver.findElement(By.id("innerElement")).click();

// Go back one level to outer frame
driver.switchTo().parentFrame();

// Go all the way back to main document
driver.switchTo().defaultContent();
```

---

## Complete Example — Frame Interaction

```java
@Test
public void handleFrameDemo() {
    driver.get("https://the-internet.herokuapp.com/frames");

    // Switch to frame by name
    driver.switchTo().frame("frame-bottom");
    String frameText = driver.findElement(By.cssSelector("p")).getText();
    System.out.println("Content in bottom frame: " + frameText);

    // Switch back to main document
    driver.switchTo().defaultContent();

    // Switch to another frame
    driver.switchTo().frame("frame-top");
    System.out.println("Now in top frame: " +
        driver.findElement(By.cssSelector("p")).getText());

    // Back to main
    driver.switchTo().defaultContent();
    System.out.println("Back to main document");
}
```

---

## Key Differences: Alert vs Frame vs Modal

| Feature | Alert | iFrame | Bootstrap Modal |
|---|---|---|---|
| Native browser | Yes | No | No |
| `switchTo()` needed | Yes (`switchTo().alert()`) | Yes (`switchTo().frame()`) | No |
| In DOM | No | Yes | Yes |
| Blocks interaction | Yes (browser-native) | No | Sometimes (modal backdrop) |
| `findElement()` works | No | Yes (after switch) | Yes |

---

## Common Alert/Frame Errors

| Exception | Cause | Fix |
|---|---|---|
| `NoAlertPresentException` | `switchTo().alert()` called with no alert open | Add `alertIsPresent()` explicit wait |
| `UnhandledAlertException` | Alert is present but code tries to do something else | Handle/dismiss the alert first |
| `NoSuchFrameException` | Frame with given name/id not found | Verify frame name; wait for frame to load |
| `StaleElementReferenceException` after `switchTo()` | Frame reloaded after switching | Re-find the frame element |
