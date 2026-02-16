package com.training.employeemgmt.services;

import java.util.Comparator;

import com.training.employeemgmt.model.Employee;

public class EmpNameComparator implements Comparator<Employee> {

	@Override
	public int compare(Employee o1, Employee o2) {
		// TODO Auto-generated method stub
		return o1.getFullName().compareTo(o2.getFullName());
	}

}
