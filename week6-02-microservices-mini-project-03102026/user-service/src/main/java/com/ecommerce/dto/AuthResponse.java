package com.ecommerce.dto;

public record AuthResponse(
	    String token,
	    Long userId,
	    String username,
	    String firstName,
	    String role
	) {}
