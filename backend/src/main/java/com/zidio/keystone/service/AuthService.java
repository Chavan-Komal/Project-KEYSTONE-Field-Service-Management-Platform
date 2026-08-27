package com.zidio.keystone.service;

import com.zidio.keystone.domain.Customer;
import com.zidio.keystone.domain.Role;
import com.zidio.keystone.domain.User;
import com.zidio.keystone.dto.LoginRequest;
import com.zidio.keystone.dto.LoginResponse;
import com.zidio.keystone.dto.RegisterRequest;
import com.zidio.keystone.dto.UserDto;
import com.zidio.keystone.repository.CustomerRepository;
import com.zidio.keystone.repository.UserRepository;
import com.zidio.keystone.security.JwtService;
import com.zidio.keystone.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        UserRepository userRepository,
        CustomerRepository customerRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
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
}
