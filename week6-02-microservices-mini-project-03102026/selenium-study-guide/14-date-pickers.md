# 14 — Handle Date Pickers

## Types of Date Pickers in Web Apps

| Type | HTML | Selenium Approach |
|---|---|---|
| **Native HTML date input** | `<input type="date">` | `sendKeys("yyyy-MM-dd")` |
| **Native HTML datetime input** | `<input type="datetime-local">` | `sendKeys("yyyy-MM-ddTHH:mm")` |
| **Bootstrap Datepicker** | Custom JS widget | Click calendar icon → navigate months → click day |
| **Angular Material DatePicker** | `mat-datepicker` | Click calendar → navigate → click day |
| **jQuery UI DatePicker** | Custom widget | Click input → navigate → click day cell |
| **Read-only input + JS** | Input blocked, JS sets value | JavascriptExecutor to set value directly |

---

## ShopEasy Context

ShopEasy's current frontend does **not** have an explicit date picker in its forms. Dates appear as:
- **Display only** — Order date, payment date in tables (`{{order.createdAt | date:'MMM dd, yyyy'}}`)
- **Card expiry** — `<input type="text" ng-model="cardExpiry" placeholder="MM/YY">` (treated as text)

The cart checkout has a card expiry field formatted as `MM/YY` which is a simple text input — not a date picker. The patterns in this module apply when you add date filters to the admin panel or date-of-birth fields to registration.

---

## Approach 1: Native HTML Date Input

The simplest case — `<input type="date">` accepts a date string in `yyyy-MM-dd` format.

```html
<input type="date" id="orderDate" name="orderDate">
<input type="datetime-local" id="appointmentTime" name="appointmentTime">
<input type="month" id="cardMonth" name="cardMonth">
```

```java
// Type date in yyyy-MM-dd format
WebElement dateInput = driver.findElement(By.id("orderDate"));
dateInput.sendKeys("2026-03-15");
System.out.println("Value set: " + dateInput.getAttribute("value")); // "2026-03-15"

// datetime-local
WebElement datetimeInput = driver.findElement(By.id("appointmentTime"));
datetimeInput.sendKeys("2026-03-15T14:30");

// Clear and set a new date
dateInput.clear();
dateInput.sendKeys("2026-12-31");

// Verify
Assert.assertEquals(dateInput.getAttribute("value"), "2026-12-31");
```

---

## Approach 2: ShopEasy Card Expiry (MM/YY Text Input)

```html
<input type="text" class="form-control"
       ng-model="cardExpiry"
       placeholder="MM/YY" maxlength="5">
```

```java
navigateTo("cart");

// First proceed to checkout to reveal the card fields
driver.findElement(By.cssSelector("button[ng-click='proceedToCheckout()']")).click();

WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
wait.until(ExpectedConditions.visibilityOfElementLocated(
    By.cssSelector("[ng-model='cardExpiry']")
));

WebElement expiryInput = driver.findElement(By.cssSelector("[ng-model='cardExpiry']"));

// Type MM/YY format
expiryInput.sendKeys("12/28");
Assert.assertEquals(expiryInput.getAttribute("value"), "12/28");
```

---

## Approach 3: JavaScript-Blocked Date Input

Some date pickers disable the text input and use JavaScript only. `sendKeys()` won't work — use `JavascriptExecutor`.

```java
WebElement dateInput = driver.findElement(By.id("datePicker"));

// Use JS executor to set the value
JavascriptExecutor js = (JavascriptExecutor) driver;
js.executeScript("arguments[0].value = arguments[1];", dateInput, "2026-03-15");

// Trigger change event (Angular/Vue/React apps need this)
js.executeScript(
    "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));", dateInput
);
js.executeScript(
    "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));", dateInput
);
```

---

## Approach 4: Bootstrap Datepicker Widget (Calendar UI)

Many apps use a visual calendar. The approach: click to open → navigate months → click the day.

```html
<!-- Example Bootstrap datepicker structure -->
<div class="input-group date" id="datePicker">
    <input type="text" class="form-control" placeholder="Select date">
    <span class="input-group-text"><i class="bi bi-calendar"></i></span>
</div>
<!-- Calendar widget (rendered dynamically) -->
<div class="datepicker-dropdown">
    <div class="datepicker-days">
        <table>
            <thead>
                <tr>
                    <th class="prev">«</th>
                    <th class="datepicker-switch" colspan="5">March 2026</th>
                    <th class="next">»</th>
                </tr>
                <tr><th>Su</th><th>Mo</th>...</tr>
            </thead>
            <tbody>
                <tr><td class="day">1</td><td class="day">2</td>...</tr>
            </tbody>
        </table>
    </div>
</div>
```

```java
public void selectDate(String targetMonth, int targetYear, int targetDay) {
    // Step 1: Click the calendar icon to open datepicker
    driver.findElement(By.cssSelector(".input-group-text")).click();

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Step 2: Navigate to the correct month/year
    while (true) {
        // Read current month/year shown
        String currentMonthYear = driver.findElement(
            By.cssSelector(".datepicker-switch")
        ).getText(); // e.g., "March 2026"

        if (currentMonthYear.equals(targetMonth + " " + targetYear)) {
            break; // Correct month
        }

        // Is target before or after current?
        // For simplicity, click "next" — real code would check direction
        driver.findElement(By.cssSelector("th.next")).click();
        wait.until(ExpectedConditions.not(
            ExpectedConditions.textToBePresentInElement(
                driver.findElement(By.cssSelector(".datepicker-switch")),
                currentMonthYear
            )
        ));
    }

    // Step 3: Click the target day
    List<WebElement> days = driver.findElements(By.cssSelector("td.day"));
    for (WebElement day : days) {
        if (day.getText().equals(String.valueOf(targetDay)) &&
            !day.getAttribute("class").contains("old") &&
            !day.getAttribute("class").contains("new")) {
            day.click();
            break;
        }
    }
}

// Usage
selectDate("March", 2026, 15);
```

---

## Approach 5: Reading Dates from Tables (ShopEasy)

ShopEasy displays dates in orders and payments tables. Reading them back:

```java
// Orders table — "Date" column shows order creation date
navigateTo("orders");
WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("table tbody tr")));

List<WebElement> rows = driver.findElements(By.cssSelector("table tbody tr"));
for (WebElement row : rows) {
    List<WebElement> cells = row.findElements(By.tagName("td"));
    if (cells.size() >= 2) {
        String orderId = cells.get(0).getText();
        String orderDate = cells.get(1).getText(); // e.g., "Mar 15, 2026"
        System.out.println("Order " + orderId + " placed on: " + orderDate);
    }
}

// Parse date if needed
String dateStr = "Mar 15, 2026";
java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy");
java.util.Date parsedDate = sdf.parse(dateStr);
System.out.println("Parsed: " + parsedDate);
```

---

## Complete Date Handling Test — Card Expiry in ShopEasy Cart

```java
@Test
public void fillCheckoutFormWithCardDetails() throws InterruptedException {
    // Assume user is logged in and has items in cart
    loginAsCustomer();
    navigateTo("cart");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

    // Wait for cart items to load
    wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.cssSelector("table tbody tr")
    ));

    // Click "Proceed to Checkout"
    WebElement checkoutBtn = wait.until(ExpectedConditions.elementToBeClickable(
        By.cssSelector("button[ng-click='proceedToCheckout()']")
    ));
    checkoutBtn.click();

    // Wait for checkout form to appear
    wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.cssSelector("[ng-model='shippingAddress']")
    ));

    // Fill shipping address
    driver.findElement(By.cssSelector("[ng-model='shippingAddress']"))
          .sendKeys("123 Main St, Springfield, IL 62701");

    // Fill card holder name
    driver.findElement(By.cssSelector("[ng-model='cardHolderName']"))
          .sendKeys("John Doe");

    // Fill card number (test number)
    driver.findElement(By.cssSelector("[ng-model='cardNumber']"))
          .sendKeys("4242424242424242");

    // Fill card expiry — MM/YY format
    WebElement expiryField = driver.findElement(By.cssSelector("[ng-model='cardExpiry']"));
    expiryField.clear();
    expiryField.sendKeys("12/28");
    Assert.assertEquals(expiryField.getAttribute("value"), "12/28",
        "Card expiry should be 12/28");

    // Fill CVV
    driver.findElement(By.cssSelector("[ng-model='cardCvv']")).sendKeys("123");

    // Verify all fields are filled before placing order
    Assert.assertFalse(
        driver.findElement(By.cssSelector("[ng-model='shippingAddress']"))
              .getAttribute("value").isEmpty(),
        "Shipping address should not be empty"
    );

    System.out.println("Checkout form filled successfully");

    // Click Place Order & Pay
    driver.findElement(By.cssSelector("button[ng-click='placeOrder()']")).click();

    // Wait for order confirmation
    wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.cssSelector(".alert-success")
    ));
    System.out.println("Order placed!");
}
```

---

## Key Tips for Date Handling

| Situation | Best Approach |
|---|---|
| `<input type="date">` | `sendKeys("yyyy-MM-dd")` |
| Text input with date format | `sendKeys("MM/YY")` or `sendKeys("dd/MM/yyyy")` |
| JavaScript-blocked input | `JavascriptExecutor.executeScript("arguments[0].value='...'")` |
| Calendar widget | Open → navigate months → click day number |
| Reading displayed date | `getText()` then parse with `SimpleDateFormat` |
| Clear existing date | `element.clear()` then `sendKeys()` |
