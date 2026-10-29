package com.recruitment.authservice.dto;

public record UserProfileResponse(
    Long id,
    String fullName,
    String username,
    String email,
    String role
) {}
