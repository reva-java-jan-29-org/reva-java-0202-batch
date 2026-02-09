package com.training.overridedemo;

public class Circle extends Shape {

	private double radius;

	public Circle() {
		super();
	}

	public Circle(double radius) {
		super();
		this.radius = radius;
	}

	@Override
	public double calculateArea() {
		// TODO Auto-generated method stub
		return Math.PI * radius * radius;
	}

	@Override
	public void showType() {
		// TODO Auto-generated method stub
		System.out.println("This is Circle");
	}

}
