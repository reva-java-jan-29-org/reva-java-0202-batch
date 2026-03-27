package com.testing;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

public class BaseTest {

    // ── Playwright object hierarchy ──────────────────────────────────
    protected Playwright playwright;    // Entry point — one per test run
    protected Browser     browser;      // The browser process
    protected BrowserContext context;   // Isolated session (like incognito)
    protected Page        page;         // The browser tab you interact with

    // Base URL of the ShopEasy frontend
    protected static final String BASE_URL = "http://127.0.0.1:5500/frontend";

    @BeforeMethod
    public void setUp() {
        // Step 1: Create the Playwright entry point
        playwright = Playwright.create();

        // Step 2: Launch Chromium (non-headless by default — you can see the browser)
        browser = playwright.chromium().launch(
            new BrowserType.LaunchOptions()
                .setHeadless(false)         // true = no visible window (for CI)
                .setSlowMo(0)               // add delay (ms) between actions for debugging
        );

        // Step 3: Create an isolated browser context
        // Each test gets a fresh context — no cookies, no localStorage from other tests
        context = browser.newContext(
            new Browser.NewContextOptions()
                .setViewportSize(1280, 720)   // Browser window size
        );

        // Step 4: Create a new page (tab) inside the context
        page = context.newPage();
    }

    @AfterMethod
    public void tearDown() {
        // Close in reverse order: Page → Context → Browser → Playwright
        if (page != null)       page.close();
        if (context != null)    context.close();
        if (browser != null)    browser.close();
        if (playwright != null) playwright.close();
    }

    // ── Helper methods ────────────────────────────────────────────────

    /**
     * Navigate to a specific route in the ShopEasy SPA.
     * Examples: navigateTo("login"), navigateTo("products"), navigateTo("admin/dashboard")
     */
    protected void navigateTo(String route) {
        page.navigate(BASE_URL + "/#!/" + route);
    }
}
