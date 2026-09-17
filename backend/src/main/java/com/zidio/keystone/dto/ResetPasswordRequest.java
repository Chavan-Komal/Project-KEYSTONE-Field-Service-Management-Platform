package com.zidio.keystone.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Step 2 of the reset flow: the token from the emailed link plus the new
// password. Same 8-char minimum as sign-up (see RegisterRequest).
public record ResetPasswordRequest(
    @NotBlank String token,
    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters.") String newPassword
) {}
