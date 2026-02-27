package com.training;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;

import com.training.entities.Employee;
import com.training.entities.Profile;
import com.training.repositories.EmployeeRepository;
import com.training.repositories.ProfileRepository;

import jakarta.transaction.Transactional;

@SpringBootTest
public class EmployeeProfileOneToOneTests {
	
	
	@Autowired
	private EmployeeRepository employeeRepository;
	
	@Autowired
	private ProfileRepository profileRepository;
	
	
//	@Test
//	@Transactional
//	@Commit
//	public void create_shouldPersistEmployeeAndProfile() throws SQLException {
//		
//		Employee employee = new Employee();
//		employee.setName("Mayuri");
//		employee.setCity("Pune");
//		
//		Profile profile = new Profile("Mayuri's BIO");
//		
////		Profile savedProfile = profileRepository.save(profile);
//		
//		employee.setProfile(profile);
//		
//		Employee savedEmp = employeeRepository.save(employee);
//				
//		assertThat(savedEmp.getEmpId()).isNotNull();
//		assertThat(savedEmp.getProfile().getProfileId()).isNotNull();
//	}
	
//	@Test
//	@Transactional
//	public void read_shouldFetchEmployeeAndProfile() {
//		
//		Employee employee = employeeRepository.findById(1L).orElse(null);
//		System.out.println("Employee :" + employee);
//		System.out.println(employee.getProfile());
//			
//		assertThat(employee).isNotNull();
//		
//	}
	
	@Test
	@Transactional
	@Commit
	public void remove_shouldRemoveEmployeeAndProfile() {
		Employee employee = employeeRepository.findById(2L).orElse(null);
			employeeRepository.delete(employee);
	}

}
