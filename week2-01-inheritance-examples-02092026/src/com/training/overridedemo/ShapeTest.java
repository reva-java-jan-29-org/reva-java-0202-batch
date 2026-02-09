package com.training.overridedemo;

import java.util.Scanner;

public class ShapeTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		Scanner scanner = new Scanner(System.in);
		
		System.out.print("\n\t1. Rectangle \n\t2. Circle");
		int choice = scanner.nextInt();
		
		Shape shape = null;
		
		switch (choice) {
		case 1: 
			shape = new Rectangle(10.00, 20.00);
			break;
			
		case 2:
			shape = new Circle(5.45);
			break;

		default:
			throw new IllegalArgumentException("Unexpected value: " + choice);
		}
		
		if(shape!=null) {
			shape.showType();
			System.out.println("Area : " + shape.calculateArea());
		}
		
	}

}
