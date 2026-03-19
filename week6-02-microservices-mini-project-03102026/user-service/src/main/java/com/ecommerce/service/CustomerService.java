package com.ecommerce.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ecommerce.entity.Customer;
import com.ecommerce.entity.Role;
import com.ecommerce.repository.CustomerRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService implements UserDetailsService {

	private final CustomerRepository customerRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		return customerRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
	}

	public List<Customer> findAll() {
		return customerRepository.findAll();
	}

	/** Returns only CUSTOMER-role accounts (used by admin to manage customers). */
	public List<Customer> findAllCustomers() {
		return customerRepository.findByRole(Role.CUSTOMER);
	}

	/** Returns only ADMIN-role accounts. */
	public List<Customer> findAllAdmins() {
		return customerRepository.findByRole(Role.ADMIN);
	}

	public Customer findById(Long id) {
		return customerRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
	}

	public Customer save(Customer customer) {
		return customerRepository.save(customer);
	}

	public void deleteById(Long id) {
		customerRepository.deleteById(id);
	}

	public boolean existsByUsername(String username) {
		return customerRepository.existsByUsername(username);
	}

	public boolean existsByMobileNumber(String mobileNumber) {
		return customerRepository.existsByMobileNumber(mobileNumber);
	}

	public Optional<Customer> findByUsername(String username) {
		return customerRepository.findByUsername(username);
	}

	/** Disables a customer account so they cannot log in. */
	public Customer disableCustomer(Long id) {
		Customer customer = findById(id);
		if (customer.getRole() == Role.ADMIN) {
			throw new RuntimeException("Cannot disable an admin account via this endpoint");
		}
		customer.setAccountEnabled(false);
		return customerRepository.save(customer);
	}

	/** Re-enables a previously disabled customer account. */
	public Customer enableCustomer(Long id) {
		Customer customer = findById(id);
		customer.setAccountEnabled(true);
		return customerRepository.save(customer);
	}
}
