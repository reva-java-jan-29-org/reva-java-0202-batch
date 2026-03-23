# 13 — Handle Dynamic & Pagination Web Tables

## What Makes a Table Dynamic?

A **dynamic table** differs from static tables in one or more of these ways:

| Feature | Static Table | Dynamic Table |
|---|---|---|
| Data loading | All loaded at once | Loaded via AJAX after page load |
| Row count | Fixed | Changes with filters, actions, API responses |
| Pagination | No | Yes — data split across pages |
| Sorting | No/server-side | Click column header to sort |
| Search/filter | No | Yes — rows filter in real-time |
| DOM refresh | Never | Rows added/removed without full page reload |

---

## ShopEasy's Dynamic Tables

All of ShopEasy's tables are dynamic because:
1. Data is fetched via AngularJS `$http` calls to the REST API
2. `ng-repeat` renders rows after the response arrives
3. Rows can be added (add product), removed (delete), or filtered (category filter)

The key challenge: **you cannot interact with table rows until AngularJS has finished rendering them.**

---

## Challenge 1: Waiting for Dynamic Data to Load

```java
// WRONG — may run before AngularJS renders rows
navigateTo("admin/products");
List<WebElement> rows = driver.findElements(By.cssSelector("table tbody tr"));
// rows.size() might be 0 if AngularJS hasn't finished!

// CORRECT — wait for rows to appear
navigateTo("admin/products");
WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
    By.cssSelector("table tbody tr:not([ng-show])"), 0
));
List<WebElement> rows = driver.findElements(By.cssSelector("table tbody tr"));
System.out.println("Rows loaded: " + rows.size());
```

**ShopEasy's empty state placeholder:**

```html
<tr ng-show="products.length === 0">
    <td colspan="6" class="text-center text-muted py-4">No products found.</td>
</tr>
```

This row exists in the DOM always. When `products.length === 0`, only this row shows. Filter it out:

```java
// Filter out the "no data" placeholder row
List<WebElement> allRows = driver.findElements(By.cssSelector("table tbody tr"));
List<WebElement> dataRows = allRows.stream()
    .filter(row -> {
        List<WebElement> cells = row.findElements(By.tagName("td"));
        return !cells.isEmpty() && cells.size() > 1;
        // placeholder has 1 td with colspan
    })
    .collect(java.util.stream.Collectors.toList());
System.out.println("Data rows: " + dataRows.size());
```

**Or use XPath to exclude the placeholder:**

```java
// The ng-repeat rows don't have ng-show on the tr itself
List<WebElement> dataRows = driver.findElements(
    By.xpath("//table/tbody/tr[@ng-repeat]")
);
```

---

## Challenge 2: Rows Change After Actions

When you delete a row, AngularJS removes it from the DOM. The stale reference must be handled.

```java
@Test
public void deleteFirstProductAndVerify() {
    loginAsAdmin();
    navigateTo("admin/products");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
        By.cssSelector("table tbody tr[ng-repeat]"), 0
    ));

    // Record initial count and first product name
    List<WebElement> rows = driver.findElements(By.cssSelector("table tbody tr[ng-repeat]"));
    int initialCount = rows.size();
    String firstProductName = rows.get(0)
        .findElement(By.cssSelector("td:nth-child(2) strong"))
        .getText();
    System.out.println("Deleting: " + firstProductName + " (total: " + initialCount + ")");

    // Click delete on first row
    rows.get(0).findElement(By.cssSelector(".btn-outline-danger")).click();

    // Handle confirm alert
    Alert alert = wait.until(ExpectedConditions.alertIsPresent());
    alert.accept();

    // Wait for row count to decrease
    wait.until(ExpectedConditions.numberOfElementsToBeLessThan(
        By.cssSelector("table tbody tr[ng-repeat]"), initialCount
    ));

    // Verify the product is gone
    List<WebElement> newRows = driver.findElements(By.cssSelector("table tbody tr[ng-repeat]"));
    Assert.assertEquals(newRows.size(), initialCount - 1, "One row should be removed");

    // Verify the deleted product name is no longer in the table
    boolean stillPresent = newRows.stream()
        .anyMatch(row -> row.getText().contains(firstProductName));
    Assert.assertFalse(stillPresent, firstProductName + " should be removed from table");
}
```

---

## Challenge 3: Add a Row and Verify It Appears

```java
@Test
public void addProductAndVerifyInTable() {
    loginAsAdmin();
    navigateTo("admin/products");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    int initialCount = wait.until(d -> {
        List<WebElement> r = d.findElements(By.cssSelector("table tbody tr[ng-repeat]"));
        return r.size(); // once this is > 0, return the count
    });

    // Open Add Product modal
    driver.findElement(By.cssSelector("button[ng-click='newProduct()']")).click();

    // Wait for modal to appear
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("productModal")));

    // Fill in product form
    String newProductName = "Test Product " + System.currentTimeMillis();
    driver.findElement(By.cssSelector("[ng-model='productForm.name']"))
          .sendKeys(newProductName);
    driver.findElement(By.cssSelector("[ng-model='productForm.price']"))
          .sendKeys("49.99");
    driver.findElement(By.cssSelector("[ng-model='productForm.stock']"))
          .sendKeys("100");
    driver.findElement(By.cssSelector("[ng-model='productForm.category']"))
          .sendKeys("Test");

    // Save
    driver.findElement(By.cssSelector("#productModal button[ng-click='saveProduct()']")).click();

    // Wait for modal to close
    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("productModal")));

    // Wait for new row to appear (count increases)
    wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
        By.cssSelector("table tbody tr[ng-repeat]"), initialCount
    ));

    // Verify new product is in table
    boolean found = driver.findElements(By.cssSelector("table tbody tr[ng-repeat]"))
        .stream()
        .anyMatch(row -> row.getText().contains(newProductName));
    Assert.assertTrue(found, "New product should appear in table: " + newProductName);
}
```

---

## Challenge 4: Real-Time Filter (Orders Status Filter)

For the orders table with real-time filtering:

```java
@Test
public void filterOrdersByStatus() {
    loginAsAdmin();
    navigateTo("admin/orders");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.cssSelector("table tbody tr")
    ));

    int totalRows = driver.findElements(By.cssSelector("table tbody tr")).size();
    System.out.println("All orders: " + totalRows);

    // Count rows by status badge
    countByStatus("PENDING");
    countByStatus("CONFIRMED");
    countByStatus("SHIPPED");
    countByStatus("DELIVERED");
    countByStatus("CANCELLED");
}

private void countByStatus(String status) {
    List<WebElement> statusBadges = driver.findElements(
        By.xpath("//tbody/tr[.//span[normalize-space()='" + status + "']]")
    );
    System.out.println("  " + status + ": " + statusBadges.size() + " orders");
}
```

---

## Challenge 5: Pagination

If a table has pagination controls, you navigate page by page:

```html
<!-- Generic Bootstrap pagination (not in ShopEasy currently) -->
<nav>
    <ul class="pagination">
        <li class="page-item disabled"><a class="page-link">Previous</a></li>
        <li class="page-item active"><a class="page-link">1</a></li>
        <li class="page-item"><a class="page-link">2</a></li>
        <li class="page-item"><a class="page-link">Next</a></li>
    </ul>
</nav>
```

```java
// Generic pagination handling
public class PaginationHelper {

    private WebDriver driver;
    private WebDriverWait wait;

    public PaginationHelper(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    /** Click Next page if available */
    public boolean goToNextPage() {
        List<WebElement> nextBtn = driver.findElements(
            By.xpath("//li[contains(@class,'page-item')][not(contains(@class,'disabled'))]" +
                     "//a[text()='Next']")
        );
        if (!nextBtn.isEmpty()) {
            nextBtn.get(0).click();
            wait.until(ExpectedConditions.stalenessOf(
                driver.findElement(By.cssSelector("table tbody tr"))
            ));
            return true;
        }
        return false; // no more pages
    }

    /** Get current page number */
    public int getCurrentPage() {
        WebElement activePage = driver.findElement(
            By.cssSelector(".pagination .page-item.active .page-link")
        );
        return Integer.parseInt(activePage.getText().trim());
    }

    /** Get total number of pages */
    public int getTotalPages() {
        List<WebElement> pages = driver.findElements(
            By.cssSelector(".pagination .page-item:not(:first-child):not(:last-child)")
        );
        return pages.size();
    }
}

// Iterate all pages and collect all product names
List<String> allProductNames = new ArrayList<>();
PaginationHelper pager = new PaginationHelper(driver);

do {
    List<WebElement> rows = driver.findElements(By.cssSelector("table tbody tr"));
    for (WebElement row : rows) {
        allProductNames.add(row.findElement(By.cssSelector("td:nth-child(2)")).getText());
    }
    System.out.println("Page " + pager.getCurrentPage() + ": " + rows.size() + " rows");
} while (pager.goToNextPage());

System.out.println("Total products across all pages: " + allProductNames.size());
```

---

## Challenge 6: Sort by Column Header

```java
@Test
public void sortProductsByPrice() {
    loginAsAdmin();
    navigateTo("admin/products");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("table tbody tr")));

    // Click the "Price" column header to sort
    WebElement priceHeader = driver.findElement(
        By.xpath("//table/thead//th[normalize-space()='Price']")
    );
    priceHeader.click();

    // Wait for rows to reorder (stale or URL change or class change)
    // Collect prices after sorting
    List<WebElement> priceCells = driver.findElements(
        By.cssSelector("table tbody td.text-end")
    );

    List<Double> prices = priceCells.stream()
        .map(e -> Double.parseDouble(e.getText().replace("$", "").replace(",", "")))
        .collect(java.util.stream.Collectors.toList());

    System.out.println("Prices after sort: " + prices);

    // Verify ascending order
    for (int i = 0; i < prices.size() - 1; i++) {
        Assert.assertTrue(prices.get(i) <= prices.get(i + 1),
            "Prices should be in ascending order");
    }
}
```

---

## Summary: Dynamic Table Best Practices

```java
// 1. Always wait for data to load
wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.cssSelector("tbody tr"), 0));

// 2. After actions that change row count, wait for count change
wait.until(ExpectedConditions.numberOfElementsToBeLessThan(By.cssSelector("tbody tr"), oldCount));
wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.cssSelector("tbody tr"), oldCount));

// 3. Re-find elements after DOM updates (avoid stale references)
// Don't store List<WebElement> for reuse after modifying the table

// 4. Filter placeholder rows (AngularJS ng-show empty state)
driver.findElements(By.xpath("//tbody/tr[@ng-repeat]")); // only real data rows

// 5. Use text-based XPath for row lookup (more robust than index)
By.xpath("//tbody/tr[.//strong[contains(text(),'ProductName')]]")
```
