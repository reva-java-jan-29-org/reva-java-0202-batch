package com.training;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.training.repositories.UserRepository;

@Configuration
public class DebugRunners {
	
	
	@Bean
	CommandLineRunner testUserRepository(UserRepository userRepo) {
		return args -> System.out.println("[DEBUG] : TOTAL USERS IN THE SYSTEM = " + userRepo.count());
	}

}
