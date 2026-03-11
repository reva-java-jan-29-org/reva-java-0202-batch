package com.ecommerce.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.dto.AuthResponse;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.entity.Customer;
import com.ecommerce.entity.Role;
import com.ecommerce.repository.CustomerRepository;
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
		customer.setRole(request.role() != null ? request.role() : Role.CUSTOMER);
		customer.setFirstName(request.firstName());
		customer.setLastName(request.lastName());
		customer.setMobileNumber(request.mobileNumber());

		customerService.save(customer);

		String token = jwtService.generateToken(customer);

		return ResponseEntity.ok(new AuthResponse(token, customer.getUsername(), customer.getRole().name()));
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {

		authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));

		Customer customer = customerService.findByUsername(request.username())
				.orElseThrow(() -> new RuntimeException("User not found"));

		String token = jwtService.generateToken(customer);

		return ResponseEntity.ok(new AuthResponse(token, customer.getUsername(), customer.getRole().name()));
	}

	@GetMapping
	public ResponseEntity<List<Customer>> findAll() {
		return ResponseEntity.ok(customerService.findAll());
	}

	@GetMapping("/{id}")
	public ResponseEntity<Customer> findById(@PathVariable Long id) {
		return ResponseEntity.ok(customerService.findById(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Customer> update(@PathVariable Long id, @RequestBody Customer customer) {
		customer.setId(id);
		return ResponseEntity.ok(customerService.save(customer));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		customerService.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
