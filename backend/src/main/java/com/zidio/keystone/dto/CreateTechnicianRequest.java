package com.zidio.keystone.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// A manager provisioning a new technician account (Section 03: staff
// accounts are manager-provisioned, never self-registered). baseAddress is
// optional — a technician can be added without one and given a base later.
public record CreateTechnicianRequest(
    @NotBlank String name,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters.") String password,
    String baseAddress
) {}
