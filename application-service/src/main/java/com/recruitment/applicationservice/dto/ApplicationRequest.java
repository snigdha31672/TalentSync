package com.recruitment.applicationservice.dto;

public record ApplicationRequest(
    Long jobId,
    String candidateId,
    String candidateName,
    String candidateEmail,
    String resumeUrl,
    String resumeLink,
    String coverLetter
) {
    public String effectiveResumeUrl() {
        if (resumeUrl != null && !resumeUrl.trim().isEmpty()) {
            return resumeUrl.trim();
        }
        if (resumeLink != null && !resumeLink.trim().isEmpty()) {
            return resumeLink.trim();
        }
        return "https://talentsync.local/resumes/default.pdf";
    }
}
