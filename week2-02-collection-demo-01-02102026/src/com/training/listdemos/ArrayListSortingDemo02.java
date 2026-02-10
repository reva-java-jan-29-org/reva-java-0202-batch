package com.training.listdemos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ArrayListSortingDemo02 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		List<String> names = new ArrayList<>(Arrays.asList("Omkar","Aryan","Sandhya","Rutuja","Monali","Rishav"));
		
		names.add("Mayuri");
		
		Collections.sort(names);
		
		System.out.println(names);

	}

}
