package com.training.listdemos;

import java.util.Date;
import java.util.Objects;

public class Movie {

	private String name;
	private Date publishedDate;
	
	public Movie(String name, Date publishedDate) {
		super();
		this.name = name;
		this.publishedDate = publishedDate;
	}
	
	@Override
	public int hashCode() {
		return name.length();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Movie other = (Movie) obj;
		return Objects.equals(name, other.name) && Objects.equals(publishedDate, other.publishedDate);
	}
	
	
}
