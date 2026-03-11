package com.ecommerce.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ecommerce.entity.Customer;
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
	
	public Optional<Customer> findByUsername(String username){
		return customerRepository.findByUsername(username);
	}

}
