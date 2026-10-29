package com.recruitment.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtUtilTest {

    private static final String SECRET = "a-very-strong-secret-key-that-is-at-least-256-bits-long-for-testing-123456";
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, 3600000L); // 1 hour
    }

    @Test
    void testGenerateAndValidateToken() {
        String token = jwtUtil.generateToken("candidate1", "CANDIDATE", "cand@test.com", 101L);
        assertThat(token).isNotBlank();
        assertThat(jwtUtil.isTokenValid(token)).isTrue();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("candidate1");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("CANDIDATE");
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("cand@test.com");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(101L);
    }

    @Test
    void testRecruiterTokenGeneration() {
        String token = jwtUtil.generateToken("recruiter_jane", "RECRUITER");
        assertThat(token).isNotBlank();
        assertThat(jwtUtil.isTokenValid(token)).isTrue();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("recruiter_jane");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("RECRUITER");
    }

    @Test
    void testExpiredTokenValidation() {
        JwtUtil shortLived = new JwtUtil(SECRET, -1000L); // already expired
        String expiredToken = shortLived.generateToken("expiredUser", "CANDIDATE");
        assertThat(jwtUtil.isTokenValid(expiredToken)).isFalse();
    }

    @Test
    void testTamperedTokenValidation() {
        String token = jwtUtil.generateToken("user", "CANDIDATE");
        String tampered = token + "xyz";
        assertThat(jwtUtil.isTokenValid(tampered)).isFalse();
    }
}
