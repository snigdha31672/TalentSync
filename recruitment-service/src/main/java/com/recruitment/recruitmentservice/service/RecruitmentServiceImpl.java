package com.recruitment.recruitmentservice.service;

import com.recruitment.recruitmentservice.client.ApplicationClient;
import com.recruitment.recruitmentservice.entity.Recruitment;
import com.recruitment.recruitmentservice.repository.RecruitmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class RecruitmentServiceImpl implements RecruitmentService {

    private static final Logger log = LoggerFactory.getLogger(RecruitmentServiceImpl.class);

    private final RecruitmentRepository repository;
    private final ApplicationClient applicationClient;

    public RecruitmentServiceImpl(RecruitmentRepository repository, ApplicationClient applicationClient) {
        this.repository = repository;
        this.applicationClient = applicationClient;
    }

    @Override
    public Recruitment create(Recruitment recruitment) {
        if (recruitment.getApplicationId() == null) {
            throw new IllegalArgumentException("Application ID is required");
        }

        // Prevent duplicate recruitment records for the same application
        if (repository.existsByApplicationId(recruitment.getApplicationId())) {
            throw new IllegalStateException("Recruitment record already exists for application ID: " + recruitment.getApplicationId());
        }

        // Inter-service verification with application-service via Feign
        try {
            Map<String, Object> appData = applicationClient.getApplicationById(recruitment.getApplicationId());
            if (appData != null) {
                if (recruitment.getCandidateId() == null && appData.get("candidateId") != null) {
                    recruitment.setCandidateId(appData.get("candidateId").toString());
                }
                if (recruitment.getCandidateName() == null && appData.get("candidateName") != null) {
                    recruitment.setCandidateName(appData.get("candidateName").toString());
                }
                if (recruitment.getJobId() == null && appData.get("jobId") != null) {
                    recruitment.setJobId(Long.valueOf(appData.get("jobId").toString()));
                }
            }

            // Keep status synchronized: update application status to INTERVIEW
            applicationClient.updateApplicationStatus(recruitment.getApplicationId(), Map.of("status", "INTERVIEW"));
            log.info("Inter-service call updated application ID {} status to INTERVIEW", recruitment.getApplicationId());
        } catch (Exception e) {
            log.warn("Feign call to application-service failed: {}", e.getMessage());
        }

        if (recruitment.getInterviewStatus() == null || recruitment.getInterviewStatus().trim().isEmpty()) {
            recruitment.setInterviewStatus("SCHEDULED");
        }
        if (recruitment.getFinalStatus() == null || recruitment.getFinalStatus().trim().isEmpty()) {
            recruitment.setFinalStatus("PENDING");
        }
        recruitment.setScheduledAt(LocalDateTime.now());
        recruitment.setUpdatedAt(LocalDateTime.now());

        return repository.save(recruitment);
    }

    @Override
    public Recruitment getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Recruitment record not found with ID: " + id));
    }

    @Override
    public Recruitment getByApplicationId(Long applicationId) {
        return repository.findByApplicationId(applicationId)
                .orElseThrow(() -> new NoSuchElementException("Recruitment record not found for application ID: " + applicationId));
    }

    @Override
    public List<Recruitment> list() {
        return repository.findAll();
    }

    @Override
    public List<Recruitment> listByCandidateId(String candidateId) {
        return repository.findByCandidateId(candidateId);
    }

    @Override
    public Recruitment update(Long id, Recruitment changes) {
        Recruitment existing = getById(id);
        if (changes.getInterviewDateTime() != null) {
            existing.setInterviewDateTime(changes.getInterviewDateTime());
        }
        if (changes.getInterviewMode() != null) {
            existing.setInterviewMode(changes.getInterviewMode());
        }
        if (changes.getInterviewLocation() != null) {
            existing.setInterviewLocation(changes.getInterviewLocation());
        }
        if (changes.getInterviewer() != null) {
            existing.setInterviewer(changes.getInterviewer());
        }
        if (changes.getInterviewStatus() != null) {
            existing.setInterviewStatus(changes.getInterviewStatus().toUpperCase());
        }
        if (changes.getFeedback() != null) {
            existing.setFeedback(changes.getFeedback());
        }
        if (changes.getFinalStatus() != null) {
            String newFinalStatus = changes.getFinalStatus().toUpperCase();
            existing.setFinalStatus(newFinalStatus);
            syncApplicationFinalStatus(existing.getApplicationId(), newFinalStatus);
        }
        existing.setUpdatedAt(LocalDateTime.now());
        return repository.save(existing);
    }

    @Override
    public Recruitment updateInterviewStatus(Long id, String interviewStatus) {
        Recruitment existing = getById(id);
        existing.setInterviewStatus(interviewStatus.toUpperCase());
        existing.setUpdatedAt(LocalDateTime.now());
        return repository.save(existing);
    }

    @Override
    public Recruitment updateFinalStatus(Long id, String finalStatus) {
        Recruitment existing = getById(id);
        String upper = finalStatus.toUpperCase();
        existing.setFinalStatus(upper);
        existing.setUpdatedAt(LocalDateTime.now());
        Recruitment saved = repository.save(existing);

        syncApplicationFinalStatus(existing.getApplicationId(), upper);
        return saved;
    }

    private void syncApplicationFinalStatus(Long applicationId, String finalStatus) {
        if ("SELECTED".equals(finalStatus) || "REJECTED".equals(finalStatus)) {
            try {
                applicationClient.updateApplicationStatus(applicationId, Map.of("status", finalStatus));
                log.info("Synchronized application ID {} status to {}", applicationId, finalStatus);
            } catch (Exception e) {
                log.warn("Could not sync final status to application-service: {}", e.getMessage());
            }
        }
    }
}
