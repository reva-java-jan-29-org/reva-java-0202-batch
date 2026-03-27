package com.testing;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

public class FirstTest extends BaseTest {

	@Test
    public void verifyPageTitle() {
        // Step 1: Open the ShopEasy login page
        driver.get(BASE_URL + "/#!/login");

        // Step 2: Get the page title from the browser tab
        String title = driver.getTitle();
        System.out.println("Page title: " + title);

        // Step 3: Assert it matches the expected title
        Assert.assertEquals(title, "ShopEasy - E-Commerce",
            "Page title did not match!");
    }
	
	@Test
    public void verifyLoginFormVisible() {
        // Navigate to login page
        driver.get(BASE_URL + "/#!/login");

        // Find the username input using CSS attribute selector
        // (no id on this input — common in AngularJS apps)
        WebElement usernameInput = driver.findElement(
            By.cssSelector("input[placeholder='Enter your username']")
        );

        WebElement passwordInput = driver.findElement(
            By.cssSelector("input[type='password']")
        );

        WebElement loginButton = driver.findElement(
            By.cssSelector("button[type='submit']")
        );

        
        
        // Assert elements are visible on the page
        Assert.assertTrue(usernameInput.isDisplayed(), "Username field not visible");
        Assert.assertTrue(passwordInput.isDisplayed(), "Password field not visible");
        Assert.assertTrue(loginButton.isDisplayed(), "Login button not visible");

        // Print element tag for reference
        System.out.println("Username input tag: " + usernameInput.getTagName());
    }
}
