package com.training.listdemos;

import java.util.ArrayList;

public class ArrayListDemo01 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		ArrayList list  = new ArrayList();
		
		list.add("helloworld");
		list.add(10);			//Integer type object 
		list.add(32.23f);		//Float type object 
		list.add(10);
		
		System.out.println("list :" + list);
		
		for(int i=0; i<list.size(); i++) {
			System.out.println(list.get(i));
		}
	}

}
