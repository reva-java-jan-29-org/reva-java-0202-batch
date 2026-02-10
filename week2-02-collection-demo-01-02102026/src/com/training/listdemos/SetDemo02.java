package com.training.listdemos;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

public class SetDemo02 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Set<Integer> set = new TreeSet<>();
		
		set.add(98);
		set.add(12);
		set.add(45);
		set.add(43);
		set.add(23);
		set.add(66);
		set.add(55);
		
		System.out.println(set);

	}

}
