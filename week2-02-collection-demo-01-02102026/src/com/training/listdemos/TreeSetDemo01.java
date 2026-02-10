package com.training.listdemos;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

public class TreeSetDemo01 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		
		Set<String> namesSet = new TreeSet<String>(Arrays.asList("Omkar","Aryan","Sandhya","Rutuja","Monali","Rishav"));

		System.out.println(namesSet);
	}

}
