package com.training;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.training.dao.CustomerDao;
import com.training.dao.CustomerDaoJDBCImpl;
import com.training.model.Customer;
import com.training.model.Employee;
import com.training.service.EmployeeService;

public class TestXmlConfig {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		ApplicationContext context = new ClassPathXmlApplicationContext("spring-beans.xml");
		
//		EmployeeService empService1 = context.getBean(EmployeeService.class);	//specify the type
//		
//		EmployeeService empService2 = context.getBean(EmployeeService.class);
//		
//		empService1.processSalary();
//		
//		context.getBean(Employee.class);
//		
//		context.getBean(Employee.class);
		
	
//		
//		CustomerDao customerDao = context.getBean(CustomerDao.class);	//specify type
//		customerDao.saveCustomer(new Customer());
//		
//		Employee employee1 = context.getBean(Employee.class); //specific type + id of the bean
//		System.out.println(employee1);
//		
//		Employee employee2 =  (Employee) context.getBean("emp2"); //specify type + id of the bean
//		System.out.println(employee2);
		
//		Employee employee3 = (Employee) context.getBean("emp2");
//		System.out.println(employee3);

	}

}
