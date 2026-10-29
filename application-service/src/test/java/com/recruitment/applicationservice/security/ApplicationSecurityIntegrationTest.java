package com.recruitment.applicationservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitment.applicationservice.client.JobClient;
import com.recruitment.applicationservice.dto.ApplicationRequest;
import com.recruitment.applicationservice.dto.ApplicationStatusRequest;
import com.recruitment.applicationservice.entity.Application;
import com.recruitment.applicationservice.repository.ApplicationRepository;
import com.recruitment.common.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ApplicationSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private ApplicationRepository applicationRepository;

    @MockBean
    private JobClient jobClient;

    private String candidateToken;
    private String recruiterToken;
    private String otherCandidateToken;
    private Application sampleApp;

    @BeforeEach
    void setUp() {
        candidateToken = "Bearer " + jwtUtil.generateToken("candidate1", "CANDIDATE");
        recruiterToken = "Bearer " + jwtUtil.generateToken("recruiter1", "RECRUITER");
        otherCandidateToken = "Bearer " + jwtUtil.generateToken("intruder_candidate", "CANDIDATE");

        sampleApp = new Application();
        sampleApp.setId(50L);
        sampleApp.setJobId(1L);
        sampleApp.setCandidateId("candidate1");
        sampleApp.setCandidateName("Alice");
        sampleApp.setResumeUrl("https://talentsync.local/resumes/alice.pdf");
        sampleApp.setStatus("SUBMITTED");
    }

    // Case 6: Apply for a job with a candidate token — expect HTTP 201
    @Test
    void testCase6_ApplyWithCandidateToken_Returns201() throws Exception {
        when(applicationRepository.existsByCandidateIdAndJobId("candidate1", 1L)).thenReturn(false);
        when(jobClient.getJobById(1L)).thenReturn(Map.of("id", 1, "status", "OPEN", "title", "Cloud Engineer"));
        when(applicationRepository.save(any(Application.class))).thenReturn(sampleApp);

        ApplicationRequest request = new ApplicationRequest(
                1L, "candidate1", "Alice", "alice@example.com",
                "https://talentsync.local/resumes/alice.pdf", null, "I am passionate about cloud architectures"
        );

        mockMvc.perform(post("/api/applications")
                        .header("Authorization", candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.candidateId").value("candidate1"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    // Case 7: Update application status with a recruiter token — expect HTTP 200
    @Test
    void testCase7_UpdateStatusWithRecruiterToken_Returns200() throws Exception {
        when(applicationRepository.findById(50L)).thenReturn(Optional.of(sampleApp));
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));

        ApplicationStatusRequest request = new ApplicationStatusRequest("SHORTLISTED");

        mockMvc.perform(patch("/api/applications/50/status")
                        .header("Authorization", recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHORTLISTED"));
    }

    // Case 11: Attempt to apply using a recruiter token — expect HTTP 403 Forbidden
    @Test
    void testCase11_ApplyWithRecruiterToken_ReturnsForbidden() throws Exception {
        ApplicationRequest request = new ApplicationRequest(
                1L, "recruiter1", "Bob Recruiter", "bob@example.com",
                "https://resume.pdf", null, "Cover letter"
        );

        mockMvc.perform(post("/api/applications")
                        .header("Authorization", recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // Case 12: Attempt to access another candidate's private application — expect HTTP 403 Forbidden
    @Test
    void testCase12_CandidateAccessingOtherCandidateApplication_ReturnsForbidden() throws Exception {
        when(applicationRepository.findById(50L)).thenReturn(Optional.of(sampleApp));

        mockMvc.perform(get("/api/applications/50")
                        .header("Authorization", otherCandidateToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied: You can only view your own applications"));
    }
}
