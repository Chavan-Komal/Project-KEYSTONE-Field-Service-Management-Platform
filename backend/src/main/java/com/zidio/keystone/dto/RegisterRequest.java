package com.zidio.keystone.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Self-service sign-up creates a new Customer organisation plus its first
// CUSTOMER-role user. Staff accounts (dispatcher/technician/manager) stay
// provisioned by a manager — see Section 03 of the brief.
public record RegisterRequest(
    @NotBlank String companyName,
    @NotBlank String name,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters.") String password
) {}
