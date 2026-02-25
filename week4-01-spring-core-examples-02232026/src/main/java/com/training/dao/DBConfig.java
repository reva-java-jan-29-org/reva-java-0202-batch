package com.training.dao;

import org.springframework.beans.factory.annotation.Value;

public class DBConfig {
	
	
	private String url;
	private String username;
	private String password;
	
	public DBConfig() {
		super();
	}

	public DBConfig(String url, String username, String password) {
		super();
		this.url = url;
		this.username = username;
		this.password = password;
	}
	
	

}
