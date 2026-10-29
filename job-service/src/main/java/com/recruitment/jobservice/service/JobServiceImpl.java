package com.recruitment.jobservice.service;

import com.recruitment.jobservice.entity.Job;
import com.recruitment.jobservice.repository.JobRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class JobServiceImpl implements JobService {

    private final JobRepository repository;

    public JobServiceImpl(JobRepository repository) {
        this.repository = repository;
    }

    @Override
    public Job create(Job job) {
        if (job.getStatus() == null || job.getStatus().trim().isEmpty()) {
            job.setStatus("OPEN");
        }
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        return repository.save(job);
    }

    @Override
    public Job getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Job not found with id: " + id));
    }

    @Override
    public List<Job> list(String search, String location, String employmentType, String status) {
        String s = (search == null || search.trim().isEmpty()) ? null : search.trim();
        String l = (location == null || location.trim().isEmpty()) ? null : location.trim();
        String e = (employmentType == null || employmentType.trim().isEmpty()) ? null : employmentType.trim();
        String st = (status == null || status.trim().isEmpty()) ? null : status.trim();

        if (s == null && l == null && e == null && st == null) {
            return repository.findAll();
        }
        return repository.searchJobs(s, l, e, st);
    }

    @Override
    public List<Job> list() {
        return repository.findAll();
    }

    @Override
    public List<Job> getByRecruiter(String recruiterId) {
        return repository.findByRecruiterId(recruiterId);
    }

    @Override
    public Job update(Long id, Job changes, String currentUsername, String currentRole) {
        Job existing = getById(id);
        validateOwnership(existing, currentUsername, currentRole);

        if (changes.getTitle() != null && !changes.getTitle().trim().isEmpty()) {
            existing.setTitle(changes.getTitle().trim());
        }
        if (changes.getCompanyName() != null && !changes.getCompanyName().trim().isEmpty()) {
            existing.setCompanyName(changes.getCompanyName().trim());
        }
        if (changes.getDescription() != null && !changes.getDescription().trim().isEmpty()) {
            existing.setDescription(changes.getDescription().trim());
        }
        if (changes.getRequiredSkills() != null && !changes.getRequiredSkills().trim().isEmpty()) {
            existing.setRequiredSkills(changes.getRequiredSkills().trim());
        }
        if (changes.getLocation() != null && !changes.getLocation().trim().isEmpty()) {
            existing.setLocation(changes.getLocation().trim());
        }
        if (changes.getEmploymentType() != null && !changes.getEmploymentType().trim().isEmpty()) {
            existing.setEmploymentType(changes.getEmploymentType().trim());
        }
        if (changes.getSalaryRange() != null) {
            existing.setSalaryRange(changes.getSalaryRange().trim());
        }
        if (changes.getExperienceRequired() != null) {
            existing.setExperienceRequired(changes.getExperienceRequired().trim());
        }
        if (changes.getApplicationDeadline() != null) {
            existing.setApplicationDeadline(changes.getApplicationDeadline());
        }
        if (changes.getStatus() != null && !changes.getStatus().trim().isEmpty()) {
            existing.setStatus(changes.getStatus().trim().toUpperCase());
        }
        existing.setUpdatedAt(LocalDateTime.now());
        return repository.save(existing);
    }

    @Override
    public Job update(Long id, Job changes) {
        return update(id, changes, null, null);
    }

    @Override
    public Job updateStatus(Long id, String status, String currentUsername, String currentRole) {
        Job existing = getById(id);
        validateOwnership(existing, currentUsername, currentRole);
        existing.setStatus(status.toUpperCase());
        existing.setUpdatedAt(LocalDateTime.now());
        return repository.save(existing);
    }

    @Override
    public void delete(Long id, String currentUsername, String currentRole) {
        Job existing = getById(id);
        validateOwnership(existing, currentUsername, currentRole);
        repository.delete(existing);
    }

    private void validateOwnership(Job job, String currentUsername, String currentRole) {
        if (currentUsername == null || currentUsername.isEmpty()) {
            return; // internal or unauthenticated context
        }
        if ("RECRUITER".equalsIgnoreCase(currentRole)) {
            if (!currentUsername.equalsIgnoreCase(job.getRecruiterId())) {
                throw new AccessDeniedException("You are not authorized to modify job postings owned by another recruiter");
            }
        }
    }
}
