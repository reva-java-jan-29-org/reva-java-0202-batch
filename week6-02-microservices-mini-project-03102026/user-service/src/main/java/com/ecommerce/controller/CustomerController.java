package com.ecommerce.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.dto.CustomerResponse;
import com.ecommerce.service.CustomerService;

import lombok.RequiredArgsConstructor;

/**
 * Read-only customer endpoints.
 * All mutating operations (disable, delete, create admin) live in AdminController.
 * Every endpoint here requires ADMIN role (enforced via X-User-Role header injected by the gateway).
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

	private final CustomerService customerService;

	@GetMapping
	public ResponseEntity<?> findAll(@RequestHeader("X-User-Role") String role) {
		if (!isAdmin(role)) return forbidden();
		List<CustomerResponse> customers = customerService.findAllCustomers()
				.stream().map(CustomerResponse::from).toList();
		return ResponseEntity.ok(customers);
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> findById(
			@RequestHeader("X-User-Role") String role,
			@PathVariable Long id) {
		if (!isAdmin(role)) return forbidden();
		return ResponseEntity.ok(CustomerResponse.from(customerService.findById(id)));
	}

	// ── helpers ───────────────────────────────────────────────────────────────

	private boolean isAdmin(String role) {
		return "ROLE_ADMIN".equals(role);
	}

	private ResponseEntity<Map<String, String>> forbidden() {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(Map.of("error", "Admin access required"));
	}
}
