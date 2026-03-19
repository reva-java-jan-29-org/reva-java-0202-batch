package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAdminRequest(

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50)
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 6)
        String password,

        String firstName,
        String lastName,

        @NotBlank(message = "Mobile number is required")
        String mobileNumber
) {}
