package com.recruitment.recruitmentservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recruitment", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"application_id"})
})
public class Recruitment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "candidate_id")
    private String candidateId;

    @Column(name = "candidate_name")
    private String candidateName;

    @Column(name = "interview_date_time")
    private String interviewDateTime;

    @Column(name = "interview_mode")
    private String interviewMode = "ONLINE"; // ONLINE, IN_PERSON, PHONE

    @Column(name = "interview_location", length = 500)
    private String interviewLocation; // e.g., Google Meet URL or Room #

    private String interviewer;

    // SCHEDULED, COMPLETED, CANCELLED
    @Column(name = "interview_status", nullable = false)
    private String interviewStatus = "SCHEDULED";

    @Column(columnDefinition = "TEXT")
    private String feedback;

    // PENDING, SELECTED, REJECTED
    @Column(name = "final_status", nullable = false)
    private String finalStatus = "PENDING";

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Recruitment() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // Alias for recruitmentId
    public Long getRecruitmentId() {
        return id;
    }

    public void setRecruitmentId(Long recruitmentId) {
        this.id = recruitmentId;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getInterviewDateTime() {
        return interviewDateTime;
    }

    public void setInterviewDateTime(String interviewDateTime) {
        this.interviewDateTime = interviewDateTime;
    }

    public String getInterviewMode() {
        return interviewMode;
    }

    public void setInterviewMode(String interviewMode) {
        this.interviewMode = interviewMode;
    }

    public String getInterviewLocation() {
        return interviewLocation;
    }

    public void setInterviewLocation(String interviewLocation) {
        this.interviewLocation = interviewLocation;
    }

    public String getInterviewer() {
        return interviewer;
    }

    public void setInterviewer(String interviewer) {
        this.interviewer = interviewer;
    }

    public String getInterviewStatus() {
        return interviewStatus;
    }

    public void setInterviewStatus(String interviewStatus) {
        this.interviewStatus = interviewStatus;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getFinalStatus() {
        return finalStatus;
    }

    public void setFinalStatus(String finalStatus) {
        this.finalStatus = finalStatus;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
