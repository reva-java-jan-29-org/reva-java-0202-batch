package com.training.listdemos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArrayListSortingDemo01 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		List<Integer> numbers = new ArrayList<Integer>();
		
		numbers.add(34);
		numbers.add(4);
		numbers.add(65);
		numbers.add(12);
		numbers.add(5);
		numbers.add(65);
		numbers.add(77);
		numbers.add(41);
		numbers.add(3);
		
		Collections.sort(numbers);
		
		System.out.println(numbers);

		
	}

}
