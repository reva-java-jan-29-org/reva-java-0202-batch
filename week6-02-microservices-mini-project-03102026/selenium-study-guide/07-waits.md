# 07 — WebDriver Methods: Different Types of Waits

## Why Are Waits Necessary?

Modern web applications like ShopEasy are **asynchronous**. When a user logs in:
1. The browser sends an HTTP request to the API
2. AngularJS processes the response
3. `ng-repeat` or `ng-show` updates the DOM

If Selenium doesn't wait, it tries to interact with elements **before they exist or are visible** — causing flaky, unreliable tests.

```
User clicks Login → HTTP request fires → Response arrives → AngularJS updates DOM → Element appears
                                                                                          ↑
                                                              Selenium should wait until here
```

---

## Three Types of Waits

| Type | Mechanism | Scope | Use When |
|---|---|---|---|
| **Implicit Wait** | Polls DOM on every `findElement` | Global (all elements) | Basic apps, simple cases |
| **Explicit Wait** | Wait for a specific condition | Per element/condition | Recommended for SPA apps |
| **Fluent Wait** | Custom polling interval + ignore exceptions | Per element | Complex scenarios |

---

## 1. Implicit Wait

Set once, applies to **every** `findElement` call. When an element is not found, Selenium keeps retrying until the timeout.

### Setup (in BaseTest)

```java
driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
```

### How It Works

```
findElement() called
     │
     ▼
Element found immediately? → YES → Return element
     │ NO
     ▼
Wait and retry up to 10 seconds
     │
  Still not found?
     ├── YES → throw NoSuchElementException
     └── NO  → Return element
```

### Limitations

- Does NOT wait for element **visibility** — only presence in DOM
- In AngularJS, `ng-if` removes elements from DOM; `ng-show` hides them but keeps them in DOM
- Does NOT wait for conditions like "text changed" or "class added"
- Cannot ignore specific exception types
- Mixing implicit + explicit waits causes unpredictable behavior

---

## 2. Explicit Wait — `WebDriverWait` (Recommended for ShopEasy)

Waits for a **specific condition** to be true, with a custom timeout per wait.

### Import Required

```java
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.time.Duration;
```

### Basic Syntax

```java
WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("...")));
```

### All ExpectedConditions — With ShopEasy Examples

#### `visibilityOfElementLocated` — Element is visible

```java
WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

// Wait for product cards to appear after AngularJS renders
navigateTo("products");
WebElement firstProduct = wait.until(
    ExpectedConditions.visibilityOfElementLocated(
        By.cssSelector(".product-card")
    )
);
```

#### `elementToBeClickable` — Element is visible AND enabled

```java
// Wait for login button to become clickable (after form is filled and valid)
WebElement loginBtn = wait.until(
    ExpectedConditions.elementToBeClickable(
        By.cssSelector("button[type='submit']")
    )
);
loginBtn.click();
```

#### `presenceOfElementLocated` — Element exists in DOM (may not be visible)

```java
// AngularJS error alert: exists in DOM (ng-show) but hidden initially
// After wrong login, wait for it to become visible
WebElement errorAlert = wait.until(
    ExpectedConditions.visibilityOfElementLocated(
        By.cssSelector(".alert-danger")
    )
);
Assert.assertTrue(errorAlert.getText().contains("Invalid"), "Should show error message");
```

#### `invisibilityOfElementLocated` — Wait for element to disappear

```java
// Wait for the loading spinner to disappear after login attempt
wait.until(
    ExpectedConditions.invisibilityOfElementLocated(
        By.cssSelector(".spinner-border")
    )
);
System.out.println("Loading complete");
```

#### `urlContains` — Wait for URL to change

```java
// After login, wait for redirect to admin dashboard
driver.findElement(By.cssSelector("button[type='submit']")).click();
wait.until(ExpectedConditions.urlContains("admin/dashboard"));
System.out.println("Redirected to: " + driver.getCurrentUrl());
```

#### `urlToBe` — Wait for exact URL

```java
wait.until(ExpectedConditions.urlToBe(BASE_URL + "/#!/products"));
```

#### `titleContains` and `titleIs`

```java
wait.until(ExpectedConditions.titleContains("ShopEasy"));
wait.until(ExpectedConditions.titleIs("ShopEasy - E-Commerce"));
```

#### `textToBePresentInElement` — Wait for specific text to appear in an element

```java
// After adding a product to cart, wait for success alert
WebElement globalAlert = wait.until(
    ExpectedConditions.visibilityOfElementLocated(
        By.cssSelector(".alert")
    )
);
wait.until(
    ExpectedConditions.textToBePresentInElement(globalAlert, "added to cart")
);
```

#### `attributeContains` — Wait for an attribute to have a value

```java
// Wait for modal to become visible (Bootstrap adds class "show" to visible modals)
wait.until(
    ExpectedConditions.attributeContains(
        By.cssSelector("#productModal"), "class", "show"
    )
);
```

#### `numberOfElementsToBeMoreThan` — Wait for a list to populate

```java
// Wait until at least 1 product card appears
wait.until(
    ExpectedConditions.numberOfElementsToBeMoreThan(
        By.cssSelector(".product-card"), 0
    )
);
List<WebElement> products = driver.findElements(By.cssSelector(".product-card"));
System.out.println("Products loaded: " + products.size());
```

#### `stalenessOf` — Wait for element to be removed from DOM

```java
// After deleting a product, wait for the row to be removed
WebElement rowToDelete = driver.findElement(By.xpath("//strong[text()='Old Product']/ancestor::tr"));
driver.findElement(By.cssSelector(".btn-outline-danger")).click(); // trigger delete
wait.until(ExpectedConditions.stalenessOf(rowToDelete));
System.out.println("Row removed from DOM");
```

---

## 3. Fluent Wait — Custom Polling

`FluentWait` gives you control over:
- Maximum wait time
- Polling frequency (how often to check)
- Which exceptions to ignore during polling

```java
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.NoSuchElementException;
import java.util.function.Function;

// Build a FluentWait
Wait<WebDriver> fluentWait = new FluentWait<>(driver)
    .withTimeout(Duration.ofSeconds(30))          // max wait
    .pollingEvery(Duration.ofSeconds(2))           // check every 2 seconds
    .ignoring(NoSuchElementException.class);       // ignore this exception

// Use it
WebElement result = fluentWait.until(new Function<WebDriver, WebElement>() {
    @Override
    public WebElement apply(WebDriver driver) {
        return driver.findElement(By.cssSelector(".product-card"));
    }
});

// Lambda equivalent (Java 8+)
WebElement resultLambda = fluentWait.until(
    d -> d.findElement(By.cssSelector(".product-card"))
);
```

**When to use FluentWait over WebDriverWait:**
- When page rendering is extremely slow and you need custom poll intervals
- When you need to ignore multiple specific exceptions
- When the condition is a custom complex check

---

## AngularJS-Specific Wait Strategies

ShopEasy's AngularJS app has asynchronous HTTP calls (`$http`). When you click a button that triggers an API call, the DOM updates after the response arrives.

### Strategy 1: Wait for Spinner to Disappear

ShopEasy shows a spinner while loading:
```html
<div class="spinner-border text-primary" role="status" ng-show="loading"></div>
```

```java
private void waitForLoadingToComplete() {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    // Wait for loading spinner to become invisible
    wait.until(ExpectedConditions.invisibilityOfElementLocated(
        By.cssSelector(".spinner-border")
    ));
}
```

### Strategy 2: Wait for Products to Appear

```java
private void waitForProductsToLoad() {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
        By.cssSelector(".product-card"), 0
    ));
}
```

### Strategy 3: Wait for Alert Message After Action

```java
private void waitForSuccessAlert() {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.cssSelector(".alert.alert-success")
    ));
}
```

### Strategy 4: Wait for URL Change (SPA Navigation)

```java
private void waitForNavigation(String routePart) {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(ExpectedConditions.urlContains(routePart));
}
```

---

## `Thread.sleep()` — The Anti-Pattern

```java
Thread.sleep(3000); // DO NOT USE IN REAL TESTS
```

**Why it's bad:**
- Wastes time if the element loads faster than the sleep duration
- Still fails if the element loads slower
- Makes test suite much slower (multiply by hundreds of tests)
- Masks real performance problems

**The only acceptable use:**
- Debugging locally to slow down execution for observation
- Never commit `Thread.sleep()` to version control

---

## Wait Strategy in BaseTest (Recommended Setup)

```java
public class BaseTest {
    protected WebDriver driver;
    protected WebDriverWait wait;
    protected static final String BASE_URL = "http://localhost:4200";

    @BeforeMethod
    public void setUp() {
        driver = new ChromeDriver();
        driver.manage().window().maximize();

        // Use ONLY implicit wait OR explicit wait — not both
        // Recommended: use explicit wait only (comment out implicit wait)
        // driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));

        // Create a shared WebDriverWait instance
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    // Reusable wait helper methods
    protected WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    protected void waitForUrl(String urlFragment) {
        wait.until(ExpectedConditions.urlContains(urlFragment));
    }

    protected void waitForSpinnerToDisappear() {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(
            By.cssSelector(".spinner-border")
        ));
    }
}
```

---

## Complete Wait Example: Login Flow

```java
@Test
public void loginFlow() {
    navigateTo("login");

    // Wait for form to be ready
    WebElement usernameInput = waitForVisible(
        By.cssSelector("[ng-model='loginData.username']")
    );

    usernameInput.sendKeys("admin");
    driver.findElement(By.cssSelector("[ng-model='loginData.password']"))
          .sendKeys("admin123");

    // Wait for button to be clickable (AngularJS may still be setting up)
    WebElement loginBtn = waitForClickable(
        By.cssSelector("button[type='submit']")
    );
    loginBtn.click();

    // Wait for navigation to admin dashboard
    waitForUrl("admin/dashboard");

    // Wait for spinner to disappear (dashboard is loading data)
    waitForSpinnerToDisappear();

    // Now verify dashboard content
    List<WebElement> statCards = driver.findElements(
        By.cssSelector(".card.shadow-sm")
    );
    Assert.assertTrue(statCards.size() > 0, "Dashboard should show stat cards");
}
```
