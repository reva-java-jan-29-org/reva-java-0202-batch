package com.training.listdemos;

import java.util.ArrayList;

public class ArrayListDemo02 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		ArrayList<Integer> listNumbers = new ArrayList<>(10);
		
		listNumbers.add(30);
		listNumbers.add(54);
		listNumbers.add(0, 100);
		listNumbers.add(34);
		listNumbers.add(23);
		listNumbers.add(12);
		listNumbers.add(43);
		listNumbers.add(4);
		listNumbers.add(3);
		listNumbers.add(3);
		
		System.out.println(listNumbers);
		
		listNumbers.add(65);
		
		System.out.println(listNumbers);
	}

}
