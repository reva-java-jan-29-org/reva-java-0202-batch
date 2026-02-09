package com.training.overridedemo;

public class Rectangle extends Shape {

	private double length, breadth;

	public Rectangle() {
		super();
	}

	public Rectangle(double length, double breadth) {
		super();
		this.length = length;
		this.breadth = breadth;
	}

	@Override
	public double calculateArea() {
		// TODO Auto-generated method stub
		return length * breadth;
	}

	public void showType() {
		System.out.println("This is a Rectangle");
	}
}
