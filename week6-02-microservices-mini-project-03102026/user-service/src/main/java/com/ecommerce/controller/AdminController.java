package com.ecommerce.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.dto.CreateAdminRequest;
import com.ecommerce.dto.CustomerResponse;
import com.ecommerce.entity.Customer;
import com.ecommerce.entity.Role;
import com.ecommerce.service.CustomerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Admin management endpoints — all require ROLE_ADMIN.
 *
 * POST  /api/admin/admins               — create a new admin account
 * GET   /api/admin/admins               — list all admin accounts
 * DELETE /api/admin/admins/{id}         — delete an admin account
 * GET   /api/admin/customers            — list all customer accounts
 * PUT   /api/admin/customers/{id}/disable  — disable a customer
 * PUT   /api/admin/customers/{id}/enable   — re-enable a customer
 * DELETE /api/admin/customers/{id}      — permanently delete a customer
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

	private final CustomerService customerService;
	private final PasswordEncoder passwordEncoder;

	// ── Admin Management ──────────────────────────────────────────────────────

	@PostMapping("/admins")
	public ResponseEntity<?> createAdmin(
			@RequestHeader("X-User-Role") String role,
			@Valid @RequestBody CreateAdminRequest request) {

		if (!isAdmin(role)) return forbidden();

		if (customerService.existsByUsername(request.username())) {
			return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
		}
		if (customerService.existsByMobileNumber(request.mobileNumber())) {
			return ResponseEntity.badRequest().body(Map.of("error", "Mobile number already in use"));
		}

		Customer admin = new Customer();
		admin.setUsername(request.username());
		admin.setPassword(passwordEncoder.encode(request.password()));
		admin.setRole(Role.ADMIN);
		admin.setFirstName(request.firstName());
		admin.setLastName(request.lastName());
		admin.setMobileNumber(request.mobileNumber());

		Customer saved = customerService.save(admin);
		return ResponseEntity.status(HttpStatus.CREATED).body(CustomerResponse.from(saved));
	}

	@GetMapping("/admins")
	public ResponseEntity<?> listAdmins(@RequestHeader("X-User-Role") String role) {
		if (!isAdmin(role)) return forbidden();
		List<CustomerResponse> admins = customerService.findAllAdmins()
				.stream().map(CustomerResponse::from).toList();
		return ResponseEntity.ok(admins);
	}

	@DeleteMapping("/admins/{id}")
	public ResponseEntity<?> deleteAdmin(
			@RequestHeader("X-User-Role") String role,
			@PathVariable Long id) {
		if (!isAdmin(role)) return forbidden();
		customerService.deleteById(id);
		return ResponseEntity.noContent().build();
	}

	// ── Customer Management ───────────────────────────────────────────────────

	@GetMapping("/customers")
	public ResponseEntity<?> listCustomers(@RequestHeader("X-User-Role") String role) {
		if (!isAdmin(role)) return forbidden();
		List<CustomerResponse> customers = customerService.findAllCustomers()
				.stream().map(CustomerResponse::from).toList();
		return ResponseEntity.ok(customers);
	}

	@PutMapping("/customers/{id}/disable")
	public ResponseEntity<?> disableCustomer(
			@RequestHeader("X-User-Role") String role,
			@PathVariable Long id) {
		if (!isAdmin(role)) return forbidden();
		try {
			return ResponseEntity.ok(CustomerResponse.from(customerService.disableCustomer(id)));
		} catch (RuntimeException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	@PutMapping("/customers/{id}/enable")
	public ResponseEntity<?> enableCustomer(
			@RequestHeader("X-User-Role") String role,
			@PathVariable Long id) {
		if (!isAdmin(role)) return forbidden();
		return ResponseEntity.ok(CustomerResponse.from(customerService.enableCustomer(id)));
	}

	@DeleteMapping("/customers/{id}")
	public ResponseEntity<?> deleteCustomer(
			@RequestHeader("X-User-Role") String role,
			@PathVariable Long id) {
		if (!isAdmin(role)) return forbidden();
		customerService.deleteById(id);
		return ResponseEntity.noContent().build();
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
