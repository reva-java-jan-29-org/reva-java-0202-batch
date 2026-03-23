# 03 — Selenium Locators: CSS Selectors

## Why CSS Selectors?

CSS selectors are the **recommended primary locator strategy** in Selenium for most real-world apps. They are:

- **Faster** than XPath in Chrome and Firefox
- **Readable** and widely understood (same syntax as your stylesheets)
- **Flexible** — can target by tag, class, attribute, or position
- **Perfect for AngularJS apps** where elements use `ng-model`, `placeholder`, and Bootstrap classes instead of IDs

---

## CSS Selector Syntax Reference

| Pattern | Syntax | Example | Matches |
|---|---|---|---|
| Tag | `tag` | `input` | `<input>` |
| ID | `#id` | `#productModal` | `id="productModal"` |
| Class | `.class` | `.btn-primary` | `class="btn-primary"` |
| Multiple classes | `.c1.c2` | `.btn.btn-primary` | Both classes present |
| Attribute exact | `[attr="val"]` | `[type="password"]` | `type="password"` |
| Attribute contains | `[attr*="val"]` | `[placeholder*="username"]` | placeholder contains "username" |
| Attribute starts with | `[attr^="val"]` | `[href^="#!/"]` | href starts with "#!/" |
| Attribute ends with | `[attr$="val"]` | `[href$="login"]` | href ends with "login" |
| Child | `parent > child` | `form > div` | direct child `div` of `form` |
| Descendant | `ancestor descendant` | `form input` | any `input` inside `form` |
| Adjacent sibling | `a + b` | `hr + p` | `p` immediately after `hr` |
| First child | `:first-child` | `tr:first-child` | first `tr` in parent |
| Last child | `:last-child` | `tr:last-child` | last `tr` in parent |
| nth child | `:nth-child(n)` | `tr:nth-child(2)` | 2nd `tr` |
| Not | `:not(selector)` | `button:not(.btn-close)` | button without class `btn-close` |

---

## Applying CSS Selectors to ShopEasy

### Login Page (`#!/login`)

```html
<form name="loginForm" ng-submit="login()" novalidate>
    <input type="text" class="form-control"
           ng-model="loginData.username"
           placeholder="Enter your username" required>
    <input type="password" class="form-control"
           ng-model="loginData.password"
           placeholder="Enter password" required>
    <button type="submit" class="btn btn-primary w-100 py-2">Login</button>
</form>
```

```java
// By type attribute
WebElement usernameInput = driver.findElement(By.cssSelector("input[type='text']"));
WebElement passwordInput = driver.findElement(By.cssSelector("input[type='password']"));

// By placeholder attribute (very readable)
WebElement usernameByPlaceholder = driver.findElement(
    By.cssSelector("input[placeholder='Enter your username']")
);

// By ng-model attribute (AngularJS-specific — great for AngularJS apps)
WebElement usernameByNgModel = driver.findElement(
    By.cssSelector("input[ng-model='loginData.username']")
);
WebElement passwordByNgModel = driver.findElement(
    By.cssSelector("input[ng-model='loginData.password']")
);

// By button type
WebElement loginButton = driver.findElement(By.cssSelector("button[type='submit']"));

// By multiple classes
WebElement loginButtonByClass = driver.findElement(
    By.cssSelector("button.btn.btn-primary")
);

// Scoped: input inside a named form
WebElement scopedInput = driver.findElement(
    By.cssSelector("form[name='loginForm'] input[type='text']")
);
```

---

### Register Page (`#!/register`)

```html
<form name="registerForm" ...>
    <input ng-model="registerData.username" placeholder="Choose a username (3–50 characters)" ...>
    <input ng-model="registerData.firstName" placeholder="John" ...>
    <input ng-model="registerData.lastName" placeholder="Doe" ...>
    <input type="tel" ng-model="registerData.mobileNumber" placeholder="e.g. 9876543210" ...>
    <input type="password" ng-model="registerData.password" placeholder="Minimum 6 characters" ...>
    <input type="password" ng-model="registerData.confirmPassword" placeholder="Re-enter your password" ...>
    <button type="submit" class="btn btn-success w-100 py-2">Create Account</button>
</form>
```

```java
// Target each field by ng-model — most precise for AngularJS
WebElement usernameField   = driver.findElement(By.cssSelector("[ng-model='registerData.username']"));
WebElement firstNameField  = driver.findElement(By.cssSelector("[ng-model='registerData.firstName']"));
WebElement lastNameField   = driver.findElement(By.cssSelector("[ng-model='registerData.lastName']"));
WebElement mobileField     = driver.findElement(By.cssSelector("[ng-model='registerData.mobileNumber']"));
WebElement passwordField   = driver.findElement(By.cssSelector("[ng-model='registerData.password']"));
WebElement confirmField    = driver.findElement(By.cssSelector("[ng-model='registerData.confirmPassword']"));

// Submit button — by class
WebElement createBtn = driver.findElement(By.cssSelector("button.btn-success[type='submit']"));

// Placeholder contains (partial)
WebElement mobileByPlaceholder = driver.findElement(
    By.cssSelector("input[placeholder*='9876']")
);
```

---

### Navbar (`index.html`)

```html
<nav class="navbar navbar-expand-lg navbar-dark bg-primary">
    <a class="navbar-brand fw-bold" href="#!/products">...</a>
    <div class="collapse navbar-collapse" id="navbarNav">
        <a class="nav-link" href="#!/login">...</a>
        <a class="nav-link" href="#!/register">...</a>
    </div>
</nav>
```

```java
// Navbar container
WebElement navbar = driver.findElement(By.cssSelector("nav.navbar"));

// Brand logo link
WebElement brand = driver.findElement(By.cssSelector("a.navbar-brand"));

// Login nav link (href ends with "login")
WebElement loginNavLink = driver.findElement(By.cssSelector("a[href$='login']"));

// Register nav link (href starts with hash)
WebElement registerNavLink = driver.findElement(By.cssSelector("a[href='#!/register']"));

// All nav links in the navbar
List<WebElement> navLinks = driver.findElements(By.cssSelector(".navbar-nav .nav-link"));
```

---

### Products Page (`#!/products`)

```html
<input type="text" class="form-control form-control-lg"
       ng-model="searchQuery" placeholder="Search products by name or description...">
<button class="btn btn-primary" ng-click="search()">Search</button>

<select class="form-select form-select-lg" ng-model="selectedCategory" ...>

<div class="row row-cols-1 row-cols-sm-2 row-cols-md-3 row-cols-lg-4 g-4">
    <div class="col" ng-repeat="product in filteredProducts">
        <div class="card h-100 product-card shadow-sm">
            <div class="card-body d-flex flex-column">
                <span class="badge bg-secondary mb-2">Electronics</span>
                <h6 class="card-title fw-bold">Laptop</h6>
            </div>
            <div class="card-footer bg-transparent">
                <button class="btn btn-primary w-100">Add to Cart</button>
            </div>
        </div>
    </div>
</div>
```

```java
// Search input
WebElement searchInput = driver.findElement(
    By.cssSelector("input[ng-model='searchQuery']")
);

// Search button — by click handler attribute
WebElement searchBtn = driver.findElement(
    By.cssSelector("button[ng-click='search()']")
);

// Category dropdown — by ng-model
WebElement categorySelect = driver.findElement(
    By.cssSelector("select[ng-model='selectedCategory']")
);

// All product cards
List<WebElement> productCards = driver.findElements(
    By.cssSelector(".product-card")
);
System.out.println("Products found: " + productCards.size());

// All product titles (h6 inside card-body)
List<WebElement> titles = driver.findElements(
    By.cssSelector(".card-body h6.card-title")
);

// All "Add to Cart" buttons
List<WebElement> addToCartButtons = driver.findElements(
    By.cssSelector(".card-footer button.btn-primary")
);

// First Add to Cart button
WebElement firstAddToCart = addToCartButtons.get(0);
firstAddToCart.click();
```

---

### Admin Products Modal (`#!/admin/products`)

```html
<button data-bs-toggle="modal" data-bs-target="#productModal"
        ng-click="newProduct()">
    <i class="bi bi-plus-circle"></i> Add Product
</button>

<div class="modal fade" id="productModal">
    <input class="form-control" ng-model="productForm.name">
    <textarea class="form-control" ng-model="productForm.description"></textarea>
    <input type="number" class="form-control" ng-model="productForm.price">
    <input type="number" class="form-control" ng-model="productForm.stock">
    <input class="form-control" ng-model="productForm.category">
    <button class="btn btn-primary" ng-click="saveProduct()">Save</button>
</div>
```

```java
// Add Product button — by ng-click
WebElement addProductBtn = driver.findElement(
    By.cssSelector("button[ng-click='newProduct()']")
);

// Modal — by id
WebElement modal = driver.findElement(By.cssSelector("#productModal"));

// Fields inside modal — scoped by modal id + ng-model
WebElement nameField = driver.findElement(
    By.cssSelector("#productModal input[ng-model='productForm.name']")
);
WebElement descField = driver.findElement(
    By.cssSelector("#productModal textarea[ng-model='productForm.description']")
);
WebElement priceField = driver.findElement(
    By.cssSelector("#productModal input[ng-model='productForm.price']")
);

// Save button inside modal
WebElement saveBtn = driver.findElement(
    By.cssSelector("#productModal button[ng-click='saveProduct()']")
);
```

---

## Complete Login Test Using CSS Selectors

```java
@Test
public void loginWithValidCredentials() {
    navigateTo("login");

    // Fill form using ng-model CSS selectors
    driver.findElement(By.cssSelector("[ng-model='loginData.username']"))
          .sendKeys("admin");

    driver.findElement(By.cssSelector("[ng-model='loginData.password']"))
          .sendKeys("admin123");

    // Click submit button
    driver.findElement(By.cssSelector("button[type='submit']")).click();

    // After login, admin redirects to #!/admin/dashboard
    // Wait handled by implicit wait (set in BaseTest)
    Assert.assertTrue(driver.getCurrentUrl().contains("admin/dashboard"),
        "Admin should land on dashboard after login");
}
```

---

## CSS Selector Pitfalls

| Mistake | Problem | Fix |
|---|---|---|
| `.btn-primary` | Matches many buttons | Be more specific: `button.btn-primary[type='submit']` |
| `input[type='text']` | Matches all text inputs | Scope to form: `form[name='loginForm'] input[type='text']` |
| `[ng-click='...()']` | May match multiple | Combine with tag: `button[ng-click='search()']` |
| Space in `[class='btn primary']` | Wrong — classes don't work like this | Use `.btn.primary` or `[class*='btn']` |
