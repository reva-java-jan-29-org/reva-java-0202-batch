package com.training.listdemos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArraySortingDemo03 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		List<Employee> employeeList = new ArrayList<>();
		
		
		employeeList.add(new Employee(101, "Aryan", 19000.00));
		employeeList.add(new Employee(102, "Ashwin", 34000.00));
		employeeList.add(new Employee(103, "Mayuri", 34000.00));
		employeeList.add(new Employee(104, "Monali", 54000.00));
		employeeList.add(new Employee(105, "Omkar", 43000.00));
		
		
		Collections.sort(employeeList, new EmployeeNameComparator());
		
		System.out.println(employeeList);
		
		 
	}

}
