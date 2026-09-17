package com.zidio.keystone.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Step 1 of the reset flow: the account email. The response is deliberately
// the same whether or not an account exists (no user enumeration).
public record ForgotPasswordRequest(
    @NotBlank @Email String email
) {}
