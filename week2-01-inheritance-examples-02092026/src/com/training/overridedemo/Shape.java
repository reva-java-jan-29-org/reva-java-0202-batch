package com.training.overridedemo;

public abstract class Shape {
	
	
	
	
	public abstract double calculateArea();

	public final void showType() {
		System.out.println("This is a shape");
	}
}
