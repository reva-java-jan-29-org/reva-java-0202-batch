package com.ecommerce.controller;
import com.ecommerce.config.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.MyService;

@RestController
public class DemoController {
	
	@Autowired
	private MyService myService;

 

	@GetMapping("/config")
	public void readConfig() {
		
		myService.showPort();
	}
}
