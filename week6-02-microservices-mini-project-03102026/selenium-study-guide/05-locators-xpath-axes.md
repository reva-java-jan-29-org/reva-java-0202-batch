# 05 — Selenium Locators: XPath Axes

## What Are XPath Axes?

XPath **axes** define the directional relationship between the current (context) node and other nodes in the document tree. They let you navigate to elements that have no unique identifier themselves but can be found relative to a known nearby element.

```
                        ancestor
                            ↑
      preceding-sibling ← [current node] → following-sibling
                            ↓
                        descendant
                    (child is direct descendant)
```

---

## XPath Axis Syntax

```xpath
//currentNode/axisName::targetTag[@filter]
```

Or without a specific current node:
```xpath
//targetTag/axisName::relativeTag
```

---

## All Important XPath Axes

| Axis | Direction | Selects |
|---|---|---|
| `child` | Down (direct) | Direct children of the current node |
| `parent` | Up (direct) | Direct parent of the current node |
| `ancestor` | Up (all) | All ancestors (parent, grandparent, …) |
| `ancestor-or-self` | Up (all + self) | Self + all ancestors |
| `descendant` | Down (all) | All descendants at any depth |
| `descendant-or-self` | Down + self | Self + all descendants (same as `//`) |
| `following-sibling` | Sideways (after) | All siblings that come AFTER the current node |
| `preceding-sibling` | Sideways (before) | All siblings that come BEFORE the current node |
| `following` | After in document | All nodes after current node (not descendants) |
| `preceding` | Before in document | All nodes before current node (not ancestors) |
| `self` | Same node | The current node itself |

---

## 1. `child` Axis

Selects direct children only (same as `/`).

```java
// Direct child rows of tbody
List<WebElement> rows = driver.findElements(
    By.xpath("//tbody/child::tr")
    // Same as: //tbody/tr
);

// Direct child input inside an input-group div
WebElement input = driver.findElement(
    By.xpath("//div[@class='input-group']/child::input")
);
```

**ShopEasy — Cart quantity input:**
```html
<div class="input-group input-group-sm">
    <button>-</button>
    <input type="number" class="form-control text-center">
    <button>+</button>
</div>
```
```java
WebElement qtyInput = driver.findElement(
    By.xpath("//div[contains(@class,'input-group-sm')]/child::input[@type='number']")
);
```

---

## 2. `parent` Axis

Moves UP to the direct parent element.

**Use case:** You found a label/icon and need its parent container.

**ShopEasy — Find the card-footer containing Add to Cart for a specific product:**
```html
<div class="card-footer bg-transparent">
    <button class="btn btn-primary w-100">Add to Cart</button>
</div>
```

```java
// Find "Add to Cart" button, then get its parent card-footer
WebElement addToCartBtn = driver.findElement(
    By.xpath("//button[contains(@class,'btn-primary') and .//i[contains(@class,'bi-cart-plus')]]/parent::div")
);

// Another example: find the input-group that wraps the search input
WebElement searchInputGroup = driver.findElement(
    By.xpath("//input[@ng-model='searchQuery']/parent::div[contains(@class,'input-group')]")
);
```

---

## 3. `ancestor` Axis

Moves UP through ALL ancestors until one matches.

**Use case:** Find the card container of a product given its title text.

**ShopEasy — Find entire product card by its title:**
```html
<div class="col">
    <div class="card h-100 product-card shadow-sm">
        <div class="card-body">
            <h6 class="card-title fw-bold">Laptop Pro</h6>
        </div>
    </div>
</div>
```

```java
// Start at the h6 title, walk up to the product-card div
WebElement laptopCard = driver.findElement(
    By.xpath("//h6[contains(@class,'card-title')][contains(text(),'Laptop')]" +
             "/ancestor::div[contains(@class,'product-card')]")
);

// Now find Add to Cart button WITHIN that specific card
WebElement addToCart = laptopCard.findElement(
    By.xpath(".//button[contains(@class,'btn-primary')]")
);
addToCart.click();
```

---

## 4. `descendant` Axis

Selects all elements at any depth below the current node (same as `//`).

```java
// All buttons anywhere inside the product modal
List<WebElement> modalButtons = driver.findElements(
    By.xpath("//div[@id='productModal']/descendant::button")
    // Same as: //div[@id='productModal']//button
);

// All inputs inside the login form
List<WebElement> loginInputs = driver.findElements(
    By.xpath("//form[@name='loginForm']/descendant::input")
);
```

---

## 5. `following-sibling` Axis

Selects all siblings that appear **AFTER** the current node in the same parent.

**ShopEasy — Admin Products table: Find the edit button column relative to the product name column:**

```html
<tr ng-repeat="p in products">
    <td>1</td>
    <td><strong>Laptop Pro</strong></td>   ← sibling
    <td><span class="badge">Electronics</span></td>  ← following sibling
    <td>$999.00</td>                        ← following sibling
    <td><span>50</span></td>               ← following sibling
    <td>
        <button class="btn-outline-primary">Edit</button>
        <button class="btn-outline-danger">Delete</button>
    </td>                                   ← following sibling (last)
</tr>
```

```java
// Find the row where product name is "Laptop Pro"
// Then get the LAST td (actions column) using following-sibling
WebElement actionsCell = driver.findElement(
    By.xpath("//tbody/tr[td/strong[contains(text(),'Laptop')]]/td[following-sibling::*[0]]")
);

// Better: directly get the actions td (last td in row)
WebElement editBtn = driver.findElement(
    By.xpath("//tbody/tr[.//strong[contains(text(),'Laptop')]]" +
             "/td[last()]//button[contains(@class,'btn-outline-primary')]")
);

// following-sibling: find the stock badge column that comes after the category column
WebElement stockBadge = driver.findElement(
    By.xpath("//td[span[@class='badge bg-secondary'][text()='Electronics']]" +
             "/following-sibling::td[2]/span")
);
```

**Practical use — table header to column index mapping:**
```java
// Find the position of the "Name" column header
// Then get the same-index cell from a data row
WebElement nameHeader = driver.findElement(
    By.xpath("//thead/tr/th[normalize-space()='Name']")
);

// Count how many siblings come before it → that's the column index (1-based)
int colIndex = nameHeader.findElements(
    By.xpath("preceding-sibling::th")
).size() + 1;

// Get the value in that column for the first data row
String productName = driver.findElement(
    By.xpath("//tbody/tr[1]/td[" + colIndex + "]")
).getText();
```

---

## 6. `preceding-sibling` Axis

Selects all siblings that appear **BEFORE** the current node.

```java
// Find the "Name" header column, check what headers come before it
List<WebElement> headersBefore = driver.findElements(
    By.xpath("//thead//th[normalize-space()='Name']/preceding-sibling::th")
);
System.out.println("Columns before Name: " + headersBefore.size()); // 1 (the ID column)

// Cart page: Given the price cell, find the product name cell (which comes before)
WebElement productNameCell = driver.findElement(
    By.xpath("//td[@class='text-center'][contains(text(),'$999')]" +
             "/preceding-sibling::td[1]")
);
```

---

## 7. `following` Axis

Selects everything in the document that comes AFTER the current node (not just siblings — all following nodes). Less commonly used; prefer `following-sibling`.

```java
// Find all elements after the search button
List<WebElement> elementsAfterSearch = driver.findElements(
    By.xpath("//button[@ng-click='search()']/following::div[contains(@class,'product-card')]")
);
```

---

## Practical Scenario: Locating Any Cell in a Table by Header + Row Value

This is one of the most common and powerful uses of XPath axes.

**Goal:** In the Admin Products table, find the price of the product named "Laptop Pro".

```java
// Step 1: Find the column index of the "Price" header
// Step 2: Find the row where Name = "Laptop Pro"
// Step 3: Get the cell at the "Price" column index

String targetProductName = "Laptop Pro";
String targetColumn = "Price";

// Get column index (count preceding siblings + 1)
String colIndexXPath = "count(//thead/tr/th[normalize-space()='" + targetColumn + "']/preceding-sibling::th) + 1";
// Note: executing this as JS or counting in Java is easier

// Direct approach — navigate by position
// Price is the 4th column (ID, Name, Category, Price, Stock, Actions)
WebElement priceCell = driver.findElement(
    By.xpath("//tbody/tr[.//strong[contains(text(),'" + targetProductName + "')]]/td[4]")
);
System.out.println("Price: " + priceCell.getText());
```

---

## Quick Reference: Which Axis to Use?

| Situation | Axis to Use | Example |
|---|---|---|
| Need the label's input | `following-sibling` | Label → input in same div |
| Need the card wrapping an item | `ancestor` | Title → card div |
| Need all items in a container | `descendant` | Modal → all inputs |
| Need the column of a given header | `following-sibling` on header | Header → count position |
| Need the row that contains a cell value | Parent row of a `td` | `td[text()='X']/parent::tr` |
| Need the next row after a match | `following-sibling` on `tr` | `tr[…]/following-sibling::tr[1]` |

---

## Summary of ShopEasy-Specific Axis Examples

```java
// 1. Find product card ancestor from title
"//h6[text()='Laptop Pro']/ancestor::div[contains(@class,'product-card')]"

// 2. Find delete button (following-sibling of edit button)
"//button[contains(@class,'btn-outline-primary')]/following-sibling::button[1]"

// 3. Find all inputs inside login form (descendant)
"//form[@name='loginForm']/descendant::input"

// 4. Find parent input-group of search input
"//input[@ng-model='searchQuery']/parent::div"

// 5. Find the row containing a specific product (ancestor from strong tag)
"//strong[contains(text(),'Laptop')]/ancestor::tr"

// 6. Actions column of a specific product row (last td)
"//tr[.//strong[text()='Laptop Pro']]/td[last()]"
```
