package com.recruitment.recruitmentservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitment.common.security.JwtUtil;
import com.recruitment.recruitmentservice.client.ApplicationClient;
import com.recruitment.recruitmentservice.dto.RecruitmentRequest;
import com.recruitment.recruitmentservice.entity.Recruitment;
import com.recruitment.recruitmentservice.repository.RecruitmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class RecruitmentSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private RecruitmentRepository recruitmentRepository;

    @MockBean
    private ApplicationClient applicationClient;

    private String recruiterToken;
    private String candidateToken;
    private Recruitment sampleRecruitment;

    @BeforeEach
    void setUp() {
        recruiterToken = "Bearer " + jwtUtil.generateToken("recruiter1", "RECRUITER");
        candidateToken = "Bearer " + jwtUtil.generateToken("candidate1", "CANDIDATE");

        sampleRecruitment = new Recruitment();
        sampleRecruitment.setId(1L);
        sampleRecruitment.setApplicationId(50L);
        sampleRecruitment.setJobId(1L);
        sampleRecruitment.setJobTitle("Cloud Engineer");
        sampleRecruitment.setCandidateId("candidate1");
        sampleRecruitment.setCandidateName("Alice");
        sampleRecruitment.setInterviewDateTime("2026-11-05 10:00 AM IST");
        sampleRecruitment.setInterviewMode("ONLINE");
        sampleRecruitment.setInterviewLocation("https://meet.google.com/xyz");
        sampleRecruitment.setInterviewer("Chief Architect");
        sampleRecruitment.setInterviewStatus("SCHEDULED");
        sampleRecruitment.setFinalStatus("PENDING");
    }

    // Case 8: Schedule an interview as a recruiter — expect HTTP 201
    @Test
    void testCase8_ScheduleInterviewAsRecruiter_Returns201() throws Exception {
        when(recruitmentRepository.existsByApplicationId(50L)).thenReturn(false);
        when(applicationClient.getApplicationById(50L)).thenReturn(Map.of(
                "id", 50, "candidateId", "candidate1", "candidateName", "Alice", "jobId", 1
        ));
        when(applicationClient.updateApplicationStatus(eq(50L), any())).thenReturn(Map.of("status", "INTERVIEW"));
        when(recruitmentRepository.save(any(Recruitment.class))).thenReturn(sampleRecruitment);

        RecruitmentRequest request = new RecruitmentRequest(
                50L, 1L, "Cloud Engineer", "candidate1", "Alice",
                "2026-11-05 10:00 AM IST", "ONLINE", "https://meet.google.com/xyz",
                "Chief Architect", "Strong technical background"
        );

        mockMvc.perform(post("/api/recruitment")
                        .header("Authorization", recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicationId").value(50))
                .andExpect(jsonPath("$.interviewStatus").value("SCHEDULED"));
    }

    // Attempt to schedule an interview with a candidate token — expect HTTP 403 Forbidden
    @Test
    void testScheduleInterviewAsCandidate_ReturnsForbidden() throws Exception {
        RecruitmentRequest request = new RecruitmentRequest(
                50L, 1L, "Cloud Engineer", "candidate1", "Alice",
                "2026-11-05 10:00 AM IST", "ONLINE", "https://meet.google.com/xyz",
                "Chief Architect", "Self scheduled"
        );

        mockMvc.perform(post("/api/recruitment")
                        .header("Authorization", candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
