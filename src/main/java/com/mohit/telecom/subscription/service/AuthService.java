package com.mohit.telecom.subscription.service;

import com.mohit.telecom.subscription.dto.request.LoginRequest;
import com.mohit.telecom.subscription.dto.request.RegisterRequest;
import com.mohit.telecom.subscription.dto.response.AuthResponse;
import com.mohit.telecom.subscription.entity.Customer;
import com.mohit.telecom.subscription.exception.TelecomException;
import com.mohit.telecom.subscription.repository.CustomerRepository;
import com.mohit.telecom.subscription.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Handles customer registration and authentication.
 */
@Service
public class AuthService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtTokenProvider tokenProvider;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    public AuthResponse register(RegisterRequest request) {
        LOG.info("Registering new customer with email: {}", request.getEmail());

        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new TelecomException(HttpStatus.CONFLICT, "AUTH-001", "Email already registered: " + request.getEmail());
        }

        Customer customer = new Customer();
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        customer.setRole("CUSTOMER");
        customer.setActive(true);

        customerRepository.save(customer);
        LOG.info("Customer registered successfully: {}", request.getEmail());

        String token = tokenProvider.generateToken(customer.getEmail(), customer.getRole());
        return new AuthResponse(token, customer.getEmail(), customer.getRole(), jwtExpirationMs);
    }

    public AuthResponse login(LoginRequest request) {
        Customer customer = customerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new TelecomException(HttpStatus.UNAUTHORIZED, "AUTH-002", "Invalid email or password"));

        if (!customer.getActive()) {
            throw new TelecomException(HttpStatus.FORBIDDEN, "AUTH-004", "Account is deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), customer.getPasswordHash())) {
            throw new TelecomException(HttpStatus.UNAUTHORIZED, "AUTH-002", "Invalid email or password");
        }

        String token = tokenProvider.generateToken(customer.getEmail(), customer.getRole());
        LOG.info("Customer logged in: {}", customer.getEmail());
        return new AuthResponse(token, customer.getEmail(), customer.getRole(), jwtExpirationMs);
    }
}
