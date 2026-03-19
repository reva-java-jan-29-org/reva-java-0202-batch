package com.ecommerce.dto;

import java.time.LocalDateTime;

import com.ecommerce.entity.Customer;

public record CustomerResponse(
        Long id,
        String username,
        String firstName,
        String lastName,
        String mobileNumber,
        String role,
        Boolean accountEnabled,
        LocalDateTime createdAt
) {
    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(
                c.getId(),
                c.getUsername(),
                c.getFirstName(),
                c.getLastName(),
                c.getMobileNumber(),
                c.getRole().name(),
                c.getAccountEnabled(),
                c.getCreatedAt()
        );
    }
}
