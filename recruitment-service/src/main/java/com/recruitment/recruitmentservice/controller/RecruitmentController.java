package com.recruitment.recruitmentservice.controller;

import com.recruitment.recruitmentservice.dto.FinalStatusRequest;
import com.recruitment.recruitmentservice.dto.InterviewStatusRequest;
import com.recruitment.recruitmentservice.dto.RecruitmentRequest;
import com.recruitment.recruitmentservice.entity.Recruitment;
import com.recruitment.recruitmentservice.service.RecruitmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/recruitment")
public class RecruitmentController {

    private final RecruitmentService service;

    public RecruitmentController(RecruitmentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> createRecruitmentRecord(@RequestBody RecruitmentRequest request) {
        try {
            if (request.applicationId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "applicationId is required"));
            }

            Recruitment record = new Recruitment();
            record.setApplicationId(request.applicationId());
            record.setJobId(request.jobId());
            record.setJobTitle(request.jobTitle());
            record.setCandidateId(request.candidateId());
            record.setCandidateName(request.candidateName());
            record.setInterviewDateTime(request.interviewDateTime() != null ? request.interviewDateTime() : "TBD");
            record.setInterviewMode(request.interviewMode() != null ? request.interviewMode() : "ONLINE");
            record.setInterviewLocation(request.interviewLocation() != null ? request.interviewLocation() : "Google Meet");
            record.setInterviewer(request.interviewer() != null ? request.interviewer() : "Hiring Committee");
            record.setFeedback(request.feedback());
            record.setInterviewStatus("SCHEDULED");
            record.setFinalStatus("PENDING");

            Recruitment saved = service.create(record);
            return new ResponseEntity<>(saved, HttpStatus.CREATED);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Recruitment>> listRecruitmentRecords(
            @RequestParam(name = "candidateId", required = false) String candidateId) {
        if (candidateId != null && !candidateId.trim().isEmpty()) {
            return ResponseEntity.ok(service.listByCandidateId(candidateId.trim()));
        }
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/{recruitmentId}")
    public ResponseEntity<?> getRecruitmentRecord(@PathVariable(name = "recruitmentId") Long recruitmentId) {
        try {
            return ResponseEntity.ok(service.getById(recruitmentId));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/application/{applicationId}")
    public ResponseEntity<?> getByApplicationId(@PathVariable(name = "applicationId") Long applicationId) {
        try {
            return ResponseEntity.ok(service.getByApplicationId(applicationId));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{recruitmentId}")
    public ResponseEntity<?> updateRecruitmentRecord(
            @PathVariable(name = "recruitmentId") Long recruitmentId,
            @RequestBody RecruitmentRequest request) {
        try {
            Recruitment changes = new Recruitment();
            changes.setInterviewDateTime(request.interviewDateTime());
            changes.setInterviewMode(request.interviewMode());
            changes.setInterviewLocation(request.interviewLocation());
            changes.setInterviewer(request.interviewer());
            changes.setFeedback(request.feedback());

            Recruitment updated = service.update(recruitmentId, changes);
            return ResponseEntity.ok(updated);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{recruitmentId}/interview-status")
    public ResponseEntity<?> updateInterviewStatus(
            @PathVariable(name = "recruitmentId") Long recruitmentId,
            @RequestBody InterviewStatusRequest request) {
        try {
            if (request.interviewStatus() == null || request.interviewStatus().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "interviewStatus is required"));
            }
            Recruitment updated = service.updateInterviewStatus(recruitmentId, request.interviewStatus().trim().toUpperCase());
            return ResponseEntity.ok(updated);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{recruitmentId}/final-status")
    public ResponseEntity<?> updateFinalStatus(
            @PathVariable(name = "recruitmentId") Long recruitmentId,
            @RequestBody FinalStatusRequest request) {
        try {
            if (request.finalStatus() == null || request.finalStatus().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "finalStatus is required"));
            }
            Recruitment updated = service.updateFinalStatus(recruitmentId, request.finalStatus().trim().toUpperCase());
            return ResponseEntity.ok(updated);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
