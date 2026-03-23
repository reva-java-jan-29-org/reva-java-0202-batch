# 19 — Handle Broken Links, SVG Elements & Shadow DOM

## Part A: Handle Broken Links

## What Is a Broken Link?

A broken link is an `<a>` tag whose `href` returns an HTTP error code (4xx, 5xx). Common causes:
- Page deleted/moved without redirects
- Typo in URL
- External site went down

Selenium alone can't detect broken links — it navigates to pages, not validate HTTP status. Combine Selenium (to find links) with `java.net.HttpURLConnection` (to check HTTP status).

---

## Strategy: Find All Links → Check Each HTTP Status

```java
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Test
public void findAllBrokenLinksOnProductsPage() throws Exception {
    navigateTo("products");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".product-card")));

    // Get all anchor tags
    List<WebElement> linkElements = driver.findElements(By.tagName("a"));
    System.out.println("Total links found: " + linkElements.size());

    // Collect all href values (do this first — stale element if we navigate away)
    List<String> hrefs = new ArrayList<>();
    for (WebElement link : linkElements) {
        String href = link.getAttribute("href");
        if (href != null && !href.isEmpty()
            && !href.startsWith("javascript")
            && !href.startsWith("mailto")
            && !href.startsWith("#")) {
            hrefs.add(href);
        }
    }

    System.out.println("Valid http links to check: " + hrefs.size());

    // Check each link
    List<String> brokenLinks = new ArrayList<>();
    List<String> workingLinks = new ArrayList<>();

    for (String href : hrefs) {
        int responseCode = getHttpResponseCode(href);
        if (responseCode >= 400) {
            brokenLinks.add(href + " [" + responseCode + "]");
            System.out.println("❌ BROKEN: " + href + " → " + responseCode);
        } else {
            workingLinks.add(href);
            System.out.println("✓ OK: " + href + " → " + responseCode);
        }
    }

    System.out.println("\n=== Summary ===");
    System.out.println("Working: " + workingLinks.size());
    System.out.println("Broken: " + brokenLinks.size());
    brokenLinks.forEach(System.out::println);

    // Assert no broken links
    Assert.assertEquals(brokenLinks.size(), 0,
        "Broken links found: " + brokenLinks);
}

private int getHttpResponseCode(String url) {
    try {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("HEAD");    // HEAD is faster than GET
        connection.setConnectTimeout(5000);     // 5 second timeout
        connection.setReadTimeout(5000);
        connection.connect();
        return connection.getResponseCode();
    } catch (Exception e) {
        System.err.println("Connection failed for: " + url + " - " + e.getMessage());
        return 999; // Custom code for connection error
    }
}
```

---

## ShopEasy's Internal Links (Hash Routes)

ShopEasy uses hash routing (`#!/products`, `#!/login`). The browser doesn't make a server request for hash-only changes — they're handled client-side by AngularJS. So `getHttpResponseCode("http://localhost:4200/#!/admin/products")` will return 200 for all routes because the server returns `index.html` for all paths.

**For ShopEasy, checking broken links means:**
1. Finding links that navigate to routes that don't exist in the AngularJS router
2. Checking external URLs embedded in the app (CDN links, image URLs)

```java
@Test
public void verifyNavbarLinksNavigate() {
    navigateTo("products");

    // Guest nav links
    List<WebElement> navLinks = driver.findElements(By.cssSelector(".navbar-nav .nav-link"));

    for (WebElement link : navLinks) {
        String href = link.getAttribute("href");
        String text = link.getText().trim();

        if (href != null && href.contains("#!/")) {
            System.out.println("Nav link: " + text + " → " + href);

            // Click and verify page loads (no 404 component shown)
            link.click();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            // SPA route change — just verify URL changed
            Assert.assertTrue(driver.getCurrentUrl().contains("#!/"),
                "URL should still be in the SPA: " + driver.getCurrentUrl());
        }
    }
}
```

---

## Part B: SVG Elements

## What Are SVG Elements?

SVG (Scalable Vector Graphics) elements render icons and graphics. ShopEasy uses Bootstrap Icons extensively:

```html
<!-- Bootstrap Icons render as <i> tags linked to CSS font -->
<i class="bi bi-cart-plus"></i>
<i class="bi bi-search"></i>
<i class="bi bi-person"></i>

<!-- SVG elements from inline SVG or image tags -->
<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16">
    <path d="M..."/>
</svg>
```

---

## Interacting with SVG Elements

Standard Selenium locators work for SVG elements but require SVG-specific XPath namespace handling.

### Finding SVG Elements

```java
// Bootstrap Icons in ShopEasy are <i> tags (CSS-based, not inline SVG)
// Find them like any other element
WebElement cartIcon = driver.findElement(By.cssSelector(".bi-cart-plus"));
System.out.println("Cart icon visible: " + cartIcon.isDisplayed());

// For true inline SVG:
WebElement svgElement = driver.findElement(By.cssSelector("svg"));
System.out.println("SVG width: " + svgElement.getAttribute("width"));

// XPath with SVG namespace
WebElement svgPath = driver.findElement(
    By.xpath("//*[name()='svg']")
);
WebElement svgPathElement = driver.findElement(
    By.xpath("//*[name()='path'][@d]")
);
```

### Click on SVG Icon (ShopEasy)

ShopEasy's icons are inside clickable elements (buttons, links). Click the **parent** element, not the `<i>` icon itself:

```java
// WRONG — clicking the icon alone may not trigger the action
// driver.findElement(By.cssSelector(".bi-cart-plus")).click();

// CORRECT — click the button containing the icon
WebElement addToCartBtn = driver.findElement(
    By.cssSelector(".card-footer button.btn-primary")
);
addToCartBtn.click();

// OR: click button that CONTAINS the icon
WebElement btnWithIcon = driver.findElement(
    By.xpath("//button[.//i[contains(@class,'bi-cart-plus')]]")
);
btnWithIcon.click();
```

### Verify Icon Presence

```java
// Verify the shopping cart icon appears in navbar
WebElement cartNavIcon = driver.findElement(
    By.cssSelector("a[href='#!/cart'] .bi-cart3")
);
Assert.assertTrue(cartNavIcon.isDisplayed(), "Cart icon should be visible");

// Verify badge count on cart icon
WebElement cartBadge = driver.findElement(
    By.cssSelector("a[href='#!/cart'] .badge.bg-danger")
);
if (cartBadge.isDisplayed()) {
    System.out.println("Cart count: " + cartBadge.getText());
}
```

### True Inline SVG Interaction

```java
// Interact with inline SVG element
WebElement svg = driver.findElement(By.xpath("//*[name()='svg' and @class='icon']"));

// Get SVG attributes
String viewBox = svg.getAttribute("viewBox");
System.out.println("ViewBox: " + viewBox);

// Click SVG element
svg.click();

// Find a path within SVG
WebElement path = svg.findElement(By.xpath(".//*[name()='path']"));
System.out.println("Path data: " + path.getAttribute("d"));

// Find SVG with specific title (accessibility)
WebElement svgWithTitle = driver.findElement(
    By.xpath("//*[name()='svg'][.//*[name()='title'][text()='Shopping Cart']]")
);
```

---

## Part C: Shadow DOM

## What Is Shadow DOM?

Shadow DOM is a web component encapsulation mechanism. Elements inside a Shadow DOM are **isolated from the main document** — regular `findElement()` cannot reach them.

```html
<!-- Host element -->
<custom-element id="myComponent">
    <!-- Shadow root (encapsulated) -->
    #shadow-root (open)
        <div class="internal-content">
            <input type="text" placeholder="Shadow input">
        </div>
</custom-element>
```

---

## Selenium 4 — Shadow DOM Support

Selenium 4 introduced native `getShadowRoot()`:

```java
// Step 1: Find the shadow host element
WebElement shadowHost = driver.findElement(By.cssSelector("custom-element#myComponent"));

// Step 2: Get the shadow root
SearchContext shadowRoot = shadowHost.getShadowRoot();

// Step 3: Find elements within the shadow root
WebElement inputInShadow = shadowRoot.findElement(By.cssSelector("input[type='text']"));
inputInShadow.sendKeys("Hello Shadow DOM");
System.out.println("Typed into shadow input: " + inputInShadow.getAttribute("value"));
```

### JavascriptExecutor Approach (Selenium 3 Compatible)

```java
JavascriptExecutor js = (JavascriptExecutor) driver;

// Access shadow root via JS
WebElement shadowHost = driver.findElement(By.cssSelector("custom-element"));
WebElement shadowInput = (WebElement) js.executeScript(
    "return arguments[0].shadowRoot.querySelector('input');",
    shadowHost
);
shadowInput.sendKeys("Hello via JS");
```

---

## Nested Shadow DOMs

```java
// Outer shadow host
WebElement outerHost = driver.findElement(By.cssSelector("outer-component"));
SearchContext outerShadow = outerHost.getShadowRoot();

// Inner shadow host (inside outer shadow)
WebElement innerHost = outerShadow.findElement(By.cssSelector("inner-component"));
SearchContext innerShadow = innerHost.getShadowRoot();

// Element inside inner shadow
WebElement deepInput = innerShadow.findElement(By.cssSelector("input"));
deepInput.sendKeys("Deep Shadow DOM value");
```

---

## ShopEasy Context — No Shadow DOM

ShopEasy does not currently use Web Components or Shadow DOM. All content is in the main document.

However, **browser native UI elements** like:
- `<input type="date">` date picker UI (Chrome)
- `<input type="color">` color picker
- `<video controls>` player controls

...are rendered via Shadow DOM internally by the browser.

```java
// Example: Interacting with Chrome's native date input's internal shadow
// (This is complex and usually not needed — just sendKeys("yyyy-MM-dd") works)
WebElement dateInput = driver.findElement(By.cssSelector("input[type='date']"));

// For Chrome's internal shadow (implementation detail — may break with Chrome updates):
SearchContext shadow = dateInput.getShadowRoot();
// Typically: sendKeys("2026-03-15") is sufficient without touching shadow root
dateInput.sendKeys("2026-03-15");
```

---

## Complete Test: SVG Icons + Broken Links

```java
@Test
public void verifyBootstrapIconsAndLinks() {
    navigateTo("login");

    // ── 1. Verify Bootstrap Icons load (bi classes present) ──────────
    List<WebElement> icons = driver.findElements(By.cssSelector("i[class*='bi-']"));
    System.out.println("Bootstrap icons on login page: " + icons.size());
    Assert.assertTrue(icons.size() > 0, "At least one Bootstrap Icon should be present");

    // Check icon renders (has non-zero size)
    WebElement shopIcon = driver.findElement(By.cssSelector(".bi-shop"));
    // Bootstrap Icons load via CSS font — size indicates it loaded
    String fontSize = shopIcon.getCssValue("font-size");
    System.out.println("Icon font size: " + fontSize);

    // ── 2. Check nav links are not broken (return 200) ───────────────
    List<WebElement> navLinks = driver.findElements(By.cssSelector(".navbar-nav a"));
    System.out.println("\nNavbar links:");
    for (WebElement link : navLinks) {
        String href = link.getAttribute("href");
        String text = link.getText().trim();
        if (href != null && href.startsWith("http")) {
            int status = getHttpResponseCode(href);
            System.out.printf("  [%d] %s → %s%n", status, text, href);
        }
    }

    // ── 3. Verify Bootstrap CDN link is accessible ───────────────────
    // The app loads Bootstrap CSS and Icons from CDN
    // Check that CDN resources are accessible
    int bootstrapStatus = getHttpResponseCode(
        "https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css"
    );
    System.out.println("Bootstrap CDN status: " + bootstrapStatus);
    // In a real test environment with internet access:
    // Assert.assertEquals(bootstrapStatus, 200);
}
```
