package com.recruitment.applicationservice.controller;

import com.recruitment.applicationservice.dto.ApplicationRequest;
import com.recruitment.applicationservice.dto.ApplicationStatusRequest;
import com.recruitment.applicationservice.entity.Application;
import com.recruitment.applicationservice.service.ApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> submitApplication(
            @RequestBody ApplicationRequest request,
            Authentication authentication) {
        try {
            String candidateUsername = (authentication != null && authentication.getName() != null)
                    ? authentication.getName()
                    : request.candidateId();

            if (candidateUsername == null || candidateUsername.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Candidate authentication required"));
            }

            Application application = new Application();
            application.setJobId(request.jobId());
            application.setCandidateId(candidateUsername);
            application.setCandidateName(request.candidateName() != null ? request.candidateName() : candidateUsername);
            application.setCandidateEmail(request.candidateEmail());
            application.setResumeUrl(request.effectiveResumeUrl());
            application.setCoverLetter(request.coverLetter());
            application.setStatus("SUBMITTED");

            Application saved = service.create(application);
            return new ResponseEntity<>(saved, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<?> getApplication(
            @PathVariable(name = "applicationId") Long applicationId,
            Authentication authentication) {
        try {
            String currentUsername = authentication != null ? authentication.getName() : null;
            String currentRole = authentication != null && !authentication.getAuthorities().isEmpty()
                    ? authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")
                    : "RECRUITER";

            Application app = service.getById(applicationId, currentUsername, currentRole);
            return ResponseEntity.ok(app);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Application>> listApplications(
            @RequestParam(name = "candidateId", required = false) String candidateId,
            @RequestParam(name = "jobId", required = false) Long jobId,
            Authentication authentication) {
        String currentUsername = authentication != null ? authentication.getName() : null;
        String currentRole = authentication != null && !authentication.getAuthorities().isEmpty()
                ? authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")
                : "";

        if ("CANDIDATE".equalsIgnoreCase(currentRole) && currentUsername != null) {
            return ResponseEntity.ok(service.listByCandidateId(currentUsername));
        }

        if (candidateId != null && !candidateId.trim().isEmpty()) {
            return ResponseEntity.ok(service.listByCandidateId(candidateId.trim()));
        }
        if (jobId != null) {
            return ResponseEntity.ok(service.listByJobId(jobId));
        }
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/candidate/{candidateId}")
    public ResponseEntity<?> getApplicationsByCandidate(
            @PathVariable(name = "candidateId") String candidateId,
            Authentication authentication) {
        String currentUsername = authentication != null ? authentication.getName() : null;
        String currentRole = authentication != null && !authentication.getAuthorities().isEmpty()
                ? authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")
                : "";

        if ("CANDIDATE".equalsIgnoreCase(currentRole) && currentUsername != null) {
            if (!currentUsername.equalsIgnoreCase(candidateId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied: You can only view your own applications"));
            }
        }
        return ResponseEntity.ok(service.listByCandidateId(candidateId));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<Application>> getApplicationsByJob(@PathVariable(name = "jobId") Long jobId) {
        return ResponseEntity.ok(service.listByJobId(jobId));
    }

    @PatchMapping("/{applicationId}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable(name = "applicationId") Long applicationId,
            @RequestBody ApplicationStatusRequest request) {
        try {
            if (request.status() == null || request.status().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Status is required"));
            }
            Application updated = service.updateStatus(applicationId, request.status().trim().toUpperCase());
            return ResponseEntity.ok(updated);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
