# 04 — Selenium Locators: XPath

## What is XPath?

XPath (XML Path Language) is a query language for navigating through elements and attributes in an XML/HTML document. It is the most powerful and flexible locator strategy in Selenium — capable of locating any element, including ones that CSS cannot reach (e.g., an element found by its text content, or a parent of a known child).

---

## Absolute vs Relative XPath

### Absolute XPath
Starts from the root of the document (`/html`). Fragile — any structural change breaks it.

```xpath
/html/body/div/main/div/div/div/div/div/form/div/div/input
```
**Never use in real tests.** This is what browser DevTools generates by default — ignore it.

### Relative XPath
Starts with `//` — searches anywhere in the document. Robust and preferred.

```xpath
//input[@placeholder='Enter your username']
```

---

## XPath Basic Syntax

```
//tagName[@attribute='value']
   │       │
   │       └── predicate (filter condition in square brackets)
   └── double slash = search anywhere in document

//tagName                   → all elements with that tag
//*[@attribute='value']     → any tag (*) with that attribute
//tag[@a='v1'][@b='v2']    → multiple attribute conditions (AND)
//tag[@a='v1' or @b='v2'] → OR condition
```

---

## XPath Functions

| Function | Syntax | Purpose |
|---|---|---|
| `text()` | `//a[text()='Login']` | Match by exact visible text |
| `contains()` | `//a[contains(text(),'Register')]` | Match by partial text or attribute |
| `starts-with()` | `//input[starts-with(@placeholder,'Enter')]` | Attribute starts with value |
| `normalize-space()` | `//button[normalize-space()='Save']` | Trims whitespace before matching |
| `not()` | `//button[not(@disabled)]` | Negation |
| `last()` | `//tr[last()]` | Last element in a set |
| `position()` | `//tr[position()=2]` | Element at specific position |

---

## Applying XPath to ShopEasy

### Login Page (`#!/login`)

```html
<form name="loginForm" ng-submit="login()" novalidate>
    <input type="text" class="form-control"
           ng-model="loginData.username"
           placeholder="Enter your username" required>
    <input type="password" class="form-control"
           ng-model="loginData.password"
           placeholder="Enter password" required>
    <button type="submit" class="btn btn-primary w-100 py-2">
        <span ng-show="!loading">
            <i class="bi bi-box-arrow-in-right"></i> Login
        </span>
    </button>
</form>
<a href="#!/register" class="text-primary fw-semibold">Register here</a>
```

```java
// By attribute — placeholder
WebElement usernameInput = driver.findElement(
    By.xpath("//input[@placeholder='Enter your username']")
);

// By ng-model attribute
WebElement passwordInput = driver.findElement(
    By.xpath("//input[@ng-model='loginData.password']")
);

// By multiple attributes (AND)
WebElement passwordStrict = driver.findElement(
    By.xpath("//input[@type='password' and @ng-model='loginData.password']")
);

// By button type
WebElement loginBtn = driver.findElement(
    By.xpath("//button[@type='submit']")
);

// By link text — exact
WebElement registerLink = driver.findElement(
    By.xpath("//a[text()='Register here']")
);

// By link text — contains (better when text has extra whitespace)
WebElement registerLinkContains = driver.findElement(
    By.xpath("//a[contains(text(),'Register')]")
);

// Scoped — input inside form with name="loginForm"
WebElement scopedInput = driver.findElement(
    By.xpath("//form[@name='loginForm']//input[@type='text']")
);
```

---

### Navbar (`index.html`)

```html
<nav class="navbar navbar-expand-lg navbar-dark bg-primary">
    <a class="navbar-brand fw-bold" href="#!/products">
        <i class="bi bi-shop"></i> ShopEasy
    </a>
    <a class="nav-link" href="#!/login">
        <i class="bi bi-box-arrow-in-right"></i> Login
    </a>
```

```java
// Brand link — by class and href
WebElement brand = driver.findElement(
    By.xpath("//a[@class='navbar-brand fw-bold']")
);

// Nav link to login — href contains "login"
WebElement loginNavLink = driver.findElement(
    By.xpath("//a[contains(@href,'login')][@class='nav-link']")
);

// All nav links
List<WebElement> navLinks = driver.findElements(
    By.xpath("//ul[contains(@class,'navbar-nav')]//a[@class='nav-link']")
);
```

---

### Products Page (`#!/products`)

```html
<div class="col" ng-repeat="product in filteredProducts">
    <div class="card h-100 product-card shadow-sm">
        <div class="card-body d-flex flex-column">
            <span class="badge bg-secondary mb-2 align-self-start">Electronics</span>
            <h6 class="card-title fw-bold">Laptop Pro</h6>
            <div class="d-flex justify-content-between align-items-center mt-auto pt-2">
                <span class="fs-5 fw-bold text-primary">$999.99</span>
            </div>
        </div>
        <div class="card-footer bg-transparent border-top-0 pb-3">
            <button class="btn btn-primary w-100">
                <i class="bi bi-cart-plus"></i> Add to Cart
            </button>
        </div>
    </div>
</div>
```

```java
// All product cards
List<WebElement> productCards = driver.findElements(
    By.xpath("//div[contains(@class,'product-card')]")
);

// Product card containing a specific title
WebElement laptopCard = driver.findElement(
    By.xpath("//div[@class='card h-100 product-card shadow-sm'][.//h6[contains(text(),'Laptop')]]")
);

// Add to Cart button inside that specific card
WebElement addToCartBtn = laptopCard.findElement(
    By.xpath(".//button[contains(@class,'btn-primary')]")
);

// Product category badge with specific text
WebElement electronicsBadge = driver.findElement(
    By.xpath("//span[@class='badge bg-secondary mb-2 align-self-start'][text()='Electronics']")
);

// Price element — by class containing text-primary
List<WebElement> prices = driver.findElements(
    By.xpath("//span[contains(@class,'text-primary') and contains(text(),'$')]")
);
```

---

### Admin Products Table (`#!/admin/products`)

```html
<table class="table table-hover mb-0 align-middle">
    <thead class="table-light">
        <tr>
            <th>ID</th><th>Name</th><th>Category</th>
            <th>Price</th><th>Stock</th><th></th>
        </tr>
    </thead>
    <tbody>
        <tr ng-repeat="p in products">
            <td class="text-muted small">1</td>
            <td><strong>Laptop Pro</strong>...</td>
            <td><span class="badge bg-secondary">Electronics</span></td>
            <td class="text-end">$999.00</td>
            <td class="text-center"><span class="badge bg-success">50</span></td>
            <td class="text-end">
                <button class="btn btn-sm btn-outline-primary me-1">Edit</button>
                <button class="btn btn-sm btn-outline-danger">Delete</button>
            </td>
        </tr>
    </tbody>
</table>
```

```java
// All data rows in the product table (tbody rows only)
List<WebElement> dataRows = driver.findElements(
    By.xpath("//table[contains(@class,'table')]//tbody/tr")
);
System.out.println("Product rows: " + dataRows.size());

// Find a row by product name
WebElement laptopRow = driver.findElement(
    By.xpath("//tbody/tr[.//strong[contains(text(),'Laptop')]]")
);

// Edit button in the Laptop row
WebElement editBtn = laptopRow.findElement(
    By.xpath(".//button[contains(@class,'btn-outline-primary')]")
);

// Delete button in a specific row by position (3rd row)
WebElement deleteBtn3rd = driver.findElement(
    By.xpath("//tbody/tr[3]//button[contains(@class,'btn-outline-danger')]")
);

// Find row where stock badge text is "0" (out of stock)
List<WebElement> outOfStockRows = driver.findElements(
    By.xpath("//tbody/tr[.//span[contains(@class,'bg-danger') and text()='0']]")
);
```

---

### Cart Page (`#!/cart`)

```html
<tbody>
    <tr ng-repeat="item in cart.items">
        <td><strong>Laptop Pro</strong></td>
        <td class="text-center">$999.00</td>
        <td class="text-center">
            <div class="input-group input-group-sm">
                <button class="btn btn-outline-secondary">-</button>
                <input type="number" class="form-control text-center" ng-model="item.quantity">
                <button class="btn btn-outline-secondary">+</button>
            </div>
        </td>
        <td class="text-end fw-semibold">$999.00</td>
        <td>
            <button class="btn btn-sm btn-outline-danger">
                <i class="bi bi-trash"></i>
            </button>
        </td>
    </tr>
</tbody>
```

```java
// All cart item rows
List<WebElement> cartRows = driver.findElements(
    By.xpath("//table//tbody/tr[td/strong]")
);

// Find cart row for a specific product
WebElement laptopCartRow = driver.findElement(
    By.xpath("//tbody/tr[td/strong[text()='Laptop Pro']]")
);

// Quantity input in that row
WebElement qtyInput = laptopCartRow.findElement(
    By.xpath(".//input[@type='number']")
);

// Remove button (bi-trash icon's parent button)
WebElement removeBtn = laptopCartRow.findElement(
    By.xpath(".//button[contains(@class,'btn-outline-danger')]")
);

// Plus (+) button
WebElement plusBtn = laptopCartRow.findElement(
    By.xpath(".//button[text()='+']")
);
```

---

## XPath Cheat Sheet for ShopEasy

| What to Find | XPath |
|---|---|
| Username input | `//input[@ng-model='loginData.username']` |
| Password input | `//input[@type='password']` |
| Login button | `//button[@type='submit']` |
| Register link | `//a[contains(text(),'Register')]` |
| Search input | `//input[@ng-model='searchQuery']` |
| Category dropdown | `//select[@ng-model='selectedCategory']` |
| All product cards | `//div[contains(@class,'product-card')]` |
| Add to Cart buttons | `//button[.//i[contains(@class,'bi-cart-plus')]]` |
| Product modal | `//*[@id='productModal']` |
| Product name field in modal | `//*[@id='productModal']//input[@ng-model='productForm.name']` |
| Table body rows | `//table//tbody/tr` |
| Nth table row | `//table//tbody/tr[2]` |

---

## contains() vs text() — Key Difference

```xpath
//a[text()='Login']            ← EXACT match of ALL text content (fails if has child elements with text)
//a[contains(text(),'Login')]  ← PARTIAL match, handles whitespace better
//a[normalize-space()='Login'] ← Trims leading/trailing spaces, then exact match
```

**In ShopEasy's navbar links**, text contains icon text from `<i>` tags:
```html
<a class="nav-link" href="#!/login">
    <i class="bi bi-box-arrow-in-right"></i> Login
</a>
```
`text()='Login'` will FAIL (the text node is just " Login" with a space, but `<i>` is a child element).
Use: `//a[contains(@href,'login') and contains(@class,'nav-link')]` instead.
