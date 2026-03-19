package com.ecommerce.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.dto.AuthResponse;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.entity.Customer;
import com.ecommerce.entity.Role;
import com.ecommerce.security.JwtService;
import com.ecommerce.service.CustomerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;
	private final CustomerService customerService;

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {

		if (customerService.existsByUsername(request.username())) {
			throw new RuntimeException("Username already exists: " + request.username());
		}

		Customer customer = new Customer();
		customer.setUsername(request.username());
		customer.setPassword(passwordEncoder.encode(request.password()));
		customer.setRole(Role.CUSTOMER); // Public registration always creates a CUSTOMER
		customer.setFirstName(request.firstName());
		customer.setLastName(request.lastName());
		customer.setMobileNumber(request.mobileNumber());

		customerService.save(customer);

		String token = jwtService.generateToken(customer);

		return ResponseEntity.ok(new AuthResponse(token, customer.getId(), customer.getUsername(), customer.getFirstName(), customer.getRole().name()));
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {

		authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));

		Customer customer = customerService.findByUsername(request.username())
				.orElseThrow(() -> new RuntimeException("User not found"));

		String token = jwtService.generateToken(customer);

		return ResponseEntity.ok(new AuthResponse(token, customer.getId(), customer.getUsername(), customer.getFirstName(), customer.getRole().name()));
	}
}
