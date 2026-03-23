# 15 — Handle Mouse Events

## The `Actions` Class

Mouse events beyond simple `click()` require the **`Actions`** class from Selenium's interaction API. `Actions` builds a chain of actions and executes them all at once via `.perform()`.

```java
import org.openqa.selenium.interactions.Actions;

Actions actions = new Actions(driver);
```

---

## Mouse Event Methods

| Method | Description |
|---|---|
| `moveToElement(element)` | Hover over an element (triggers `mouseover`, `mouseenter`) |
| `click()` | Left-click at current mouse position |
| `click(element)` | Left-click a specific element |
| `doubleClick(element)` | Double-click a specific element |
| `contextClick(element)` | Right-click a specific element (opens context menu) |
| `clickAndHold(element)` | Press and hold left mouse button |
| `release()` | Release held mouse button |
| `dragAndDrop(source, target)` | Drag source element and drop on target |
| `dragAndDropBy(source, x, y)` | Drag source element by x,y offset |
| `moveByOffset(x, y)` | Move mouse by pixel offset from current position |
| `.perform()` | Execute all queued actions |

---

## 1. Hover — `moveToElement()`

Hovering reveals hidden menus, tooltips, or makes hidden buttons visible.

**ShopEasy — Hover over navbar Brand (to see tooltip) or over a product card:**

```java
Actions actions = new Actions(driver);

// Hover over the ShopEasy brand logo
WebElement brand = driver.findElement(By.cssSelector("a.navbar-brand"));
actions.moveToElement(brand).perform();
System.out.println("Hovering over brand");

// Hover over a product card — Bootstrap may show hover effects (shadow deepens)
navigateTo("products");
WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".product-card")));

WebElement firstCard = driver.findElement(By.cssSelector(".product-card"));
actions.moveToElement(firstCard).perform();
System.out.println("Hovering over product card");

// After hover, CSS class may change (e.g. Bootstrap adds shadow on hover)
String cssClass = firstCard.getAttribute("class");
System.out.println("Card classes after hover: " + cssClass);
```

**ShopEasy — Hover to reveal navbar dropdown:**

```java
// Hover over "Catalog" dropdown toggle (admin navbar) to open without clicking
WebElement catalogToggle = driver.findElement(
    By.xpath("//a[contains(@class,'dropdown-toggle') and contains(.,'Catalog')]")
);
actions.moveToElement(catalogToggle).perform();

WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
// Note: Bootstrap 5 dropdowns open on CLICK by default, not hover.
// CSS-based hover dropdowns would need: wait for dropdown-menu to show
```

---

## 2. Double Click — `doubleClick()`

```java
// Generic example — double click to edit a table cell inline
WebElement productNameCell = driver.findElement(
    By.xpath("//tbody/tr[1]/td[2]/strong")
);
actions.doubleClick(productNameCell).perform();
System.out.println("Double-clicked product name cell");

// ShopEasy — double click search input to select all text, then replace
WebElement searchInput = driver.findElement(
    By.cssSelector("[ng-model='searchQuery']")
);
actions.doubleClick(searchInput).perform();
// All text is now selected
searchInput.sendKeys("Laptop"); // replaces selected text
```

---

## 3. Right Click (Context Click) — `contextClick()`

```java
navigateTo("products");
WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".product-card")));

WebElement firstCard = driver.findElement(By.cssSelector(".product-card"));

// Right-click the product card
actions.contextClick(firstCard).perform();

// Browser shows native context menu (Open Link, Copy, Inspect...)
// To dismiss: press Escape
actions.sendKeys(org.openqa.selenium.Keys.ESCAPE).perform();
System.out.println("Context menu dismissed");
```

---

## 4. Click and Hold — `clickAndHold()` + `release()`

Used for drag-and-drop, slider interactions, and hold-to-confirm patterns.

```java
// Cart quantity + button — click and hold to rapidly increase (if supported)
WebElement plusBtn = driver.findElement(
    By.xpath("//div[contains(@class,'input-group-sm')]//button[text()='+']")
);
actions.clickAndHold(plusBtn).perform();
Thread.sleep(500); // hold for 500ms
actions.release(plusBtn).perform();
```

---

## 5. Drag and Drop — `dragAndDrop()`

Moves an element from one location to another.

```java
// Generic drag-and-drop
WebElement source = driver.findElement(By.id("draggableItem"));
WebElement target = driver.findElement(By.id("dropZone"));

actions.dragAndDrop(source, target).perform();
System.out.println("Dragged and dropped");

// Alternative — clickAndHold + moveToElement + release
actions.clickAndHold(source)
       .moveToElement(target)
       .release()
       .perform();
```

**dragAndDropBy — move by pixel offset:**

```java
WebElement slider = driver.findElement(By.cssSelector(".slider-handle"));
// Move slider 100 pixels to the right
actions.dragAndDropBy(slider, 100, 0).perform();
```

---

## 6. Move to Element with Offset

```java
// Move to element then offset from its center
WebElement card = driver.findElement(By.cssSelector(".product-card"));
actions.moveToElement(card, 10, 10).perform(); // 10px right and down from center
```

---

## 7. Chaining Multiple Actions

```java
// Chain: hover → wait → click
Actions actions = new Actions(driver);

WebElement catalogToggle = driver.findElement(By.cssSelector(".dropdown-toggle"));

actions
    .moveToElement(catalogToggle)  // hover
    .pause(Duration.ofMillis(500)) // wait 500ms
    .click()                       // click to open dropdown
    .perform();                    // execute all at once
```

---

## Complete Example: ShopEasy Mouse Events Test

```java
@Test
public void testMouseEventsOnShopEasy() {
    Actions actions = new Actions(driver);
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

    // ── 1. Hover over product card ─────────────────────────────────────
    navigateTo("products");
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".product-card")));

    List<WebElement> cards = driver.findElements(By.cssSelector(".product-card"));
    System.out.println("Found " + cards.size() + " product cards");

    if (!cards.isEmpty()) {
        WebElement firstCard = cards.get(0);
        String productName = firstCard.findElement(By.cssSelector("h6.card-title")).getText();
        System.out.println("Hovering over: " + productName);

        actions.moveToElement(firstCard).perform();

        // Bootstrap adds shadow on hover — verify via CSS
        String boxShadow = firstCard.getCssValue("box-shadow");
        System.out.println("Box shadow on hover: " + boxShadow);
    }

    // ── 2. Hover over navbar brand ─────────────────────────────────────
    WebElement brand = driver.findElement(By.cssSelector("a.navbar-brand"));
    actions.moveToElement(brand).perform();
    System.out.println("Hovering over ShopEasy brand: " + brand.getText());

    // ── 3. Move to search input and double-click ───────────────────────
    WebElement searchInput = driver.findElement(By.cssSelector("[ng-model='searchQuery']"));
    actions.moveToElement(searchInput).click().perform();  // move and click

    // Type in the search box
    searchInput.sendKeys("Test Product");
    String searchValue = searchInput.getAttribute("value");
    System.out.println("Typed into search: " + searchValue);

    // Double-click to select all, then type new value
    actions.doubleClick(searchInput).perform();
    searchInput.sendKeys("Laptop");
    System.out.println("Replaced search text with: Laptop");

    // ── 4. Right-click on product card ────────────────────────────────
    if (!cards.isEmpty()) {
        actions.contextClick(cards.get(0)).perform();
        System.out.println("Right-clicked on product card (native context menu shown)");
        // Dismiss context menu
        actions.sendKeys(org.openqa.selenium.Keys.ESCAPE).perform();
    }

    // ── 5. Scroll and hover over footer ───────────────────────────────
    WebElement footer = driver.findElement(By.cssSelector("footer"));
    actions.moveToElement(footer).perform();
    System.out.println("Moved to footer: " + footer.getText());
}

@Test
public void testCartQuantityButtons() {
    loginAsCustomer();
    // Add product to cart first (via API or by clicking Add to Cart)
    navigateTo("products");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    wait.until(ExpectedConditions.elementToBeClickable(
        By.cssSelector(".card-footer .btn-primary")
    ));
    driver.findElement(By.cssSelector(".card-footer .btn-primary")).click();

    navigateTo("cart");
    wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.cssSelector("table tbody tr")
    ));

    // Find the quantity + button in the first cart row
    WebElement plusBtn = driver.findElement(
        By.xpath("(//div[contains(@class,'input-group-sm')]//button[text()='+'])[1]")
    );
    WebElement minusBtn = driver.findElement(
        By.xpath("(//div[contains(@class,'input-group-sm')]//button[text()='-'])[1]")
    );
    WebElement qtyInput = driver.findElement(
        By.xpath("(//div[contains(@class,'input-group-sm')]//input[@type='number'])[1]")
    );

    int initialQty = Integer.parseInt(qtyInput.getAttribute("value"));
    System.out.println("Initial quantity: " + initialQty);

    // Click + button
    Actions actions = new Actions(driver);
    actions.moveToElement(plusBtn).click().perform();
    System.out.println("Clicked + button");

    // Click - button
    actions.moveToElement(minusBtn).click().perform();
    System.out.println("Clicked - button");
}
```

---

## Common Mouse Event Issues

| Issue | Cause | Fix |
|---|---|---|
| `MoveTargetOutOfBoundsException` | Element is not in viewport | Scroll element into view first (see 17-file-upload-scroll-js.md) |
| Hover doesn't trigger menu | Bootstrap dropdown opens on click | Use `.click()` instead of just hover |
| `dragAndDrop` doesn't work | HTML5 drag-and-drop not supported by Selenium's Actions | Use JS-based drag-and-drop script |
| Double-click not selecting text | Element not focused | Click first, then double-click |
| Context menu closes immediately | Something else captures the click | Add a `pause()` after `contextClick()` |

---

## HTML5 Drag-and-Drop (When `dragAndDrop` Fails)

Some frameworks intercept HTML5 drag events. Use JavaScript:

```java
JavascriptExecutor js = (JavascriptExecutor) driver;
String script = "var src=arguments[0],tgt=arguments[1];" +
    "var dnd=new DataTransfer();" +
    "src.dispatchEvent(new DragEvent('dragstart',{dataTransfer:dnd,bubbles:true}));" +
    "tgt.dispatchEvent(new DragEvent('dragover',{dataTransfer:dnd,bubbles:true}));" +
    "tgt.dispatchEvent(new DragEvent('drop',{dataTransfer:dnd,bubbles:true}));" +
    "src.dispatchEvent(new DragEvent('dragend',{dataTransfer:dnd,bubbles:true}));";

js.executeScript(script, sourceElement, targetElement);
```
