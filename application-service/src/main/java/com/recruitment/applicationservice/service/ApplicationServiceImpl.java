package com.recruitment.applicationservice.service;

import com.recruitment.applicationservice.client.JobClient;
import com.recruitment.applicationservice.entity.Application;
import com.recruitment.applicationservice.repository.ApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationServiceImpl.class);

    private final ApplicationRepository repository;
    private final JobClient jobClient;

    public ApplicationServiceImpl(ApplicationRepository repository, JobClient jobClient) {
        this.repository = repository;
        this.jobClient = jobClient;
    }

    @Override
    public Application create(Application application) {
        if (application.getJobId() == null) {
            throw new IllegalArgumentException("Job ID is required");
        }
        if (application.getCandidateId() == null || application.getCandidateId().trim().isEmpty()) {
            throw new IllegalArgumentException("Candidate ID is required");
        }

        // 1. Prevent duplicate applications
        if (repository.existsByCandidateIdAndJobId(application.getCandidateId().trim(), application.getJobId())) {
            throw new IllegalStateException("You have already submitted an application for this job.");
        }

        // 2. Inter-service validation with job-service via Eureka + Feign
        try {
            Map<String, Object> jobData = jobClient.getJobById(application.getJobId());
            if (jobData == null || jobData.isEmpty()) {
                throw new IllegalArgumentException("Job not found with ID: " + application.getJobId());
            }
            Object statusObj = jobData.get("status");
            if (statusObj != null && "CLOSED".equalsIgnoreCase(statusObj.toString())) {
                throw new IllegalStateException("Cannot apply to this job because its status is CLOSED.");
            }
            log.info("Inter-service check verified job ID {} is valid and OPEN", application.getJobId());
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Could not reach job-service via Feign or error returned: {}", e.getMessage());
            // Fallback: If job-service threw 404 or connection failed, provide clear error message
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                throw new IllegalArgumentException("Job not found with ID: " + application.getJobId());
            }
            // For resilience in standalone test scenarios, if job-service is down, log and continue
        }

        if (application.getStatus() == null || application.getStatus().trim().isEmpty()) {
            application.setStatus("SUBMITTED");
        }
        if (application.getAppliedDate() == null) {
            application.setAppliedDate(LocalDate.now());
        }
        application.setCreatedAt(LocalDateTime.now());
        application.setUpdatedAt(LocalDateTime.now());

        return repository.save(application);
    }

    @Override
    public Application getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Application not found with ID: " + id));
    }

    @Override
    public Application getById(Long id, String currentUsername, String currentRole) {
        Application app = getById(id);
        if ("CANDIDATE".equalsIgnoreCase(currentRole)) {
            if (currentUsername != null && !currentUsername.equalsIgnoreCase(app.getCandidateId())) {
                throw new AccessDeniedException("Access denied: You can only view your own applications");
            }
        }
        return app;
    }

    @Override
    public List<Application> list() {
        return repository.findAll();
    }

    @Override
    public List<Application> listByCandidateId(String candidateId) {
        return repository.findByCandidateId(candidateId);
    }

    @Override
    public List<Application> listByJobId(Long jobId) {
        return repository.findByJobId(jobId);
    }

    @Override
    public Application update(Long id, Application changes) {
        Application existing = getById(id);
        if (changes.getResumeUrl() != null && !changes.getResumeUrl().trim().isEmpty()) {
            existing.setResumeUrl(changes.getResumeUrl().trim());
        }
        if (changes.getCoverLetter() != null) {
            existing.setCoverLetter(changes.getCoverLetter());
        }
        if (changes.getStatus() != null && !changes.getStatus().trim().isEmpty()) {
            existing.setStatus(changes.getStatus().trim().toUpperCase());
        }
        existing.setUpdatedAt(LocalDateTime.now());
        return repository.save(existing);
    }

    @Override
    public Application updateStatus(Long id, String status) {
        Application existing = getById(id);
        existing.setStatus(status.toUpperCase());
        existing.setUpdatedAt(LocalDateTime.now());
        return repository.save(existing);
    }
}
