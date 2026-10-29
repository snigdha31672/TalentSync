package com.recruitment.authservice.dto;

public record RegisterRequest(
    String fullName,
    String username,
    String email,
    String password,
    String role
) {}
