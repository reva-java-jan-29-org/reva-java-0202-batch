package com.training;

import java.util.List;
import com.training.repositories.ProfileRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import com.training.entities.Employee;
import com.training.entities.EmployeeType;
import com.training.entities.Profile;
import com.training.repositories.EmployeeRepository;

@SpringBootApplication
public class Week404SpringDataJpaDemo02262026Application {

    private final ProfileRepository profileRepository;

    Week404SpringDataJpaDemo02262026Application(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

	public static void main(String[] args) {
	
	  ApplicationContext context =	SpringApplication.run(Week404SpringDataJpaDemo02262026Application.class, args);
	
	  EmployeeRepository employeeRepository = context.getBean(EmployeeRepository.class);
	  
//	  ProfileRepository profileRepository = context.getBean(ProfileRepository.class);
//	  
//	  	
//		
//
//		Profile profile = new Profile("Vaibhav's BIO");
//		Profile savedProfile = profileRepository.save(profile);
//		
//		Employee employee = new Employee();
//		employee.setName("Vaibhav");
//		employee.setCity("Pune");
//		employee.setProfile(savedProfile);
//		
//		Employee savedEmp = employeeRepository.save(employee);
//	  
//	  
//	  
//	  
	
//	  Employee emp = new Employee("Siddhant", "Pune", 12000.00, EmployeeType.SENIOR);
	  
//	  empRep.save(emp);
	  
//	  Iterable<Employee> iterable =  empRep.findAll();
//	  
//	  iterable
//	    .forEach(emp -> System.out.println(emp));
	  
//	  Employee emp = empRep.findById(101L).orElse(null);
//	  System.out.println(emp);
//	  
//	  emp.setCity("Mumbai");
//	  emp.setSalary(13000.00);
//	  empRep.save(emp);
	  
//	  List<Employee> list = empRep.getAllEmployeesByCity("Mumbai");
//	  list.forEach(System.out::println);
	  
//	  List<Employee> list = empRep.findAllByCity("Mumbai");
//	  list.forEach(System.out::println);
	  
	  
	}

}
