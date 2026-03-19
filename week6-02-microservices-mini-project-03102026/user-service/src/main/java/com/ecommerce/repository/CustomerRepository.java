package com.ecommerce.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import com.ecommerce.entity.Customer;
import com.ecommerce.entity.Role;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

	Optional<Customer> findByUsername(String username);

	boolean existsByUsername(String username);

	List<Customer> findByRole(Role role);

	boolean existsByMobileNumber(String mobileNumber);
}
