# 07 — Page Object Model (POM)

## What is Page Object Model?

Page Object Model (POM) is a design pattern where each **web page** (or significant UI component) has a corresponding **Java class**. The class encapsulates:

- The locators for all elements on that page
- Methods that represent user actions on that page

**Benefits:**

| Benefit | Explanation |
|---|---|
| **Reusability** | Login logic written once, used in every test that needs login |
| **Maintainability** | When UI changes, only the Page Object updates — not every test |
| **Readability** | Tests read like user stories: `loginPage.login("admin", "admin123")` |
| **Separation of concerns** | Tests describe WHAT to test; page objects describe HOW to interact |

---

## Playwright POM vs Selenium POM — Comparison

| Aspect | Playwright POM | Selenium POM |
|---|---|---|
| Locator storage | Store `Locator` objects or define them inline in methods | Store `By` locators or `@FindBy` annotations |
| Element re-query | Automatic — `Locator` re-queries on every use | Manual — must re-findElement if DOM changes |
| Waits in page objects | Not needed — actions auto-wait | Often need `WebDriverWait` calls |
| Constructor | Receives `Page` object | Receives `WebDriver` object |
| `PageFactory` | Not used | Optional — `PageFactory.initElements(driver, this)` |
| Return type from methods | `void` or new Page object | `void` or new Page object |

---

## Project Directory Structure

```
src/test/java/com/shopeasy/playwright/
├── base/
│   └── BaseTest.java              ← Playwright setup/teardown
├── pages/
│   ├── LoginPage.java             ← Login form interactions
│   ├── RegisterPage.java          ← Registration form
│   ├── ProductsPage.java          ← Product catalog: search, filter, add to cart
│   ├── CartPage.java              ← Cart: view items, checkout, payment
│   └── AdminProductsPage.java     ← Admin: CRUD product table + modal
└── tests/
    ├── LoginTest.java
    ├── ProductTest.java
    ├── CartTest.java
    └── AdminTest.java
```

---

## LoginPage

Create file: `src/test/java/com/shopeasy/playwright/pages/LoginPage.java`

```java
package com.shopeasy.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Page Object for ShopEasy Login Page.
 * URL: http://localhost:4200/#!/login
 */
public class LoginPage {

    private final Page page;

    // ── Locators ───────────────────────────────────────────────────────
    private final Locator usernameInput;
    private final Locator passwordInput;
    private final Locator loginButton;
    private final Locator errorAlert;
    private final Locator registerLink;

    public LoginPage(Page page) {
        this.page = page;
        // Define locators once in constructor
        this.usernameInput = page.getByPlaceholder("Enter your username");
        this.passwordInput = page.getByPlaceholder("Enter your password");
        this.loginButton   = page.locator("button[type='submit']");
        this.errorAlert    = page.locator(".alert-danger");
        this.registerLink  = page.locator("a[href='#!/register']");
    }

    // ── Navigation ─────────────────────────────────────────────────────

    public LoginPage navigate() {
        page.navigate("http://localhost:4200/#!/login");
        return this;
    }

    // ── Actions ────────────────────────────────────────────────────────

    public LoginPage enterUsername(String username) {
        usernameInput.fill(username);
        return this;
    }

    public LoginPage enterPassword(String password) {
        passwordInput.fill(password);
        return this;
    }

    public LoginPage clickLoginButton() {
        loginButton.click();
        return this;
    }

    /**
     * Complete login flow — fills credentials and clicks submit.
     * Returns ProductsPage for customer login, or use waitForAdminDashboard() for admin.
     */
    public void login(String username, String password) {
        usernameInput.fill(username);
        passwordInput.fill(password);
        loginButton.click();
    }

    public ProductsPage loginAsCustomer(String username, String password) {
        login(username, password);
        page.waitForURL("**/#!/products");
        return new ProductsPage(page);
    }

    public void loginAsAdmin(String username, String password) {
        login(username, password);
        page.waitForURL("**/#!/admin/dashboard");
    }

    public RegisterPage clickRegisterLink() {
        registerLink.click();
        page.waitForURL("**/#!/register");
        return new RegisterPage(page);
    }

    // ── Assertions / State Getters ─────────────────────────────────────

    public boolean isErrorVisible() {
        return errorAlert.isVisible();
    }

    public String getErrorText() {
        return errorAlert.textContent();
    }

    public void assertLoginPageVisible() {
        assertThat(page).hasURL("**/#!/login");
        assertThat(usernameInput).isVisible();
        assertThat(passwordInput).isVisible();
        assertThat(loginButton).isVisible();
    }

    public void assertErrorContains(String text) {
        assertThat(errorAlert).isVisible();
        assertThat(errorAlert).containsText(text);
    }
}
```

---

## RegisterPage

Create file: `src/test/java/com/shopeasy/playwright/pages/RegisterPage.java`

```java
package com.shopeasy.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Page Object for ShopEasy Registration Page.
 * URL: http://localhost:4200/#!/register
 */
public class RegisterPage {

    private final Page page;

    private final Locator firstNameInput;
    private final Locator lastNameInput;
    private final Locator emailInput;
    private final Locator usernameInput;
    private final Locator passwordInput;
    private final Locator registerButton;
    private final Locator successAlert;
    private final Locator loginLink;

    public RegisterPage(Page page) {
        this.page = page;
        this.firstNameInput  = page.getByPlaceholder("Enter your first name");
        this.lastNameInput   = page.getByPlaceholder("Enter your last name");
        this.emailInput      = page.getByPlaceholder("Enter your email");
        this.usernameInput   = page.getByPlaceholder("Choose a username");
        this.passwordInput   = page.getByPlaceholder("Create a password");
        this.registerButton  = page.locator("button[type='submit']");
        this.successAlert    = page.locator(".alert-success");
        this.loginLink       = page.locator("a[href='#!/login']");
    }

    public RegisterPage navigate() {
        page.navigate("http://localhost:4200/#!/register");
        return this;
    }

    public RegisterPage fillFirstName(String firstName) {
        firstNameInput.fill(firstName);
        return this;
    }

    public RegisterPage fillLastName(String lastName) {
        lastNameInput.fill(lastName);
        return this;
    }

    public RegisterPage fillEmail(String email) {
        emailInput.fill(email);
        return this;
    }

    public RegisterPage fillUsername(String username) {
        usernameInput.fill(username);
        return this;
    }

    public RegisterPage fillPassword(String password) {
        passwordInput.fill(password);
        return this;
    }

    public void clickRegister() {
        registerButton.click();
    }

    public LoginPage registerUser(String firstName, String lastName,
                                  String email, String username, String password) {
        fillFirstName(firstName)
            .fillLastName(lastName)
            .fillEmail(email)
            .fillUsername(username)
            .fillPassword(password);
        clickRegister();
        assertThat(successAlert).isVisible();
        loginLink.click();
        return new LoginPage(page);
    }
}
```

---

## ProductsPage

Create file: `src/test/java/com/shopeasy/playwright/pages/ProductsPage.java`

```java
package com.shopeasy.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Page Object for ShopEasy Products Page.
 * URL: http://localhost:4200/#!/products
 */
public class ProductsPage {

    private final Page page;

    private final Locator searchInput;
    private final Locator categoryDropdown;
    private final Locator productCards;
    private final Locator successAlert;
    private final Locator cartNavLink;

    public ProductsPage(Page page) {
        this.page = page;
        this.searchInput      = page.getByPlaceholder("Search products...");
        this.categoryDropdown = page.locator("select[ng-model='selectedCategory']");
        this.productCards     = page.locator(".product-card");
        this.successAlert     = page.locator(".alert-success");
        this.cartNavLink      = page.locator("a[href='#!/cart']");
    }

    public ProductsPage navigate() {
        page.navigate("http://localhost:4200/#!/products");
        return this;
    }

    // ── Wait for page ready ────────────────────────────────────────────

    public ProductsPage waitForProductsToLoad() {
        assertThat(productCards.first()).isVisible();
        return this;
    }

    // ── Actions ────────────────────────────────────────────────────────

    public ProductsPage searchFor(String term) {
        searchInput.fill(term);
        return this;
    }

    public ProductsPage clearSearch() {
        searchInput.clear();
        return this;
    }

    public ProductsPage selectCategory(String category) {
        categoryDropdown.selectOption(category);
        return this;
    }

    public ProductsPage resetCategory() {
        categoryDropdown.selectOption("All Categories");
        return this;
    }

    public ProductsPage addToCart(String productName) {
        productCards
            .filter(new Locator.FilterOptions().setHasText(productName))
            .locator("button.btn-primary")
            .click();
        assertThat(successAlert).isVisible();
        return this;
    }

    public ProductsPage addFirstProductToCart() {
        productCards.first().locator("button.btn-primary").click();
        assertThat(successAlert).isVisible();
        return this;
    }

    public CartPage goToCart() {
        cartNavLink.click();
        page.waitForURL("**/#!/cart");
        return new CartPage(page);
    }

    // ── Getters / State ────────────────────────────────────────────────

    public int getProductCount() {
        return productCards.count();
    }

    public String getFirstProductName() {
        return productCards.first().locator("h5").textContent().trim();
    }

    public String getProductPrice(String productName) {
        return productCards
            .filter(new Locator.FilterOptions().setHasText(productName))
            .locator(".text-success")
            .textContent()
            .trim();
    }

    public List<String> getAllProductNames() {
        return productCards.all().stream()
            .map(card -> card.locator("h5").textContent().trim())
            .toList();
    }

    public boolean isProductVisible(String productName) {
        return productCards
            .filter(new Locator.FilterOptions().setHasText(productName))
            .isVisible();
    }

    // ── Assertions ─────────────────────────────────────────────────────

    public void assertProductsPageVisible() {
        assertThat(page).hasURL("**/#!/products");
        assertThat(productCards.first()).isVisible();
    }

    public void assertProductVisible(String productName) {
        assertThat(
            productCards.filter(new Locator.FilterOptions().setHasText(productName))
        ).isVisible();
    }

    public void assertSuccessAlertContains(String text) {
        assertThat(successAlert).isVisible();
        assertThat(successAlert).containsText(text);
    }
}
```

---

## CartPage

Create file: `src/test/java/com/shopeasy/playwright/pages/CartPage.java`

```java
package com.shopeasy.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Page Object for ShopEasy Cart Page.
 * URL: http://localhost:4200/#!/cart
 */
public class CartPage {

    private final Page page;

    private final Locator cartItems;
    private final Locator checkoutButton;
    private final Locator cardNumberInput;
    private final Locator cardExpiryInput;
    private final Locator cardCvvInput;
    private final Locator payNowButton;
    private final Locator orderSuccessMessage;
    private final Locator emptyCartMessage;

    public CartPage(Page page) {
        this.page = page;
        this.cartItems           = page.locator(".cart-item");
        this.checkoutButton      = page.getByText("Proceed to Checkout");
        this.cardNumberInput     = page.getByPlaceholder("Card Number");
        this.cardExpiryInput     = page.getByPlaceholder("MM/YY");
        this.cardCvvInput        = page.getByPlaceholder("CVV");
        this.payNowButton        = page.locator("button.btn-success");
        this.orderSuccessMessage = page.locator(".alert-success");
        this.emptyCartMessage    = page.getByText("Your cart is empty");
    }

    public CartPage navigate() {
        page.navigate("http://localhost:4200/#!/cart");
        return this;
    }

    public int getCartItemCount() {
        return cartItems.count();
    }

    public boolean isCartEmpty() {
        return emptyCartMessage.isVisible();
    }

    public CartPage clickCheckout() {
        checkoutButton.click();
        return this;
    }

    public CartPage enterCardDetails(String cardNumber, String expiry, String cvv) {
        cardNumberInput.fill(cardNumber);
        cardExpiryInput.fill(expiry);
        cardCvvInput.fill(cvv);
        return this;
    }

    public CartPage clickPayNow() {
        payNowButton.click();
        return this;
    }

    public CartPage placeOrder(String cardNumber, String expiry, String cvv) {
        clickCheckout();
        enterCardDetails(cardNumber, expiry, cvv);
        clickPayNow();
        return this;
    }

    public void assertCartHasItems() {
        assertThat(cartItems.first()).isVisible();
    }

    public void assertOrderSuccessful() {
        assertThat(orderSuccessMessage).isVisible();
        assertThat(orderSuccessMessage).containsText("Order");
    }

    public void assertCartEmpty() {
        assertThat(emptyCartMessage).isVisible();
    }
}
```

---

## Test Class Using Page Objects

Create file: `src/test/java/com/shopeasy/playwright/tests/LoginTest.java`

```java
package com.shopeasy.playwright.tests;

import com.shopeasy.playwright.base.BaseTest;
import com.shopeasy.playwright.pages.LoginPage;
import com.shopeasy.playwright.pages.ProductsPage;
import org.testng.Assert;
import org.testng.annotations.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class LoginTest extends BaseTest {

    @Test
    public void successfulCustomerLogin() {
        LoginPage loginPage = new LoginPage(page).navigate();
        loginPage.assertLoginPageVisible();

        ProductsPage productsPage = loginPage.loginAsCustomer("customer1", "pass123");
        productsPage.assertProductsPageVisible();
    }

    @Test
    public void successfulAdminLogin() {
        LoginPage loginPage = new LoginPage(page).navigate();
        loginPage.loginAsAdmin("admin", "admin123");

        assertThat(page).hasURL("**/#!/admin/dashboard");
    }

    @Test
    public void invalidCredentialsShowError() {
        LoginPage loginPage = new LoginPage(page).navigate();
        loginPage.login("baduser", "badpass");

        loginPage.assertErrorContains("Invalid");
        assertThat(page).hasURL("**/#!/login");  // Should NOT navigate away
    }

    @Test
    public void navigateToRegisterPage() {
        LoginPage loginPage = new LoginPage(page).navigate();
        loginPage.clickRegisterLink();

        assertThat(page).hasURL("**/#!/register");
    }
}
```

---

## Test Class: ProductTest.java

```java
package com.shopeasy.playwright.tests;

import com.shopeasy.playwright.base.BaseTest;
import com.shopeasy.playwright.pages.CartPage;
import com.shopeasy.playwright.pages.LoginPage;
import com.shopeasy.playwright.pages.ProductsPage;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ProductTest extends BaseTest {

    private ProductsPage productsPage;

    @BeforeMethod
    public void loginAndGoToProducts() {
        // Login and land on products page
        productsPage = new LoginPage(page)
            .navigate()
            .loginAsCustomer("customer1", "pass123");
        productsPage.waitForProductsToLoad();
    }

    @Test
    public void productsPageLoadsWithItems() {
        int count = productsPage.getProductCount();
        Assert.assertTrue(count > 0, "Products page should show at least one product");
        System.out.println("Total products loaded: " + count);
    }

    @Test
    public void searchFiltersProducts() {
        int totalCount = productsPage.getProductCount();

        productsPage.searchFor("laptop");
        int filteredCount = productsPage.getProductCount();

        System.out.println("All: " + totalCount + " | Filtered: " + filteredCount);
        Assert.assertTrue(filteredCount <= totalCount, "Search should filter down results");
    }

    @Test
    public void categoryFilterWorks() {
        productsPage.selectCategory("Electronics");
        int electronicsCount = productsPage.getProductCount();

        productsPage.resetCategory();
        int allCount = productsPage.getProductCount();

        Assert.assertTrue(electronicsCount <= allCount, "Category filter should reduce count");
    }

    @Test
    public void addProductToCartAndCheckout() {
        // Add product to cart
        productsPage.addFirstProductToCart();

        // Navigate to cart
        CartPage cartPage = productsPage.goToCart();
        cartPage.assertCartHasItems();

        int itemCount = cartPage.getCartItemCount();
        Assert.assertTrue(itemCount >= 1, "Cart should have at least 1 item");
    }
}
```

---

## Fluent Interface Pattern (Method Chaining)

Notice how page object methods return `this` (for chaining on the same page) or a new page object (when navigation occurs):

```java
// Chaining on the same page object
new LoginPage(page)
    .navigate()
    .enterUsername("admin")
    .enterPassword("admin123")
    .clickLoginButton();

// Chaining across page objects (navigation)
new LoginPage(page)
    .navigate()
    .loginAsCustomer("customer1", "pass123")   // returns ProductsPage
    .waitForProductsToLoad()
    .searchFor("laptop")                        // returns ProductsPage
    .addFirstProductToCart()                    // returns ProductsPage
    .goToCart()                                 // returns CartPage
    .assertCartHasItems();
```

---

## Summary: Key Differences from Selenium POM

```
SELENIUM POM                          PLAYWRIGHT POM
─────────────────────────────────     ─────────────────────────────────
Constructor receives WebDriver        Constructor receives Page
Uses By locators (static)             Uses Locator objects (lazy, re-queried)
Must add waits in methods             No waits needed — actions auto-wait
findElement() throws if not found     Locator.click() waits and then acts
@FindBy annotation optional           No annotation equivalent
StaleElementReferenceException risk   No stale element issue — re-queried
PageFactory.initElements() optional   Not applicable
```
