package com.testing;

import org.testng.annotations.Test;

import com.microsoft.playwright.Locator;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class FirstTest extends BaseTest {

    @Test
    public void verifyPageTitle() {
        // Step 1: Navigate to the ShopEasy login page
        page.navigate(BASE_URL + "/#!/login");

        // Step 2: Get the page title from the browser tab
        String title = page.title();
        System.out.println("Page title: " + title);

        // Step 3: Assert using Playwright's built-in assertThat
        assertThat(page).hasTitle("ShopEasy - E-Commerce");
    }

    @Test
    public void verifyLoginFormVisible() {
        // Navigate to login page
        navigateTo("login");

        // Playwright auto-waits for these elements to be visible before asserting
        // No explicit wait code needed!
        Locator usernameInput = page.locator("input[placeholder='Enter your username']");
        Locator passwordInput = page.locator("input[type='password']");
        Locator loginButton   = page.locator("button[type='submit']");

        // Assert elements are visible — Playwright waits up to 30s automatically
        assertThat(usernameInput).isVisible();
        assertThat(passwordInput).isVisible();
        assertThat(loginButton).isVisible();

        System.out.println("All login form elements are visible");
    }
}