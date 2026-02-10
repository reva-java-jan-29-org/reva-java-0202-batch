package com.training.listdemos;

import java.util.HashSet;
import java.util.Set;

public class UserDemo {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		User u1 = new User(1, "Rohit", "rohit@test.com");
		
		User u2 = new User(1, "Mohit", "mohit@test.com");
		
		Set<User> userSet = new HashSet<>();
		
		userSet.add(u1);	//-->hashCode() ---> check equality using equals()
		userSet.add(u2);
		
		System.out.println("u1 hashcode : "+ u1.hashCode());
		System.out.println("u2 hashcode : "+ u2.hashCode());

		
		System.out.println(userSet);

	}

}
