package com.recruitment.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared JWT utility without servlet dependencies.
 * Compatible with reactive API Gateway (Spring Cloud Gateway / WebFlux)
 * and servlet-based business services (Spring MVC).
 */
public class JwtUtil {

    private final Key signingKey;
    private final long expirationMillis;

    public JwtUtil(String secret, long expirationMillis) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMillis = expirationMillis;
    }

    public String generateToken(String username, String role) {
        return generateToken(username, role, null, null);
    }

    public String generateToken(String username, String role, String email, Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        if (email != null) {
            claims.put("email", email);
        }
        if (userId != null) {
            claims.put("userId", userId);
        }

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    public Long extractUserId(String token) {
        Object val = extractAllClaims(token).get("userId");
        if (val instanceof Number num) {
            return num.longValue();
        }
        return null;
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Date expiration = claims.getExpiration();
            return expiration != null && !expiration.before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
