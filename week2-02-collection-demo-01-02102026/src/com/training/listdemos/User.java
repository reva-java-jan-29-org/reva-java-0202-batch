package com.training.listdemos;

import java.util.Objects;

public class User {
	
	private int id;
	private String name;
	private String email;
	
	
	
	
	public User(int id, String name, String email) {
		super();
		this.id = id;
		this.name = name;
		this.email = email;
	}


	@Override
	public String toString() {
		return "\n\n User [id=" + id + ", name=" + name + ", email=" + email + "]";
	}


	@Override
	public int hashCode() {
		return Objects.hash(email, id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		User other = (User) obj;
		return Objects.equals(email, other.email) && id == other.id;
	}

	

}
