# 12 — Handle Static Web Tables

## What Is a Static Web Table?

A **static web table** has a fixed, predictable structure:
- A `<table>` element
- `<thead>` for headers
- `<tbody>` for data rows
- `<tr>` for each row, `<th>` or `<td>` for each cell

Data does not change based on pagination or AJAX calls — it's all loaded at once.

---

## HTML Table Anatomy

```html
<table class="table table-hover mb-0">
    <thead class="table-light">
        <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Category</th>
            <th class="text-end">Price</th>
            <th class="text-center">Stock</th>
            <th></th>    <!-- Actions column (no header text) -->
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>1</td>
            <td><strong>Laptop Pro</strong></td>
            <td><span class="badge bg-secondary">Electronics</span></td>
            <td class="text-end">$999.00</td>
            <td class="text-center"><span class="badge bg-success">50</span></td>
            <td><button>Edit</button> <button>Delete</button></td>
        </tr>
        <tr>
            <td>2</td>
            <td><strong>Wireless Mouse</strong></td>
            <td><span class="badge bg-secondary">Accessories</span></td>
            <td class="text-end">$29.99</td>
            <td class="text-center"><span class="badge bg-danger">0</span></td>
            <td><button>Edit</button> <button>Delete</button></td>
        </tr>
    </tbody>
</table>
```

---

## ShopEasy Tables

| Page | Table Contents | Key Columns |
|---|---|---|
| `#!/admin/products` | All products | ID, Name, Category, Price, Stock, Actions |
| `#!/admin/customers` | All customers | ID, Username, Name, Mobile, Status, Actions |
| `#!/admin/orders` | All orders | Order ID, Customer ID, Total, Status, Date |
| `#!/cart` | Cart items | Product, Price, Qty, Subtotal, Remove |
| `#!/orders` | My orders | Order ID, Date, Items, Total, Status, View |
| `#!/payments` | My payments | Transaction ID, Order ID, Amount, Status, Card, Date |

---

## Basic Table Operations

### 1. Count Rows

```java
navigateTo("admin/products");
WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("table tbody tr")));

// Count data rows only (exclude header row)
List<WebElement> rows = driver.findElements(By.cssSelector("table tbody tr"));
System.out.println("Total products in table: " + rows.size());
```

### 2. Count Columns

```java
List<WebElement> headers = driver.findElements(By.cssSelector("table thead th"));
System.out.println("Number of columns: " + headers.size()); // 6
```

### 3. Read All Header Texts

```java
List<WebElement> headers = driver.findElements(By.cssSelector("table thead th"));
System.out.println("Table headers:");
for (WebElement th : headers) {
    System.out.println("  " + th.getText());
}
// Output: ID, Name, Category, Price, Stock, (empty)
```

### 4. Read a Specific Cell (Row Index, Column Index)

Both are 1-based in XPath, 0-based in Java list indexing.

```java
// Get cell at row 2, column 2 (Name of second product) — 1-based XPath
String productName = driver.findElement(
    By.xpath("//table/tbody/tr[2]/td[2]")
).getText();
System.out.println("Second product name: " + productName);

// Java list approach — 0-based
List<WebElement> rows = driver.findElements(By.cssSelector("table tbody tr"));
WebElement secondRow = rows.get(1);  // 0-based index 1 = second row
List<WebElement> cells = secondRow.findElements(By.tagName("td"));
System.out.println("Name: " + cells.get(1).getText()); // 0-based col 1
```

### 5. Read All Data from the Entire Table

```java
List<WebElement> rows = driver.findElements(By.cssSelector("table tbody tr"));

System.out.println("=== Product Table Contents ===");
for (WebElement row : rows) {
    List<WebElement> cells = row.findElements(By.tagName("td"));
    if (cells.isEmpty()) continue;  // skip empty placeholder rows

    String id       = cells.get(0).getText();
    String name     = cells.get(1).getText();
    String category = cells.get(2).getText();
    String price    = cells.get(3).getText();
    String stock    = cells.get(4).getText();

    System.out.printf("ID: %-5s | Name: %-25s | Category: %-15s | Price: %-10s | Stock: %s%n",
        id, name, category, price, stock);
}
```

---

## Advanced Table Operations

### 6. Search for a Specific Row by Column Value

```java
// Find the row where product name is "Laptop Pro"
private WebElement findRowByProductName(String productName) {
    List<WebElement> rows = driver.findElements(By.cssSelector("table tbody tr"));
    for (WebElement row : rows) {
        List<WebElement> cells = row.findElements(By.tagName("td"));
        if (cells.size() > 1 && cells.get(1).getText().contains(productName)) {
            return row;
        }
    }
    return null; // not found
}

// Usage
WebElement laptopRow = findRowByProductName("Laptop Pro");
Assert.assertNotNull(laptopRow, "Laptop Pro should be in the table");
System.out.println("Found Laptop row: " + laptopRow.getText());
```

**XPath version (more concise):**
```java
WebElement laptopRow = driver.findElement(
    By.xpath("//table/tbody/tr[.//strong[contains(text(),'Laptop Pro')]]")
);
```

### 7. Get a Specific Column Value from a Found Row

```java
// Find the price of "Laptop Pro"
WebElement laptopRow = driver.findElement(
    By.xpath("//table/tbody/tr[.//strong[contains(text(),'Laptop Pro')]]")
);
List<WebElement> cells = laptopRow.findElements(By.tagName("td"));
String price = cells.get(3).getText(); // Price is at index 3
System.out.println("Laptop Pro price: " + price); // e.g., "$999.00"
```

### 8. Click Action Button in a Specific Row

```java
// Click Edit for "Wireless Mouse"
WebElement mouseRow = driver.findElement(
    By.xpath("//tbody/tr[.//strong[contains(text(),'Wireless Mouse')]]")
);

// Click the edit button (first btn in the row's last td)
WebElement editBtn = mouseRow.findElement(
    By.cssSelector("button.btn-outline-primary")
);
editBtn.click();
System.out.println("Edit button clicked for Wireless Mouse");
```

### 9. Verify Table is Sorted

```java
// Get all product names in order
List<WebElement> nameElements = driver.findElements(
    By.cssSelector("table tbody tr td:nth-child(2) strong")
);
List<String> names = nameElements.stream()
    .map(WebElement::getText)
    .collect(java.util.stream.Collectors.toList());

// Check if sorted alphabetically
List<String> sorted = new ArrayList<>(names);
Collections.sort(sorted);
Assert.assertEquals(names, sorted, "Products should be sorted alphabetically");
```

### 10. Verify a Column Contains Specific Values

```java
// Verify all stock badges are either green (bg-success) or red (bg-danger)
List<WebElement> stockBadges = driver.findElements(
    By.cssSelector("table tbody td span.badge")
);
for (WebElement badge : stockBadges) {
    String classes = badge.getAttribute("class");
    boolean isValid = classes.contains("bg-success") || classes.contains("bg-danger");
    Assert.assertTrue(isValid, "Stock badge should be green or red: " + classes);
}
```

---

## Admin Customers Table

```html
<!-- admin-customers.html -->
<tr ng-repeat="c in customers">
    <td>{{c.id}}</td>
    <td>{{c.username}}</td>
    <td>{{c.firstName}} {{c.lastName}}</td>
    <td>{{c.mobileNumber}}</td>
    <td>
        <span class="badge bg-success" ng-show="c.enabled">Active</span>
        <span class="badge bg-danger" ng-show="!c.enabled">Disabled</span>
    </td>
    <td>
        <button ng-show="c.enabled" ng-click="disableCustomer(c.id)">Disable</button>
        <button ng-show="!c.enabled" ng-click="enableCustomer(c.id)">Enable</button>
        <button ng-click="deleteCustomer(c.id)">Delete</button>
    </td>
</tr>
```

```java
@Test
public void verifyCustomersTableStatus() {
    loginAsAdmin();
    navigateTo("admin/customers");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.cssSelector("table tbody tr")
    ));

    List<WebElement> rows = driver.findElements(By.cssSelector("table tbody tr"));
    System.out.println("Total customers: " + rows.size());

    int activeCount = 0, disabledCount = 0;

    for (WebElement row : rows) {
        List<WebElement> cells = row.findElements(By.tagName("td"));
        String username = cells.get(1).getText();

        // Check status by badge class
        WebElement statusCell = cells.get(4);
        boolean isActive = !statusCell.findElements(By.cssSelector(".badge.bg-success")).isEmpty();
        boolean isDisabled = !statusCell.findElements(By.cssSelector(".badge.bg-danger")).isEmpty();

        String status = isActive ? "Active" : (isDisabled ? "Disabled" : "Unknown");
        System.out.println("  " + username + ": " + status);

        if (isActive) activeCount++;
        if (isDisabled) disabledCount++;
    }

    System.out.println("Active: " + activeCount + ", Disabled: " + disabledCount);
}
```

---

## Utility Class: TableUtil

```java
public class TableUtil {

    private WebDriver driver;

    public TableUtil(WebDriver driver) {
        this.driver = driver;
    }

    /** Get column index (1-based) from header text */
    public int getColumnIndex(String headerText) {
        List<WebElement> headers = driver.findElements(By.cssSelector("table thead th"));
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).getText().trim().equalsIgnoreCase(headerText)) {
                return i + 1; // 1-based
            }
        }
        throw new RuntimeException("Column not found: " + headerText);
    }

    /** Get cell value by row number (1-based) and column header name */
    public String getCellValue(int rowNum, String columnHeader) {
        int colIndex = getColumnIndex(columnHeader);
        return driver.findElement(
            By.xpath("//table/tbody/tr[" + rowNum + "]/td[" + colIndex + "]")
        ).getText().trim();
    }

    /** Get all values in a column by header name */
    public List<String> getColumnValues(String columnHeader) {
        int colIndex = getColumnIndex(columnHeader);
        return driver.findElements(
            By.xpath("//table/tbody/tr/td[" + colIndex + "]")
        ).stream().map(e -> e.getText().trim()).collect(java.util.stream.Collectors.toList());
    }
}

// Usage in test
TableUtil table = new TableUtil(driver);
System.out.println(table.getCellValue(1, "Name"));     // First product name
System.out.println(table.getColumnValues("Category")); // All categories
```
