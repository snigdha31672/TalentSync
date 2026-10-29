package com.recruitment.jobservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitment.common.security.JwtUtil;
import com.recruitment.jobservice.dto.JobRequest;
import com.recruitment.jobservice.entity.Job;
import com.recruitment.jobservice.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class JobSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private JobRepository jobRepository;

    private String recruiterToken;
    private String candidateToken;
    private Job sampleJob;

    @BeforeEach
    void setUp() {
        recruiterToken = "Bearer " + jwtUtil.generateToken("recruiter1", "RECRUITER");
        candidateToken = "Bearer " + jwtUtil.generateToken("candidate1", "CANDIDATE");

        sampleJob = new Job();
        sampleJob.setId(1L);
        sampleJob.setTitle("Full Stack Engineer");
        sampleJob.setCompanyName("TalentSync Inc");
        sampleJob.setDescription("Develop high-throughput microservices");
        sampleJob.setRequiredSkills("React, Java, Spring Cloud");
        sampleJob.setLocation("Bengaluru");
        sampleJob.setEmploymentType("FULL_TIME");
        sampleJob.setStatus("OPEN");
        sampleJob.setRecruiterId("recruiter1");
    }

    // Case 4: Create a job with a recruiter token — expect HTTP 201
    @Test
    void testCase4_CreateJobWithRecruiterToken_Returns201() throws Exception {
        when(jobRepository.save(any(Job.class))).thenReturn(sampleJob);

        JobRequest request = new JobRequest(
                "Full Stack Engineer", null, "TalentSync Inc",
                "Develop high-throughput microservices", "React, Java, Spring Cloud",
                "Bengaluru", "FULL_TIME", "20-30 LPA", "3+ years", null, "OPEN", "recruiter1"
        );

        mockMvc.perform(post("/api/jobs")
                        .header("Authorization", recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Full Stack Engineer"))
                .andExpect(jsonPath("$.recruiterId").value("recruiter1"));
    }

    // Case 5: View jobs with a candidate token — expect HTTP 200
    @Test
    void testCase5_ViewJobsWithCandidateToken_Returns200() throws Exception {
        when(jobRepository.findAll()).thenReturn(List.of(sampleJob));

        mockMvc.perform(get("/api/jobs")
                        .header("Authorization", candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Full Stack Engineer"));
    }

    // Case 9: Attempt to create a job without a token — expect HTTP 401 or 403
    @Test
    void testCase9_CreateJobWithoutToken_ReturnsUnauthorized() throws Exception {
        JobRequest request = new JobRequest(
                "Unauthorized Job", null, "Ghost Co",
                "Desc", "Skills", "Remote", "CONTRACT", "10 LPA", "1 year", null, "OPEN", "unknown"
        );

        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // Case 10: Attempt to create a job using a candidate token — expect HTTP 403 Forbidden
    @Test
    void testCase10_CreateJobWithCandidateToken_ReturnsForbidden() throws Exception {
        JobRequest request = new JobRequest(
                "Candidate Posting Job", null, "Fake Co",
                "Desc", "Skills", "Remote", "CONTRACT", "10 LPA", "1 year", null, "OPEN", "candidate1"
        );

        mockMvc.perform(post("/api/jobs")
                        .header("Authorization", candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
