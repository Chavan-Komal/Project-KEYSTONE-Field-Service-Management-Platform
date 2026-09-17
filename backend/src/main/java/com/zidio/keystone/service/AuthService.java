package com.zidio.keystone.service;

import com.zidio.keystone.domain.Customer;
import com.zidio.keystone.domain.PasswordResetToken;
import com.zidio.keystone.domain.Role;
import com.zidio.keystone.domain.User;
import com.zidio.keystone.dto.ForgotPasswordRequest;
import com.zidio.keystone.dto.ForgotPasswordResponse;
import com.zidio.keystone.dto.LoginRequest;
import com.zidio.keystone.dto.LoginResponse;
import com.zidio.keystone.dto.RegisterRequest;
import com.zidio.keystone.dto.ResetPasswordRequest;
import com.zidio.keystone.dto.UserDto;
import com.zidio.keystone.repository.CustomerRepository;
import com.zidio.keystone.repository.PasswordResetTokenRepository;
import com.zidio.keystone.repository.UserRepository;
import com.zidio.keystone.security.JwtService;
import com.zidio.keystone.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String GENERIC_RESET_MESSAGE =
        "If an account exists for that email, a password reset link has been sent.";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${keystone.auth.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${keystone.auth.reset-token-ttl-minutes:30}")
    private long resetTokenTtlMinutes;

    @Value("${keystone.auth.expose-reset-token:true}")
    private boolean exposeResetToken;

    public AuthService(
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        UserRepository userRepository,
        CustomerRepository customerRepository,
        PasswordResetTokenRepository resetTokenRepository,
        PasswordEncoder passwordEncoder,
        EmailService emailService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);
        UserDto userDto = new UserDto(principal.getId(), principal.getName(), principal.getEmail(), principal.getRole());

        return new LoginResponse(token, userDto);
    }

    // Self-service sign-up (F9-adjacent): a new organisation registers itself
    // and its first user, landing in the same CUSTOMER role and pipeline as
    // any other customer. Staff roles are never created through this path.
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("An account with that email already exists.");
        }

        Customer customer = Customer.builder()
            .name(request.companyName())
            .contactEmail(request.email())
            .build();
        customer = customerRepository.save(customer);

        User user = User.builder()
            .name(request.name())
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .role(Role.CUSTOMER)
            .customer(customer)
            .build();
        user = userRepository.save(user);

        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtService.generateToken(principal);
        UserDto userDto = new UserDto(user.getId(), user.getName(), user.getEmail(), user.getRole());

        return new LoginResponse(token, userDto);
    }

    // ---------------------------------------------------------------
    // Password reset (self-service)
    // ---------------------------------------------------------------

    /**
     * Step 1 — issue a reset token for the account with this email.
     * The response is identical whether or not the email matched an account,
     * so this endpoint can't be used to discover which emails are registered.
     * The link is delivered by email if a mail server is configured, always
     * logged, and returned in the response when expose-reset-token is on.
     */
    @Transactional
    public ForgotPasswordResponse requestPasswordReset(ForgotPasswordRequest request) {
        Optional<User> maybeUser = userRepository.findByEmail(request.email().trim());
        if (maybeUser.isEmpty()) {
            log.info("[password-reset] requested for unknown email {} — no token issued", request.email());
            return new ForgotPasswordResponse(GENERIC_RESET_MESSAGE, null);
        }

        User user = maybeUser.get();
        String token = generateToken();
        PasswordResetToken entity = PasswordResetToken.builder()
            .user(user)
            .token(token)
            .expiresAt(Instant.now().plus(Duration.ofMinutes(resetTokenTtlMinutes)))
            .used(false)
            .build();
        resetTokenRepository.save(entity);

        String resetUrl = frontendBaseUrl.replaceAll("/+$", "") + "/reset-password?token=" + token;
        emailService.sendPasswordReset(user.getEmail(), resetUrl);

        return new ForgotPasswordResponse(GENERIC_RESET_MESSAGE, exposeResetToken ? resetUrl : null);
    }

    /**
     * Step 2 — consume the token and set the new password. The token is
     * single-use and expires after {@code reset-token-ttl-minutes}.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = resetTokenRepository.findByToken(request.token().trim())
            .orElseThrow(() -> new IllegalArgumentException("This reset link is invalid or has already been used."));

        if (!token.isUsable()) {
            throw new IllegalArgumentException("This reset link has expired or has already been used. Request a new one.");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        token.setUsed(true);
        resetTokenRepository.save(token);

        log.info("[password-reset] password updated for {}", user.getEmail());
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
