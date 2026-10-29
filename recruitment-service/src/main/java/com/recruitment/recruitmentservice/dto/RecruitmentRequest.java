package com.recruitment.recruitmentservice.dto;

public record RecruitmentRequest(
    Long applicationId,
    Long jobId,
    String jobTitle,
    String candidateId,
    String candidateName,
    String interviewDateTime,
    String interviewMode,
    String interviewLocation,
    String interviewer,
    String feedback
) {
}
