# 20 — Selenium Interview Questions & Answers

## Category 1: Selenium Basics & Architecture

**Q1. What is Selenium WebDriver and how does it differ from Selenium RC?**

Selenium WebDriver communicates directly with the browser using the browser's native support (W3C WebDriver protocol). Selenium RC used a server proxy to inject JavaScript. WebDriver is faster, more reliable, and supports modern browsers without a separate server.

---

**Q2. What is the W3C WebDriver protocol?**

A standardized HTTP-based protocol defined by W3C for browser automation. WebDriver clients (your Java code) send HTTP requests to a browser driver (ChromeDriver), which translates them into browser-specific commands. All major browsers implement this standard — it replaced the proprietary JSONWireProtocol in Selenium 4.

---

**Q3. What is Selenium Manager?**

Introduced in Selenium 4.6+, Selenium Manager is a built-in tool that automatically downloads the correct browser driver (e.g., ChromeDriver) matching your installed browser version. You no longer need to manually manage driver binaries or use WebDriverManager.

---

**Q4. What are the main components of Selenium Suite?**

- **Selenium WebDriver** — Browser automation API
- **Selenium Grid** — Distributed test execution across multiple machines/browsers
- **Selenium IDE** — Browser extension for recording/playback (Firefox/Chrome plugin)

---

**Q5. What is the difference between `driver.close()` and `driver.quit()`?**

- `driver.close()` — Closes the currently focused browser window. If multiple windows are open, others remain open. Does NOT kill the ChromeDriver process.
- `driver.quit()` — Closes ALL browser windows AND kills the WebDriver (ChromeDriver) process entirely. Always use in `@AfterMethod`.

---

## Category 2: Locators

**Q6. List all types of locators in Selenium. Which is fastest?**

`By.id`, `By.name`, `By.className`, `By.tagName`, `By.linkText`, `By.partialLinkText`, `By.cssSelector`, `By.xpath`.

**Fastest:** `By.id` — browsers have native `getElementById()` optimized at the engine level.
**Most flexible:** `By.xpath` — can traverse the DOM in any direction.
**Recommended for real apps:** `By.cssSelector` — fast, readable, handles complex cases.

---

**Q7. What is the difference between `By.linkText` and `By.partialLinkText`?**

- `By.linkText("Register here")` — exact, case-sensitive full match of visible link text
- `By.partialLinkText("Register")` — matches if visible text *contains* the string

---

**Q8. Write a CSS selector to find the username input on ShopEasy's login page.**

```css
input[ng-model='loginData.username']
/* or */
input[placeholder='Enter your username']
/* or */
form[name='loginForm'] input[type='text']
```

---

**Q9. Write an XPath to find a table row containing the product "Laptop Pro".**

```xpath
//table/tbody/tr[.//strong[contains(text(),'Laptop Pro')]]
```

---

**Q10. What is the difference between absolute XPath and relative XPath?**

- **Absolute:** Starts from root `/html/body/...` — fragile, breaks with any DOM change
- **Relative:** Starts with `//` — searches anywhere in DOM — robust and preferred

---

**Q11. What are XPath axes? Give 3 examples.**

Axes define directional relationships in the DOM:
- `ancestor::div` — all `<div>` parents/grandparents above current node
- `following-sibling::td` — all `<td>` siblings that come after current node
- `descendant::input` — all `<input>` elements anywhere inside current node

---

**Q12. What is the difference between `findElement()` and `findElements()`?**

| | `findElement()` | `findElements()` |
|---|---|---|
| Returns | Single `WebElement` | `List<WebElement>` |
| If not found | Throws `NoSuchElementException` | Returns empty list (never throws) |
| Use when | Expecting exactly one element | Expecting zero or more elements |

---

## Category 3: WebDriver Methods

**Q13. How do you check if an element is present on the page without throwing an exception?**

```java
List<WebElement> elements = driver.findElements(By.cssSelector(".some-class"));
boolean isPresent = !elements.isEmpty();
```
`findElements()` returns an empty list instead of throwing an exception.

---

**Q14. What is the difference between `isDisplayed()`, `isEnabled()`, and `isSelected()`?**

- `isDisplayed()` — Is the element visible? (CSS `display:none` or `visibility:hidden` → false)
- `isEnabled()` — Can the element be interacted with? (HTML `disabled` attribute → false)
- `isSelected()` — Is a checkbox/radio/option currently checked/selected?

---

**Q15. How do you get the value of a text input field in Selenium?**

```java
String value = element.getAttribute("value");
```
`getText()` returns visible text (not applicable to input values). `getAttribute("value")` returns what was typed into the field.

---

**Q16. What does `getPageSource()` return in an AngularJS app?**

It returns the current DOM as rendered by the browser, including dynamically generated HTML from `ng-repeat`, `ng-show`, etc. It does NOT return the original static HTML template.

---

## Category 4: Waits

**Q17. What are the three types of waits in Selenium? Which is recommended?**

1. **Implicit Wait** — Global polling on every `findElement()` until element found or timeout
2. **Explicit Wait** (`WebDriverWait` + `ExpectedConditions`) — Wait for a specific condition per element. **Recommended.**
3. **Fluent Wait** — Custom polling interval + ignore specific exceptions + custom conditions

---

**Q18. Why should you not mix implicit and explicit waits?**

Mixing causes unpredictable behavior. When both are active, the actual wait time becomes the sum or max of both values. For example, if implicit wait is 10s and explicit wait is 15s, an element search might wait up to 25s. Use explicit waits only.

---

**Q19. What is `ExpectedConditions.stalenessOf()` used for?**

It waits until the given `WebElement` reference becomes stale (removed from DOM). Used after actions that remove an element — like after deleting a table row — to confirm the deletion happened.

```java
WebElement row = driver.findElement(By.cssSelector("tr.first-row"));
deleteBtn.click();
wait.until(ExpectedConditions.stalenessOf(row)); // confirm row removed
```

---

**Q20. Why is `Thread.sleep()` considered bad practice?**

- Wastes fixed time even if the element appears faster
- Test still fails if the element appears slower than the sleep duration
- Multiplied across hundreds of tests, it makes the suite slow
- Masks real performance problems
Use `WebDriverWait` with `ExpectedConditions` instead.

---

**Q21. How do you wait for an AngularJS page to finish loading in Selenium?**

AngularJS uses asynchronous `$http` calls. Strategies:
1. Wait for the loading spinner to disappear: `ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".spinner-border"))`
2. Wait for elements to appear: `ExpectedConditions.numberOfElementsToBeMoreThan(By.cssSelector(".product-card"), 0)`
3. Wait for URL to change after navigation: `ExpectedConditions.urlContains("admin/dashboard")`

---

## Category 5: Interactions

**Q22. How do you handle a `<select>` dropdown in Selenium?**

Use the `Select` class:
```java
Select select = new Select(driver.findElement(By.cssSelector("select[ng-model='selectedCategory']")));
select.selectByVisibleText("Electronics");
select.selectByIndex(0);
select.selectByValue("electronics");
String selected = select.getFirstSelectedOption().getText();
List<WebElement> allOptions = select.getOptions();
```

---

**Q23. What is the difference between `click()` on an element vs `Actions.click(element)`?**

- `element.click()` — Direct WebDriver click via W3C protocol
- `actions.click(element).perform()` — Simulates mouse movement to element then click (more realistic, handles hover-triggered scenarios)

---

**Q24. How do you perform drag and drop in Selenium?**

```java
Actions actions = new Actions(driver);
actions.dragAndDrop(sourceElement, targetElement).perform();

// Or:
actions.clickAndHold(source).moveToElement(target).release().perform();
```

---

**Q25. How do you handle JavaScript alerts/confirms in Selenium?**

```java
Alert alert = driver.switchTo().alert();
String text = alert.getText();   // read message
alert.accept();                  // click OK
alert.dismiss();                 // click Cancel
alert.sendKeys("text");         // type (for prompt)
```

---

**Q26. How do you switch between multiple browser tabs?**

```java
String original = driver.getWindowHandle();
Set<String> allHandles = driver.getWindowHandles();
for (String handle : allHandles) {
    if (!handle.equals(original)) {
        driver.switchTo().window(handle);
        break;
    }
}
// Return to original
driver.switchTo().window(original);
```

---

**Q27. How do you interact with iFrames?**

```java
// Switch to frame
driver.switchTo().frame("frameNameOrId");
driver.switchTo().frame(0);                  // by index
driver.switchTo().frame(frameWebElement);    // by element

// Return to main document
driver.switchTo().defaultContent();
driver.switchTo().parentFrame(); // go up one level
```

---

## Category 6: Screenshots, Headless, SSL

**Q28. How do you take a screenshot in Selenium?**

```java
TakesScreenshot ts = (TakesScreenshot) driver;
File screenshot = ts.getScreenshotAs(OutputType.FILE);
Files.copy(screenshot.toPath(), Paths.get("screenshot.png"));

// Selenium 4: Screenshot of a specific element
File elementShot = element.getScreenshotAs(OutputType.FILE);
```

---

**Q29. How do you run Selenium tests in headless mode?**

```java
ChromeOptions options = new ChromeOptions();
options.addArguments("--headless=new");   // Chrome 112+
options.addArguments("--window-size=1920,1080");
options.addArguments("--no-sandbox");
options.addArguments("--disable-dev-shm-usage");
driver = new ChromeDriver(options);
```

---

**Q30. How do you handle SSL certificate errors?**

```java
ChromeOptions options = new ChromeOptions();
options.setAcceptInsecureCerts(true); // Accept all SSL certs
driver = new ChromeDriver(options);
```

---

## Category 7: Advanced Topics

**Q31. What is JavascriptExecutor and when do you use it?**

`JavascriptExecutor` is a Selenium interface for executing JavaScript in the browser context. Use it when:
- Normal `click()` fails due to overlapping elements: `js.executeScript("arguments[0].click();", element)`
- `sendKeys()` doesn't work (blocked inputs): `js.executeScript("arguments[0].value='x';", input)`
- Scrolling: `js.executeScript("window.scrollTo(0, 500);")`
- Reading localStorage: `js.executeScript("return localStorage.getItem('token');")`
- Highlighting elements during debugging

---

**Q32. What is a StaleElementReferenceException? How do you fix it?**

Occurs when you hold a reference to a `WebElement` but the DOM changes — the element is no longer attached to the page document (it was re-rendered, removed, or the page was refreshed).

**Fix:**
1. Re-find the element after any DOM-changing action
2. Use `ExpectedConditions.stalenessOf()` to wait for staleness, then re-find
3. Use Page Object Model patterns where elements are found fresh on each method call

---

**Q33. What is Shadow DOM and how do you interact with it in Selenium 4?**

Shadow DOM encapsulates HTML/CSS/JS within a web component, isolated from the main document. Regular `findElement()` cannot reach inside it.

```java
WebElement host = driver.findElement(By.cssSelector("custom-element"));
SearchContext shadowRoot = host.getShadowRoot(); // Selenium 4
WebElement inputInShadow = shadowRoot.findElement(By.cssSelector("input"));
```

---

**Q34. How do you handle file upload in Selenium?**

For `<input type="file">`:
```java
WebElement fileInput = driver.findElement(By.cssSelector("input[type='file']"));
fileInput.sendKeys("/absolute/path/to/file.jpg"); // No click needed
```
For hidden file inputs, make them visible first via JavascriptExecutor.

---

**Q35. What is the Page Object Model (POM)?**

POM is a design pattern where each web page has a corresponding Java class. The class contains:
- WebElement locators (as fields)
- Methods representing actions on that page

Benefits: code reuse, readability, easy maintenance. When the UI changes, only the Page Object needs updating.

```java
public class LoginPage {
    private WebDriver driver;

    private By usernameInput = By.cssSelector("[ng-model='loginData.username']");
    private By passwordInput = By.cssSelector("[ng-model='loginData.password']");
    private By loginButton   = By.cssSelector("button[type='submit']");

    public LoginPage(WebDriver driver) { this.driver = driver; }

    public void login(String username, String password) {
        driver.findElement(usernameInput).sendKeys(username);
        driver.findElement(passwordInput).sendKeys(password);
        driver.findElement(loginButton).click();
    }
}
```

---

**Q36. How do you handle a Bootstrap modal dialog (not an alert)?**

Bootstrap modals are regular DOM elements — no `switchTo()` needed. Wait for visibility and interact directly:
```java
wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("productModal")));
driver.findElement(By.cssSelector("#productModal input[ng-model='productForm.name']"))
      .sendKeys("New Product");
```

---

**Q37. What is the difference between `getText()` and `getAttribute("value")`?**

| Method | Returns | Use For |
|---|---|---|
| `getText()` | Visible text content of element | `<span>`, `<p>`, `<h1>`, `<button>`, `<td>` |
| `getAttribute("value")` | `value` attribute of form element | `<input>`, `<textarea>`, `<select>` |

---

**Q38. How do you find broken links using Selenium?**

```java
List<WebElement> links = driver.findElements(By.tagName("a"));
for (WebElement link : links) {
    String href = link.getAttribute("href");
    if (href != null && href.startsWith("http")) {
        HttpURLConnection conn = (HttpURLConnection) new URL(href).openConnection();
        conn.setRequestMethod("HEAD");
        conn.connect();
        int code = conn.getResponseCode();
        if (code >= 400) {
            System.out.println("Broken: " + href + " [" + code + "]");
        }
    }
}
```

---

**Q39. What is `scrollIntoView` and when do you use it?**

```java
js.executeScript("arguments[0].scrollIntoView(true);", element);
```
Use it before interacting with elements that are below the viewport. Prevents `ElementNotInteractableException` caused by elements being off-screen.

---

**Q40. How do you verify a table is sorted correctly?**

```java
List<WebElement> cells = driver.findElements(By.cssSelector("tbody td:nth-child(2)"));
List<String> actualValues = cells.stream().map(WebElement::getText).collect(Collectors.toList());
List<String> sortedValues = new ArrayList<>(actualValues);
Collections.sort(sortedValues);
Assert.assertEquals(actualValues, sortedValues, "Table should be sorted alphabetically");
```

---

## Quick Tips for Interviews

1. **Always explain WHY, not just WHAT** — "I use explicit wait because implicit wait doesn't handle visibility conditions in AngularJS apps"
2. **Mention Page Object Model** — Shows you think about maintainability
3. **Know your locator preference** — CSS > XPath for most cases, ID when available
4. **Understand SPA challenges** — Waits, dynamic DOM, hash routing
5. **Mention test design** — `@BeforeMethod` setup, `@AfterMethod` teardown with screenshot on failure
