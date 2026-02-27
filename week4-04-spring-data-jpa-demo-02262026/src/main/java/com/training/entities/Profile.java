package com.training.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.Data;

@Entity
@Data
public class Profile {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long profileId;
	
	private String bio;
	
	@OneToOne
	private Employee employee;
	
	public Profile() {
		
	}
	
	public Profile(String bio) {
		this.bio = bio;
	}

	@Override
	public String toString() {
		return "\n\t Profile [profileId=" + profileId + ", bio=" + bio + "]";
	}
	
	

}
