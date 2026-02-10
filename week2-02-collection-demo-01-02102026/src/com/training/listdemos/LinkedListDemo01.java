package com.training.listdemos;

import java.util.LinkedList;
import java.util.List;

public class LinkedListDemo01 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		List<Integer> list = new LinkedList<>();
		
		
		list.add(10);
		list.add(43);
		list.add(2, 65);
		list.add(45);
		list.add(64);
		list.add(23);
		
		System.out.println(list);
	}

}
