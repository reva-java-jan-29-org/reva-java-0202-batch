package com.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CaculatorTest {
	
	@BeforeAll
	public static void beforeAllTheTests() {
		System.out.println("This is method execute only once before all the tests ");
	}
	
	@BeforeEach
	public void beforeEachTests() {
		System.out.println("This method executes the code before each test");
	}
	
	@AfterEach
	public void afterEachTest() {
		System.out.println("This executes after each test");
	}
	
	@Test
	public void shouldAddTwoNumbersCorrectly() {
		//arrange
		Calculator calculator = new Calculator();
		
		//act
		int result = calculator.add(2, 3);
		
		//assert
		assertEquals(5, result);
	}
	
	public void shouldThrowExceptionWhenDividingByZero() {
		Calculator calculator = new Calculator();
		
		Exception exception = assertThrows(IllegalArgumentException.class, ()-> calculator.divide(10, 0));
	
		assertEquals("Division by zero is not allowed", exception.getMessage());
	}
	
	@Test
	public void test1() {
		
	}
	
	@Test
	public void test2() {
		
	}
	

}
