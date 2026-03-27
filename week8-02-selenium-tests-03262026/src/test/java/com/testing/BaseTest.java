package com.testing;

import java.time.Duration;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;


public class BaseTest {

	// WebDriver instance — one per test method (thread-safe for now)
	protected WebDriver driver;

	// Base URL of the ShopEasy frontend
	protected static final String BASE_URL = "http://127.0.0.1:5500/frontend";

	@BeforeMethod
	public void setUp() {
		// ChromeOptions lets you configure Chrome behavior
		ChromeOptions options = new ChromeOptions();

		// Run Chrome in a standard window
		// For headless mode, see: 18-screenshots-ssl-headless.md

		// Create ChromeDriver — Selenium Manager auto-downloads ChromeDriver
		driver = new ChromeDriver(options);

		// Maximize the browser window
		driver.manage().window().maximize();

		// Implicit wait: if an element is not found immediately,
		// wait up to 10 seconds before throwing NoSuchElementException
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

		// Page load timeout: wait up to 30 seconds for a page to fully load
		driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
	}

	@AfterMethod
	public void tearDown() {
		// Always close the browser after each test method
		// driver.close() — closes current window only
		// driver.quit() — closes ALL windows AND kills the driver process
		if (driver != null) {
			driver.quit();
		}
	}

	// ── Helper methods ────────────────────────────────────────────────

	/**
	 * Navigate to a specific route in the ShopEasy SPA. Examples:
	 * navigateTo("login"), navigateTo("products"), navigateTo("admin/dashboard")
	 */
	protected void navigateTo(String route) {
		driver.get(BASE_URL + "/#!/" + route);
	}
	
}
