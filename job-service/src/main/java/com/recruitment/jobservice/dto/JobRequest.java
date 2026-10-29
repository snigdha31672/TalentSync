package com.recruitment.jobservice.dto;

import java.time.LocalDate;

public record JobRequest(
    String title,
    String role,
    String companyName,
    String description,
    String requiredSkills,
    String location,
    String employmentType,
    String salaryRange,
    String experienceRequired,
    LocalDate applicationDeadline,
    String status,
    String recruiterId
) {
    public String effectiveTitle() {
        if (title != null && !title.trim().isEmpty()) {
            return title.trim();
        }
        if (role != null && !role.trim().isEmpty()) {
            return role.trim();
        }
        return "Software Engineer";
    }
}
