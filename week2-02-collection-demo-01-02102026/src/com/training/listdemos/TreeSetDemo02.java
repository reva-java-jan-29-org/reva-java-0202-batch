package com.training.listdemos;

import java.util.Set;
import java.util.TreeSet;

public class TreeSetDemo02 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		Set<Employee> set = new TreeSet<>(new EmployeeNameComparator());
		
		
		set.add(new Employee(101, "Aryan", 19000.00));
		set.add(new Employee(102, "Ashwin", 34000.00));
		set.add(new Employee(103, "Mayuri", 34000.00));
		set.add(new Employee(104, "Monali", 54000.00));
		set.add(new Employee(105, "Omkar", 43000.00));
		
		System.out.println(set);
	}

}
