package com.training.listdemos;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class ArrayListDemo03 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

//		List<Integer> listNumbers = new ArrayList<>(); 
//		List<String> listNames =  new ArrayList<>(); 
//		List<Date> listDates =  new ArrayList<>(); 
		
		List<Employee> employeeList = new ArrayList<>();
		
		
		employeeList.add(new Employee(101, "Aryan", 19000.00));
		employeeList.add(new Employee(102, "Ashwin", 34000.00));
		employeeList.add(new Employee(103, "Mayuri", 34000.00));
		employeeList.add(new Employee(104, "Monali", 54000.00));
		employeeList.add(new Employee(105, "Omkar", 43000.00));

		
		
		Employee employee = new Employee(102, "Ashwin", 34000.00);
		
		employeeList.remove(employee);
		
		//System.out.println(employeeList);
		
		Iterator<Employee> iteratror =  employeeList.iterator();
		
		while(iteratror.hasNext()) {
			Employee emp =	iteratror.next();
			System.out.println(emp);
		}
		
		
	}

}
