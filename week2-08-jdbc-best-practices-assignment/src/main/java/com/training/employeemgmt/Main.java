package com.training.employeemgmt;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.training.employeemgmt.dao.EmployeeDao;
import com.training.employeemgmt.dao.JdbcEmployeeDaoImpl;
import com.training.employeemgmt.db.DbConnectionFactory;
import com.training.employeemgmt.model.Employee;
import com.training.employeemgmt.services.EmployeeService;

public class Main {
    public static void main(String[] args) {
        DbConnectionFactory factory = DbConnectionFactory.fromEnv();
        EmployeeDao dao = new JdbcEmployeeDaoImpl(factory);
        EmployeeService service = new EmployeeService(dao);

        // CREATE
//        Employee created = service.createEmployee(Employee.builder()
//                .empCode("EMP-1008")
//                .fullName("Vaishnavi")
//                .email("vaishnavi@example.com")
//                .department("Engineering")
//                .salary(new BigDecimal("94000.00"))
//                .build());
//
//        System.out.println("Created: " + created);

        // READ
//        Employee fetched = service.getEmployee(created.getId());
//        System.out.println("Fetched: " + fetched);
//
//        // UPDATE
//        Employee updated = service.updateEmployee(fetched.toBuilder()
//                .department("Platform Engineering")
//                .salary(new BigDecimal("135000.00"))
//                .build());
//        System.out.println("Updated: " + updated);
//
//        // LIST
//        System.out.println("List:");
//        service.listEmployees(10, 0).forEach(System.out::println);
//
//        // DELETE
//        service.deleteEmployee(updated.getId());
//        System.out.println("Deleted id=" + updated.getId());
        
        
//        List<Employee> list =  service.listEmployees(10, 0);
        
//        Comparator<Employee> comparator = new EmpNameComparator();
        
        
        
        //annonymous object 
//        Comparator<Employee> comparator = new Comparator<Employee>() {
//			
//			@Override
//			public int compare(Employee o1, Employee o2) {
//				
//				return o1.getFullName().compareTo(o2.getFullName());
//			}
//		};
        
//        Comparator<Employee> comparator = (o1, o2) ->  o1.getFullName().compareTo(o2.getFullName());
		
        
        
        
//        Collections.sort(list, comparator);
//        System.out.println(list);
        
//      
        
        
//       list
//        	.stream()
//        	.filter(emp -> emp.getDepartment().equals("Engineering"))
//        	.forEach(emp -> System.out.println(emp));
//        	
        	
        
//        List<Employee> list =  service.listEmployees(10, 0);
        
//        List<String> listEmails = new ArrayList<String>();
//        for(Employee emp : list) {
//        	String email = emp.getEmail();
//        	listEmails.add(email);
//        }
        
//        List<String> listEmails = list
//        	.stream()
//        	.map(emp -> emp.getEmail())
//        	.collect(Collectors.toList());
//        
//        
//        for(String email: listEmails)
//        	System.out.println(email);
        
        
        
        
        //print all the employees who's name starts with 'S'
        
//        List<Employee> list =  service.listEmployees(10, 0);
//        
//        list
//        	.stream()
//        	.filter(emp -> emp.getFullName().startsWith("S"))
//        	.forEach(emp -> System.out.println(emp));
        
        
        //Get and pring all the employee emails
//        

//        list
//        	.stream()
//        	.map(emp -> emp.getEmail())
//        	.forEach(System.out :: println);
        
        
//	      list
//	    	.stream()
//	    	.map(emp -> emp.getEmail())
//	    	.forEach(Main :: myOwnMethod);
        
//        list
//    	.stream()
//    	.map(emp -> emp.getEmail())
//    	.forEach(System.out :: println);
        
        
        
        //convert all names to uppercase and print the list of employees
//        List<Employee> list =  service.listEmployees(10, 0);
//        
//        
//        list.stream()
//        	.map(emp -> emp.getFullName().toUpperCase())
//        	.forEach(System.out :: println);
        
        //Calcualte and print the Total Salary of all the employees 
//        List<Employee> list =  service.listEmployees(10, 0);
//        
//        double sum = list.stream()
//        	.mapToDouble(emp -> emp.getSalary().doubleValue())
//        	.sum();
//        
//        System.out.println("Sum of Salary : " + sum);
        

        
        
        
        
        List<Employee> list =  service.listEmployees(10, 0);
        
        //Calculate Average salary of all employees
        
//        double average = list.stream()
//            	.mapToDouble(emp -> emp.getSalary().doubleValue())
//            	.average()
//            	.orElse(0);
//        
//        System.out.println("average salary :" + average);
        
        //print highest salary 
//        double maxSalary = list.stream()
//            	.mapToDouble(emp -> emp.getSalary().doubleValue())
//            	.max()
//            	.orElse(0);
//        System.out.println("max salary :" + maxSalary);
        
        
        //Sort the employees by salary ascending / descending
        
//        list.stream()
//        	.sorted( (e1, e2) -> e1.getSalary().compareTo(e2.getSalary()) )	
//        	.forEach(System.out :: println);
        
        //descending by salary
//        list.stream()
//    	.sorted( Comparator.comparing(Employee :: getSalary).reversed() )	
//    	.forEach(System.out :: println);
        
        
        
        //Sort by department and then by salary 
//        list.stream()
//        	.sorted(
//        			Comparator.comparing(Employee :: getDepartment)
//        				.thenComparing(Employee:: getSalary)
//        				.reversed()
//        			)
//        	.forEach(System.out :: println);
        
        
        Map<String, List<Employee>> map; 
        	
        
        
        
        
        
        
        
    }
    
    public static void myOwnMethod(String value) {
    	System.out.println(value);
    }
}

