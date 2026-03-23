# 17 — Handle File Upload, Scrolling Page & JavascriptExecutor

## Part A: File Upload

## How File Upload Works in Selenium

Native `<input type="file">` elements can be handled directly with `sendKeys()` — pass the **absolute file path** as the value.

```html
<input type="file" id="avatarUpload" name="avatar" accept="image/*">
```

```java
// Get the absolute path of the file to upload
String filePath = System.getProperty("user.dir") + "/src/test/resources/test-image.jpg";

WebElement fileInput = driver.findElement(By.id("avatarUpload"));
fileInput.sendKeys(filePath); // This triggers the file selection
```

---

## ShopEasy Context — Image URL (Not File Upload)

ShopEasy's product form uses an **Image URL** text field — not a file upload:

```html
<input type="text" class="form-control"
       ng-model="productForm.imageUrl"
       placeholder="https://...">
```

This is a text `<input>`, handled with `sendKeys()`:

```java
// Open add product modal
driver.findElement(By.cssSelector("button[ng-click='newProduct()']")).click();
WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("productModal")));

// Fill Image URL field
driver.findElement(By.cssSelector("[ng-model='productForm.imageUrl']"))
      .sendKeys("https://images.unsplash.com/photo-12345/laptop.jpg");
```

---

## File Upload Patterns

### Pattern 1: Visible file input — `sendKeys(absolutePath)`

```java
String absolutePath = new java.io.File("src/test/resources/product-image.jpg")
    .getAbsolutePath();

WebElement fileInput = driver.findElement(By.cssSelector("input[type='file']"));
fileInput.sendKeys(absolutePath);

// Verify file selected
System.out.println("File name: " + fileInput.getAttribute("value"));
// Typically shows: "C:\fakepath\product-image.jpg" or just the filename
```

### Pattern 2: Hidden file input (display:none)

If the `<input type="file">` is hidden (common with custom styled buttons), make it visible first:

```java
WebElement hiddenFileInput = driver.findElement(By.cssSelector("input[type='file']"));

// Make visible via JS
JavascriptExecutor js = (JavascriptExecutor) driver;
js.executeScript("arguments[0].style.display='block';", hiddenFileInput);

// Now use sendKeys
hiddenFileInput.sendKeys(absolutePath);
```

### Pattern 3: Multiple file upload

```java
WebElement multiFileInput = driver.findElement(By.cssSelector("input[type='file'][multiple]"));
String file1 = new java.io.File("src/test/resources/image1.jpg").getAbsolutePath();
String file2 = new java.io.File("src/test/resources/image2.jpg").getAbsolutePath();

// Send multiple paths separated by newlines
multiFileInput.sendKeys(file1 + "\n" + file2);
```

---

## Part B: Scrolling

## Why Scroll?

- Elements below the viewport are not interactable until scrolled into view
- `ElementNotInteractableException` often means the element is off-screen
- AngularJS lazy-loads products as you scroll (infinite scroll pattern)

---

## Scroll Methods

### Method 1: `scrollIntoView` via JavascriptExecutor

```java
JavascriptExecutor js = (JavascriptExecutor) driver;
WebElement element = driver.findElement(By.cssSelector("footer"));

// Scroll until element is in view
js.executeScript("arguments[0].scrollIntoView(true);", element);

// Scroll into view with alignment options
// true  = align to top of viewport
// false = align to bottom of viewport
js.executeScript("arguments[0].scrollIntoView({behavior:'smooth', block:'center'});", element);
```

### Method 2: Scroll by pixel offset

```java
JavascriptExecutor js = (JavascriptExecutor) driver;

// Scroll down 500 pixels
js.executeScript("window.scrollBy(0, 500);");

// Scroll up 300 pixels
js.executeScript("window.scrollBy(0, -300);");

// Scroll to bottom of page
js.executeScript("window.scrollTo(0, document.body.scrollHeight);");

// Scroll to top of page
js.executeScript("window.scrollTo(0, 0);");

// Scroll to specific coordinates
js.executeScript("window.scrollTo(0, 1000);");
```

### Method 3: `Actions` class scroll (Selenium 4+)

```java
Actions actions = new Actions(driver);

// Scroll from element by delta
WebElement element = driver.findElement(By.cssSelector(".product-card"));
actions.scrollToElement(element).perform();

// Scroll from origin point
actions.scrollByAmount(0, 500).perform(); // scroll down 500px
actions.scrollByAmount(0, -300).perform(); // scroll up 300px
```

### Method 4: Arrow keys / Page Down

```java
WebElement body = driver.findElement(By.cssSelector("body"));
body.sendKeys(Keys.PAGE_DOWN);
body.sendKeys(Keys.END);  // scroll to bottom
body.sendKeys(Keys.HOME); // scroll to top
```

---

## ShopEasy Scrolling Examples

### Scroll to Footer and Verify Copyright

```java
@Test
public void scrollToFooterAndVerify() {
    navigateTo("products");
    JavascriptExecutor js = (JavascriptExecutor) driver;

    // Scroll to footer
    WebElement footer = driver.findElement(By.cssSelector("footer"));
    js.executeScript("arguments[0].scrollIntoView(true);", footer);

    // Verify footer text
    Assert.assertTrue(footer.isDisplayed(), "Footer should be visible after scrolling");
    String footerText = footer.getText();
    System.out.println("Footer: " + footerText);
    Assert.assertTrue(footerText.contains("ShopEasy"), "Footer should mention ShopEasy");
}
```

### Scroll Through Products Page

```java
@Test
public void scrollThroughProductsGrid() {
    navigateTo("products");
    JavascriptExecutor js = (JavascriptExecutor) driver;

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".product-card")));

    List<WebElement> cards = driver.findElements(By.cssSelector(".product-card"));
    System.out.println("Products loaded: " + cards.size());

    // Scroll through each product card
    for (WebElement card : cards) {
        js.executeScript("arguments[0].scrollIntoView({behavior:'smooth', block:'center'});", card);

        // Read product info
        String name = card.findElement(By.cssSelector("h6.card-title")).getText();
        System.out.println("Scrolled to: " + name);
    }

    // Scroll back to top
    js.executeScript("window.scrollTo(0, 0);");
    System.out.println("Scrolled back to top");
}
```

### Scroll to "Add to Cart" Button Before Clicking

```java
WebElement addToCartBtn = driver.findElement(
    By.cssSelector(".card-footer .btn-primary")
);

// Scroll into view
JavascriptExecutor js = (JavascriptExecutor) driver;
js.executeScript("arguments[0].scrollIntoView(true);", addToCartBtn);

// Now click (element is in viewport)
addToCartBtn.click();
```

---

## Part C: JavascriptExecutor

## What Is JavascriptExecutor?

`JavascriptExecutor` is an interface implemented by `ChromeDriver` (and all WebDriver implementations). It lets you execute arbitrary JavaScript in the browser context.

```java
JavascriptExecutor js = (JavascriptExecutor) driver;

// Execute JS and get return value
Object result = js.executeScript("return document.title;");
System.out.println("Title via JS: " + result);

// Execute JS with element argument
js.executeScript("arguments[0].click();", element);

// Execute JS with return value and element
Object text = js.executeScript("return arguments[0].textContent;", element);
```

---

## Common JavascriptExecutor Operations

### Click via JavaScript (when normal click fails)

```java
WebElement loginBtn = driver.findElement(By.cssSelector("button[type='submit']"));
JavascriptExecutor js = (JavascriptExecutor) driver;

// Useful when element is obscured by another element
js.executeScript("arguments[0].click();", loginBtn);
```

### Set Input Value via JavaScript (for blocked inputs)

```java
WebElement input = driver.findElement(By.cssSelector("[ng-model='loginData.username']"));

// Set value directly (bypasses AngularJS two-way binding — use only if sendKeys fails)
js.executeScript("arguments[0].value = 'admin';", input);

// IMPORTANT: For AngularJS, also trigger the input event so ng-model updates
js.executeScript(
    "angular.element(arguments[0]).triggerHandler('input');", input
);
```

### Get Element Properties

```java
// Get inner text
String text = (String) js.executeScript("return arguments[0].innerText;", element);

// Get attribute value
String href = (String) js.executeScript("return arguments[0].getAttribute('href');", element);

// Get computed style
String bg = (String) js.executeScript(
    "return window.getComputedStyle(arguments[0]).backgroundColor;", element
);

// Check if element is visible
Boolean visible = (Boolean) js.executeScript(
    "var el = arguments[0];" +
    "var rect = el.getBoundingClientRect();" +
    "return rect.top >= 0 && rect.bottom <= window.innerHeight;",
    element
);
```

### Interact with localStorage (ShopEasy uses this for JWT)

```java
// Read JWT token from localStorage
String token = (String) js.executeScript("return localStorage.getItem('token');");
System.out.println("JWT token: " + (token != null ? "present" : "absent"));

// Read user info
String userJson = (String) js.executeScript("return localStorage.getItem('user');");
System.out.println("User data: " + userJson);

// Simulate logout by clearing localStorage
js.executeScript("localStorage.clear();");
driver.navigate().refresh();
// After clear + refresh, user should be redirected to login
WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
wait.until(ExpectedConditions.urlContains("login"));
System.out.println("Logged out via localStorage.clear()");

// Set a fake token (for testing authenticated pages without going through login)
js.executeScript("localStorage.setItem('token', 'Bearer fake-token-for-testing');");
```

### Highlight an Element (Useful for Debugging)

```java
private void highlight(WebElement element) {
    JavascriptExecutor js = (JavascriptExecutor) driver;
    String originalStyle = element.getAttribute("style");
    js.executeScript(
        "arguments[0].setAttribute('style', 'border: 3px solid red; background: yellow;');",
        element
    );
    try { Thread.sleep(300); } catch (InterruptedException e) {}
    js.executeScript("arguments[0].setAttribute('style', '" + originalStyle + "');", element);
}

// Usage — highlight the login button for 300ms during debugging
highlight(driver.findElement(By.cssSelector("button[type='submit']")));
```

---

## Complete Test: Scrolling + JavascriptExecutor + localStorage

```java
@Test
public void testScrollingAndJavaScript() {
    JavascriptExecutor js = (JavascriptExecutor) driver;

    // ── 1. Verify no JWT token initially ────────────────────────────
    navigateTo("products");
    String token = (String) js.executeScript("return localStorage.getItem('token');");
    System.out.println("Token before login: " + token); // null

    // ── 2. Log in via UI ─────────────────────────────────────────────
    navigateTo("login");
    driver.findElement(By.cssSelector("[ng-model='loginData.username']")).sendKeys("admin");
    driver.findElement(By.cssSelector("[ng-model='loginData.password']")).sendKeys("admin123");
    driver.findElement(By.cssSelector("button[type='submit']")).click();

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    wait.until(ExpectedConditions.urlContains("admin/dashboard"));

    // ── 3. Verify JWT is in localStorage ────────────────────────────
    token = (String) js.executeScript("return localStorage.getItem('token');");
    Assert.assertNotNull(token, "JWT token should be stored after login");
    System.out.println("Token set: " + (token.startsWith("Bearer") ? "Bearer ..." : token));

    // ── 4. Scroll to bottom of dashboard ────────────────────────────
    js.executeScript("window.scrollTo(0, document.body.scrollHeight);");

    WebElement footer = driver.findElement(By.cssSelector("footer"));
    js.executeScript("arguments[0].scrollIntoView(true);", footer);
    Assert.assertTrue(footer.isDisplayed());
    System.out.println("Scrolled to footer: " + footer.getText());

    // ── 5. Scroll back to top ────────────────────────────────────────
    js.executeScript("window.scrollTo(0, 0);");
    System.out.println("Scrolled to top");

    // ── 6. Get page title via JS ─────────────────────────────────────
    String title = (String) js.executeScript("return document.title;");
    Assert.assertEquals(title, "ShopEasy - E-Commerce");

    // ── 7. Read current URL via JS ───────────────────────────────────
    String url = (String) js.executeScript("return window.location.href;");
    System.out.println("URL via JS: " + url);

    // ── 8. Simulate logout via localStorage ──────────────────────────
    js.executeScript("localStorage.clear();");
    driver.navigate().refresh();
    wait.until(ExpectedConditions.urlContains("login"));
    System.out.println("Redirected to login after localStorage clear");
}
```
