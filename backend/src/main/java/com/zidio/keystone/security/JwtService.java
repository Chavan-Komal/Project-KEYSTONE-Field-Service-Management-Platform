package com.zidio.keystone.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * Stateless JWT issuing/parsing — Section 08 of the brief.
 * The server holds no session; every request must present a valid, unexpired
 * token, and the signature is checked on every parse.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(
        @Value("${keystone.jwt.secret}") String secret,
        @Value("${keystone.jwt.expiration-minutes}") long expirationMinutes
    ) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMinutes * 60_000);

        // Declared explicitly as JwtBuilder (not `var`) so the chained
        // reassignment below never risks being inferred as plain Object.
        JwtBuilder builder = Jwts.builder()
            .subject(principal.getEmail())
            .claim("userId", principal.getId().toString())
            .claim("role", principal.getRole().name())
            .issuedAt(now)
            .expiration(expiry);

        // Only present for CUSTOMER-role users — avoid adding a null claim.
        if (principal.getCustomerId() != null) {
            builder = builder.claim("customerId", principal.getCustomerId().toString());
        }

        return builder.signWith(key).compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isValid(String token, String expectedEmail) {
        try {
            Claims claims = parseClaims(token);
            return claims.getSubject().equals(expectedEmail) && claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
