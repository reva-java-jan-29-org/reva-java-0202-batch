# 11 — Handle Different Types of Dropdowns

## Types of Dropdowns in Web Apps

| Type | HTML | Selenium Approach |
|---|---|---|
| **Native HTML `<select>`** | `<select><option>...</option></select>` | `Select` class |
| **Bootstrap dropdown** | `<div class="dropdown">` + JS | Click the toggle, click the item |
| **AngularJS custom dropdown** | `ng-select` or custom directive | Click-based interaction |

---

## Part A: Native `<select>` Dropdown — The `Select` Class

The `Select` class (from `org.openqa.selenium.support.ui`) provides dedicated methods for HTML `<select>` elements.

### Import

```java
import org.openqa.selenium.support.ui.Select;
```

### Create a Select Instance

```java
WebElement selectElement = driver.findElement(By.cssSelector("select[ng-model='selectedCategory']"));
Select select = new Select(selectElement);
```

---

## Select Methods

### 1. `selectByVisibleText(text)` — Most Readable

Selects the option whose visible text matches exactly.

```java
Select categorySelect = new Select(
    driver.findElement(By.cssSelector("select[ng-model='selectedCategory']"))
);
categorySelect.selectByVisibleText("Electronics");
```

### 2. `selectByValue(value)` — By `value` Attribute

Selects the option whose `value` attribute matches.

```html
<option value="electronics">Electronics</option>
```
```java
categorySelect.selectByValue("electronics");
```

### 3. `selectByIndex(index)` — By Position (0-based)

```java
categorySelect.selectByIndex(0); // First option
categorySelect.selectByIndex(2); // Third option
```

### 4. `getOptions()` — Get All Options

```java
List<WebElement> options = categorySelect.getOptions();
System.out.println("Total categories: " + options.size());
for (WebElement opt : options) {
    System.out.println("  " + opt.getText() + " [value=" + opt.getAttribute("value") + "]");
}
```

### 5. `getFirstSelectedOption()` — Get Currently Selected

```java
WebElement selected = categorySelect.getFirstSelectedOption();
System.out.println("Currently selected: " + selected.getText());
```

### 6. `getAllSelectedOptions()` — For Multi-Select

Only relevant for `<select multiple>`:

```java
List<WebElement> selected = categorySelect.getAllSelectedOptions();
```

### 7. `isMultiple()` — Check If Multi-Select

```java
boolean isMulti = categorySelect.isMultiple(); // false for ShopEasy's category dropdown
```

---

## ShopEasy — Category Dropdown on Products Page

```html
<!-- products.html -->
<select class="form-select form-select-lg"
        ng-model="selectedCategory"
        ng-change="filterByCategory(selectedCategory)">
    <option ng-repeat="cat in categories" ng-value="cat">{{cat}}</option>
</select>
```

The options are dynamically rendered via `ng-repeat` from the `categories` array. Available options are populated from the product data returned by the API.

```java
@Test
public void filterProductsByCategory() {
    navigateTo("products");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

    // Wait for products and category dropdown to load
    wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
        By.cssSelector(".product-card"), 0
    ));

    // Get the category dropdown
    WebElement categoryDropdown = wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("select[ng-model='selectedCategory']")
        )
    );
    Select select = new Select(categoryDropdown);

    // Print all available categories
    List<WebElement> options = select.getOptions();
    System.out.println("Available categories:");
    for (WebElement opt : options) {
        System.out.println("  → " + opt.getText());
    }

    // Get the count of products before filtering
    int totalProducts = driver.findElements(By.cssSelector(".product-card")).size();
    System.out.println("Total products: " + totalProducts);

    // Get first non-"All" category
    String firstCategory = options.size() > 1 ? options.get(1).getText() : options.get(0).getText();

    // Select by visible text
    select.selectByVisibleText(firstCategory);

    // Wait for filter to apply (AngularJS updates ng-repeat)
    // The filtered list should have <= totalProducts
    wait.until(d -> {
        int currentCount = d.findElements(By.cssSelector(".product-card")).size();
        return currentCount < totalProducts || currentCount == totalProducts;
        // If all products are in this category, count stays same
    });

    // Verify selected option
    String selectedText = select.getFirstSelectedOption().getText();
    Assert.assertEquals(selectedText, firstCategory,
        "Selected category should match what we selected");

    System.out.println("Filtered by: " + selectedText);
    System.out.println("Products shown: " + driver.findElements(By.cssSelector(".product-card")).size());
}
```

---

## Iterating All Categories and Testing Each

```java
@Test
public void testAllCategoryFilters() {
    navigateTo("products");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
        By.cssSelector(".product-card"), 0
    ));

    Select select = new Select(
        driver.findElement(By.cssSelector("select[ng-model='selectedCategory']"))
    );

    List<WebElement> options = select.getOptions();

    for (int i = 0; i < options.size(); i++) {
        // Re-find select each iteration (stale element prevention)
        Select freshSelect = new Select(
            driver.findElement(By.cssSelector("select[ng-model='selectedCategory']"))
        );
        freshSelect.selectByIndex(i);

        String categoryName = freshSelect.getFirstSelectedOption().getText();
        System.out.println("Testing category[" + i + "]: " + categoryName);

        // Small delay for AngularJS to apply filter (or use explicit wait)
        // In production tests, use explicit wait instead
        List<WebElement> visibleProducts = driver.findElements(By.cssSelector(".product-card"));
        System.out.println("  Products found: " + visibleProducts.size());
    }
}
```

---

## Part B: Bootstrap Dropdown (Navbar)

ShopEasy's navbar has Bootstrap dropdown menus (Catalog, Users):

```html
<li class="nav-item dropdown">
    <a class="nav-link dropdown-toggle" href="#" data-bs-toggle="dropdown">
        <i class="bi bi-box-seam"></i> Catalog
    </a>
    <ul class="dropdown-menu">
        <li><a class="dropdown-item" href="#!/products">Browse Products</a></li>
        <li><a class="dropdown-item" href="#!/admin/products">Manage Products</a></li>
    </ul>
</li>
```

Bootstrap dropdowns are **not** `<select>` elements — they're click-toggle `<ul>/<li>` menus.

```java
@Test
public void navigateViaCatalogDropdown() {
    // Login as admin first to see the admin navbar
    loginAsAdmin();

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

    // Step 1: Click the "Catalog" dropdown toggle
    WebElement catalogDropdown = wait.until(
        ExpectedConditions.elementToBeClickable(
            By.xpath("//a[contains(@class,'dropdown-toggle')][.//i[contains(@class,'bi-box-seam')]]")
        )
    );
    catalogDropdown.click();

    // Step 2: Wait for dropdown menu to appear
    WebElement dropdownMenu = wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector(".dropdown-menu.show")
        )
    );

    // Step 3: Click "Manage Products" item
    WebElement manageProducts = dropdownMenu.findElement(
        By.xpath(".//a[contains(text(),'Manage Products')]")
    );
    manageProducts.click();

    // Step 4: Verify navigation
    wait.until(ExpectedConditions.urlContains("admin/products"));
    Assert.assertTrue(driver.getCurrentUrl().contains("admin/products"));
}
```

---

## Part C: AngularJS `ng-select` / Custom Dropdown

Some AngularJS apps use custom components. The approach is the same as Bootstrap dropdown — click to open, click the item.

```java
// Generic custom dropdown pattern:
// 1. Click the trigger to open
WebElement trigger = driver.findElement(By.cssSelector(".custom-dropdown-trigger"));
trigger.click();

// 2. Wait for options to appear
WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
wait.until(ExpectedConditions.visibilityOfElementLocated(
    By.cssSelector(".custom-dropdown-options")
));

// 3. Click the desired option
driver.findElement(By.xpath(
    "//div[contains(@class,'custom-dropdown-options')]//div[text()='Electronics']"
)).click();
```

---

## Deselect Methods (Multi-Select Only)

```java
// Only works if select.isMultiple() == true
select.deselectAll();
select.deselectByVisibleText("Electronics");
select.deselectByValue("electronics");
select.deselectByIndex(0);
```

---

## Common Dropdown Errors

| Error | Cause | Fix |
|---|---|---|
| `UnexpectedTagNameException` | `new Select()` called on a non-`<select>` element | Verify the element is `<select>` |
| `NoSuchElementException` | Option text not in dropdown | Print all options with `getOptions()` to debug |
| Options list empty | AngularJS hasn't rendered `ng-repeat` yet | Wait for options to appear before creating Select |
| `StaleElementReferenceException` | Dropdown re-rendered after selection | Re-find the `<select>` element |

### Fix: Wait for AngularJS to Populate Options

```java
// Wait until the dropdown has more than just the default option
WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
wait.until(d -> {
    Select s = new Select(d.findElement(By.cssSelector("select[ng-model='selectedCategory']")));
    return s.getOptions().size() > 1;
});
```
