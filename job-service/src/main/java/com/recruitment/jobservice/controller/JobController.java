package com.recruitment.jobservice.controller;

import com.recruitment.jobservice.dto.JobRequest;
import com.recruitment.jobservice.dto.JobStatusRequest;
import com.recruitment.jobservice.entity.Job;
import com.recruitment.jobservice.service.JobService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService service;

    public JobController(JobService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> createJob(@RequestBody JobRequest request, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : request.recruiterId();
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Recruiter authentication required"));
        }

        Job job = new Job();
        job.setTitle(request.effectiveTitle());
        job.setCompanyName(request.companyName() != null ? request.companyName().trim() : "TalentSync Partner");
        job.setDescription(request.description() != null ? request.description() : "No description provided.");
        job.setRequiredSkills(request.requiredSkills() != null ? request.requiredSkills() : "Java, Spring Boot, React");
        job.setLocation(request.location() != null ? request.location().trim() : "Remote");
        job.setEmploymentType(request.employmentType() != null ? request.employmentType().trim() : "FULL_TIME");
        job.setSalaryRange(request.salaryRange() != null ? request.salaryRange().trim() : "Competitive");
        job.setExperienceRequired(request.experienceRequired() != null ? request.experienceRequired().trim() : "1-3 years");
        job.setApplicationDeadline(request.applicationDeadline());
        job.setStatus(request.status() != null ? request.status().trim().toUpperCase() : "OPEN");
        job.setRecruiterId(username);

        Job created = service.create(job);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Job>> listJobs(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "location", required = false) String location,
            @RequestParam(name = "employmentType", required = false) String employmentType,
            @RequestParam(name = "status", required = false) String status) {
        return ResponseEntity.ok(service.list(search, location, employmentType, status));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<Job> getJob(@PathVariable(name = "jobId") Long jobId) {
        return ResponseEntity.ok(service.getById(jobId));
    }

    @GetMapping("/recruiter/{recruiterId}")
    public ResponseEntity<List<Job>> getJobsByRecruiter(@PathVariable(name = "recruiterId") String recruiterId) {
        return ResponseEntity.ok(service.getByRecruiter(recruiterId));
    }

    @PutMapping("/{jobId}")
    public ResponseEntity<?> updateJob(
            @PathVariable(name = "jobId") Long jobId,
            @RequestBody JobRequest request,
            Authentication authentication) {
        String currentUsername = authentication != null ? authentication.getName() : null;
        String currentRole = authentication != null && !authentication.getAuthorities().isEmpty()
                ? authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")
                : "RECRUITER";

        Job changes = new Job();
        changes.setTitle(request.effectiveTitle());
        changes.setCompanyName(request.companyName());
        changes.setDescription(request.description());
        changes.setRequiredSkills(request.requiredSkills());
        changes.setLocation(request.location());
        changes.setEmploymentType(request.employmentType());
        changes.setSalaryRange(request.salaryRange());
        changes.setExperienceRequired(request.experienceRequired());
        changes.setApplicationDeadline(request.applicationDeadline());
        changes.setStatus(request.status());

        Job updated = service.update(jobId, changes, currentUsername, currentRole);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{jobId}/status")
    public ResponseEntity<?> updateJobStatus(
            @PathVariable(name = "jobId") Long jobId,
            @RequestBody JobStatusRequest request,
            Authentication authentication) {
        String currentUsername = authentication != null ? authentication.getName() : null;
        String currentRole = authentication != null && !authentication.getAuthorities().isEmpty()
                ? authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")
                : "RECRUITER";

        Job updated = service.updateStatus(jobId, request.status(), currentUsername, currentRole);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<?> deleteJob(
            @PathVariable(name = "jobId") Long jobId,
            Authentication authentication) {
        String currentUsername = authentication != null ? authentication.getName() : null;
        String currentRole = authentication != null && !authentication.getAuthorities().isEmpty()
                ? authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")
                : "RECRUITER";

        service.delete(jobId, currentUsername, currentRole);
        return ResponseEntity.ok(Map.of("message", "Job deleted successfully", "jobId", jobId));
    }
}
