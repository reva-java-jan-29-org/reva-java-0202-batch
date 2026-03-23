# 16 — Handle Keyboard Events, Tabs & Windows

## Part A: Keyboard Events

## The `Keys` Class

Selenium's `Keys` class provides constants for all special keyboard keys. Use with `sendKeys()` on elements or via the `Actions` class.

```java
import org.openqa.selenium.Keys;
```

---

## Common `Keys` Constants

| Key Constant | Keyboard Key |
|---|---|
| `Keys.ENTER` | Enter / Return |
| `Keys.TAB` | Tab (move to next field) |
| `Keys.ESCAPE` / `Keys.ESCAPE` | Escape |
| `Keys.BACK_SPACE` | Backspace |
| `Keys.DELETE` | Delete |
| `Keys.SPACE` | Space bar |
| `Keys.ARROW_UP/DOWN/LEFT/RIGHT` | Arrow keys |
| `Keys.HOME` / `Keys.END` | Home / End |
| `Keys.PAGE_UP` / `Keys.PAGE_DOWN` | Page Up / Down |
| `Keys.F5` | F5 (refresh) |
| `Keys.CONTROL + "a"` | Ctrl+A (select all) |
| `Keys.CONTROL + "c"` | Ctrl+C (copy) |
| `Keys.CONTROL + "v"` | Ctrl+V (paste) |
| `Keys.CONTROL + "t"` | Ctrl+T (new tab) |
| `Keys.SHIFT + Keys.TAB` | Shift+Tab (previous field) |

---

## 1. Enter Key — Submit Forms

```java
navigateTo("login");

WebElement usernameInput = driver.findElement(
    By.cssSelector("[ng-model='loginData.username']")
);
WebElement passwordInput = driver.findElement(
    By.cssSelector("[ng-model='loginData.password']")
);

usernameInput.sendKeys("admin");
passwordInput.sendKeys("admin123");

// Submit form using Enter key instead of clicking the button
passwordInput.sendKeys(Keys.ENTER);

WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
wait.until(ExpectedConditions.urlContains("admin/dashboard"));
System.out.println("Logged in via Enter key");
```

---

## 2. Tab Key — Navigate Between Form Fields

ShopEasy's register form has 6 fields. Tab through them:

```java
navigateTo("register");

WebElement usernameField = driver.findElement(
    By.cssSelector("[ng-model='registerData.username']")
);
usernameField.sendKeys("newuser");

// Press Tab to move to next field (First Name)
usernameField.sendKeys(Keys.TAB);

// Now focus is on first name — sendKeys on active element
driver.switchTo().activeElement().sendKeys("John");

// Tab to Last Name
driver.switchTo().activeElement().sendKeys(Keys.TAB);
driver.switchTo().activeElement().sendKeys("Doe");

// Tab to Mobile
driver.switchTo().activeElement().sendKeys(Keys.TAB);
driver.switchTo().activeElement().sendKeys("9876543210");

// Tab to Password
driver.switchTo().activeElement().sendKeys(Keys.TAB);
driver.switchTo().activeElement().sendKeys("secret123");

// Tab to Confirm Password
driver.switchTo().activeElement().sendKeys(Keys.TAB);
driver.switchTo().activeElement().sendKeys("secret123");

// Press Enter to submit
driver.switchTo().activeElement().sendKeys(Keys.ENTER);
System.out.println("Registration submitted via keyboard navigation");
```

---

## 3. Keyboard Shortcuts in Search

```java
navigateTo("products");
WebElement searchInput = driver.findElement(
    By.cssSelector("[ng-model='searchQuery']")
);

// Type a search term
searchInput.sendKeys("Laptop");

// Press Enter to trigger search (ng-keyup="$event.keyCode === 13 && search()")
searchInput.sendKeys(Keys.ENTER);

WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".product-card")));

// Clear search: Ctrl+A then Delete
searchInput.sendKeys(Keys.CONTROL + "a"); // Select all
searchInput.sendKeys(Keys.DELETE);        // Delete selection
System.out.println("Search cleared via Ctrl+A + Delete");

// Or use element.clear()
searchInput.clear();
```

---

## 4. Keyboard Events via Actions

Use `Actions` for complex key combinations:

```java
Actions actions = new Actions(driver);

// Ctrl+A to select all text in an input
WebElement input = driver.findElement(By.cssSelector("[ng-model='loginData.username']"));
input.click();
actions.keyDown(Keys.CONTROL).sendKeys("a").keyUp(Keys.CONTROL).perform();

// Ctrl+C to copy, Ctrl+V to paste
actions.keyDown(Keys.CONTROL).sendKeys("c").keyUp(Keys.CONTROL).perform();
actions.keyDown(Keys.CONTROL).sendKeys("v").keyUp(Keys.CONTROL).perform();

// Shift + Tab to go to previous field
actions.keyDown(Keys.SHIFT).sendKeys(Keys.TAB).keyUp(Keys.SHIFT).perform();
```

---

## Part B: Multiple Tabs

## Opening and Switching Between Browser Tabs

Each browser tab has a unique **window handle** (a string identifier). Use `driver.getWindowHandle()` for the current tab and `driver.getWindowHandles()` for all open tabs.

---

## Opening a New Tab

```java
// Method 1: Keyboard shortcut (Ctrl+T)
driver.findElement(By.cssSelector("body")).sendKeys(Keys.CONTROL + "t");

// Method 2: JavaScript (Selenium 4 preferred)
((JavascriptExecutor) driver).executeScript("window.open('about:blank', '_blank');");

// Method 3: Selenium 4 — newWindow
driver.switchTo().newWindow(WindowType.TAB);
```

---

## Switching Between Tabs

```java
// Get current window handle (tab 1)
String originalTab = driver.getWindowHandle();
System.out.println("Original tab: " + originalTab);

// Open new tab via JS
((JavascriptExecutor) driver).executeScript("window.open();");

// Get all window handles
Set<String> allTabs = driver.getWindowHandles();
System.out.println("Total tabs: " + allTabs.size()); // 2

// Switch to the NEW tab
for (String tab : allTabs) {
    if (!tab.equals(originalTab)) {
        driver.switchTo().window(tab);
        break;
    }
}

// Navigate in new tab
driver.get(BASE_URL + "/#!/register");
System.out.println("New tab URL: " + driver.getCurrentUrl());

// Close new tab
driver.close();

// Switch back to original tab
driver.switchTo().window(originalTab);
System.out.println("Back to original tab: " + driver.getCurrentUrl());
```

---

## ShopEasy Multi-Tab Scenario

Test that "Register here" link on the login page can open in a new tab (Ctrl+Click):

```java
@Test
public void openRegisterInNewTab() {
    navigateTo("login");

    String originalTab = driver.getWindowHandle();

    // Ctrl+Click opens link in new tab
    WebElement registerLink = driver.findElement(By.linkText("Register here"));
    Actions actions = new Actions(driver);
    actions.keyDown(Keys.CONTROL).click(registerLink).keyUp(Keys.CONTROL).perform();

    // Wait for new tab to open
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(d -> d.getWindowHandles().size() > 1);

    // Switch to new tab
    Set<String> allTabs = driver.getWindowHandles();
    for (String tab : allTabs) {
        if (!tab.equals(originalTab)) {
            driver.switchTo().window(tab);
            break;
        }
    }

    // Verify new tab has register page
    wait.until(ExpectedConditions.urlContains("register"));
    Assert.assertTrue(driver.getCurrentUrl().contains("register"),
        "New tab should show register page");
    System.out.println("New tab URL: " + driver.getCurrentUrl());

    // Close new tab and return to original
    driver.close();
    driver.switchTo().window(originalTab);
    Assert.assertTrue(driver.getCurrentUrl().contains("login"),
        "Original tab should still show login page");
}
```

---

## Part C: Multiple Windows

A **new window** works exactly like a new tab in terms of window handles. The only difference is presentation (separate window vs tab).

```java
// Open new window
driver.switchTo().newWindow(WindowType.WINDOW);
driver.get(BASE_URL + "/#!/products");

// Work in the new window
String productTitle = driver.findElement(By.cssSelector("h2")).getText();
System.out.println("Products page title: " + productTitle);

// Get all windows
Set<String> windows = driver.getWindowHandles();
System.out.println("Total windows: " + windows.size());

// Close current window and switch back
driver.close();
driver.switchTo().window(windows.iterator().next());
```

---

## WindowHelper Utility Class

```java
public class WindowHelper {

    private WebDriver driver;

    public WindowHelper(WebDriver driver) {
        this.driver = driver;
    }

    /** Switch to tab/window at the given index (0 = first/original) */
    public void switchToWindowByIndex(int index) {
        List<String> handles = new ArrayList<>(driver.getWindowHandles());
        if (index < handles.size()) {
            driver.switchTo().window(handles.get(index));
        }
    }

    /** Switch to window whose title contains the given text */
    public boolean switchToWindowByTitle(String titleFragment) {
        String currentHandle = driver.getWindowHandle();
        for (String handle : driver.getWindowHandles()) {
            driver.switchTo().window(handle);
            if (driver.getTitle().contains(titleFragment)) {
                return true;
            }
        }
        // Didn't find it — return to original
        driver.switchTo().window(currentHandle);
        return false;
    }

    /** Switch to window whose URL contains the given text */
    public boolean switchToWindowByUrl(String urlFragment) {
        String currentHandle = driver.getWindowHandle();
        for (String handle : driver.getWindowHandles()) {
            driver.switchTo().window(handle);
            if (driver.getCurrentUrl().contains(urlFragment)) {
                return true;
            }
        }
        driver.switchTo().window(currentHandle);
        return false;
    }

    /** Close all windows except the first one */
    public void closeAllExceptFirst() {
        List<String> handles = new ArrayList<>(driver.getWindowHandles());
        for (int i = 1; i < handles.size(); i++) {
            driver.switchTo().window(handles.get(i));
            driver.close();
        }
        driver.switchTo().window(handles.get(0));
    }
}

// Usage
WindowHelper wh = new WindowHelper(driver);
wh.switchToWindowByIndex(1);       // Switch to second tab
wh.switchToWindowByTitle("ShopEasy"); // Switch by title
wh.closeAllExceptFirst();          // Clean up extra tabs
```

---

## Complete Test: Keyboard + Multi-Tab

```java
@Test
public void fullKeyboardAndTabTest() {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    Actions actions = new Actions(driver);

    // ── 1. Fill login form with Tab navigation ──────────────────────
    navigateTo("login");

    // Click username field
    WebElement usernameField = wait.until(
        ExpectedConditions.elementToBeClickable(
            By.cssSelector("[ng-model='loginData.username']")
        )
    );
    usernameField.click();
    usernameField.sendKeys("admin");

    // Tab to password field
    usernameField.sendKeys(Keys.TAB);
    driver.switchTo().activeElement().sendKeys("admin123");

    // Submit with Enter
    driver.switchTo().activeElement().sendKeys(Keys.ENTER);
    wait.until(ExpectedConditions.urlContains("admin/dashboard"));
    System.out.println("1. Logged in via keyboard");

    // ── 2. Open admin/products in new tab ───────────────────────────
    String originalTab = driver.getWindowHandle();
    ((JavascriptExecutor) driver).executeScript("window.open();");
    wait.until(d -> d.getWindowHandles().size() > 1);

    // Switch to new tab
    for (String tab : driver.getWindowHandles()) {
        if (!tab.equals(originalTab)) {
            driver.switchTo().window(tab);
            break;
        }
    }

    driver.get(BASE_URL + "/#!/admin/products");
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("table tbody tr")));
    System.out.println("2. Opened admin products in new tab: " + driver.getCurrentUrl());

    // ── 3. Use keyboard shortcut to search in table ──────────────────
    // Press Ctrl+F to open browser find (not Selenium-controllable)
    // Instead: use the table search if available, or Ctrl+A on a field
    WebElement addProductBtn = driver.findElement(
        By.cssSelector("button[ng-click='newProduct()']")
    );
    // Tab to the button from the search field (keyboard nav)
    actions.sendKeys(Keys.TAB).perform(); // move focus

    // ── 4. Close new tab and return ─────────────────────────────────
    driver.close();
    driver.switchTo().window(originalTab);
    Assert.assertTrue(driver.getCurrentUrl().contains("admin/dashboard"),
        "Should be back on dashboard in original tab");
    System.out.println("3. Back on original tab: " + driver.getCurrentUrl());
}
```
