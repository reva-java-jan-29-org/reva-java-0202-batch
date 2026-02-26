package com.training;

import java.util.List;

import com.training.entities.Employee;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

public class Test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		EntityManagerFactory factory = Persistence.createEntityManagerFactory("employeePU");
		
		EntityManager entityManager = factory.createEntityManager();
		
		
		//Insert 
//		Employee employee = new Employee(new Long(102), "Monali", "Pune", new Double(10000.00));
		
//		entityManager.getTransaction().begin();
//		entityManager.persist(employee);
//		entityManager.getTransaction().commit();
		
		
		
		//find the records 
//		entityManager.getTransaction().begin();
//		
//		Employee emp = entityManager.find(Employee.class, new Long(101));
//		System.out.println(emp);
//		
//		emp.setCity("Mumbai");
//		emp.setSalary(12000.00);
//		entityManager.persist(emp);
//		entityManager.getTransaction().commit();
		
//		entityManager.getTransaction().begin();
//		Employee emp = entityManager.find(Employee.class, new Long(101));
//		entityManager.remove(emp);
//		entityManager.getTransaction().commit();
		
		Query query = entityManager.createQuery("SELECT e FROM Employee e");
		List list = query.getResultList();
		
		list
			.forEach(item -> System.out.println(item));
		
		TypedQuery<Employee> empQuery = entityManager.createQuery("SELECT e FROM Employee e WHERE e.city= :city", Employee.class);
		empQuery.setParameter("city", "Mumbai");
		empQuery.getResultList()
			.forEach(emp -> System.out.println(emp));
	}

}
