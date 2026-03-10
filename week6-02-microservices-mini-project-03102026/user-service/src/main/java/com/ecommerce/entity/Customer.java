package com.ecommerce.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "customers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends User {

	private String firstName;
	
	private String lastName;
	
	@Column(unique = true)
	private String mobileNumber;
	
	@Column(name = "createdAt")
	private LocalDateTime createdAt = LocalDateTime.now();			 
	
	
}
