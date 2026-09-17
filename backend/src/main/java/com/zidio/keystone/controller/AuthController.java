package com.zidio.keystone.controller;

import com.zidio.keystone.dto.ForgotPasswordRequest;
import com.zidio.keystone.dto.ForgotPasswordResponse;
import com.zidio.keystone.dto.LoginRequest;
import com.zidio.keystone.dto.LoginResponse;
import com.zidio.keystone.dto.RegisterRequest;
import com.zidio.keystone.dto.ResetPasswordRequest;
import com.zidio.keystone.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    // Step 1 of the self-service reset flow. Always 200 with the same body,
    // whether or not the email matched an account.
    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.requestPasswordReset(request);
    }

    // Step 2 — consume the token from the emailed link and set a new password.
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
    }
}
