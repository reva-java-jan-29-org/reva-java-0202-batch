package com.ecommerce;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class MyService {

	@Autowired
	private Environment environment;
	
	public void showPort() {
		String port = environment.getProperty("server.port");
		System.out.println("Current Port : " + port);
	}
}
