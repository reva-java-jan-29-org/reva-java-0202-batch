package com.ecommerce.security;

import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secretKeyString;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    // ── Key ───────────────────────────────────────────────────────────────

    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKeyString);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ── Generate Token ────────────────────────────────────────────────────

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        // Add the role as a custom claim in the token payload
        extraClaims.put("role", userDetails.getAuthorities()
                .iterator().next().getAuthority());
        return buildToken(extraClaims, userDetails);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())         // "sub" claim
                .issuedAt(new Date(System.currentTimeMillis()))  // "iat" claim
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs)) // "exp" claim
                .signWith(getSigningKey())
                .compact();  // serializes to "header.payload.signature"
    }

    // ── Validate Token ────────────────────────────────────────────────────

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ── Extract Claims ────────────────────────────────────────────────────

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())  // verifies signature
                .build()
                .parseSignedClaims(token)     // parses and validates
                .getPayload();                // returns claims map
    }
}
