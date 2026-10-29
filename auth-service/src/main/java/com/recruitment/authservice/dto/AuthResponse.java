package com.recruitment.authservice.dto;

public record AuthResponse(
    String token,
    Long id,
    String fullName,
    String username,
    String email,
    String role
) {}
