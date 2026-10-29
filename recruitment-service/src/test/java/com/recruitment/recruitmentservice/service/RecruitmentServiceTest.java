package com.recruitment.recruitmentservice.service;

import com.recruitment.recruitmentservice.client.ApplicationClient;
import com.recruitment.recruitmentservice.entity.Recruitment;
import com.recruitment.recruitmentservice.repository.RecruitmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RecruitmentServiceTest {

    @Mock
    private RecruitmentRepository repository;

    @Mock
    private ApplicationClient applicationClient;

    @InjectMocks
    private RecruitmentServiceImpl recruitmentService;

    private Recruitment recruitment;

    @BeforeEach
    void setUp() {
        recruitment = new Recruitment();
        recruitment.setId(5L);
        recruitment.setApplicationId(100L);
        recruitment.setJobId(1L);
        recruitment.setCandidateId("candidate1");
        recruitment.setCandidateName("Alice");
        recruitment.setInterviewDateTime("2026-11-05 10:00 AM IST");
        recruitment.setInterviewMode("ONLINE");
        recruitment.setInterviewLocation("https://meet.google.com/abc-defg-hij");
        recruitment.setInterviewer("Jane Smith, VP Eng");
        recruitment.setInterviewStatus("SCHEDULED");
        recruitment.setFinalStatus("PENDING");
    }

    @Test
    void testCreateRecruitment_Success_And_SyncsApplicationStatus() {
        when(repository.existsByApplicationId(100L)).thenReturn(false);
        when(applicationClient.getApplicationById(100L)).thenReturn(Map.of(
                "id", 100, "candidateId", "candidate1", "candidateName", "Alice", "jobId", 1
        ));
        when(applicationClient.updateApplicationStatus(eq(100L), any())).thenReturn(Map.of("status", "INTERVIEW"));
        when(repository.save(any(Recruitment.class))).thenReturn(recruitment);

        Recruitment created = recruitmentService.create(recruitment);
        assertThat(created).isNotNull();
        assertThat(created.getInterviewStatus()).isEqualTo("SCHEDULED");

        verify(applicationClient, times(1)).updateApplicationStatus(eq(100L), any());
        verify(repository, times(1)).save(recruitment);
    }

    @Test
    void testCreateRecruitment_DuplicateApplication_ThrowsException() {
        when(repository.existsByApplicationId(100L)).thenReturn(true);

        assertThatThrownBy(() -> recruitmentService.create(recruitment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Recruitment record already exists");
        verify(repository, never()).save(any());
    }

    @Test
    void testUpdateFinalStatus_Selected_SyncsToApplicationService() {
        when(repository.findById(5L)).thenReturn(Optional.of(recruitment));
        when(repository.save(any(Recruitment.class))).thenAnswer(i -> i.getArgument(0));

        Recruitment updated = recruitmentService.updateFinalStatus(5L, "SELECTED");
        assertThat(updated.getFinalStatus()).isEqualTo("SELECTED");

        verify(applicationClient, times(1)).updateApplicationStatus(eq(100L), eq(Map.of("status", "SELECTED")));
    }

    @Test
    void testUpdateFinalStatus_Rejected_SyncsToApplicationService() {
        when(repository.findById(5L)).thenReturn(Optional.of(recruitment));
        when(repository.save(any(Recruitment.class))).thenAnswer(i -> i.getArgument(0));

        Recruitment updated = recruitmentService.updateFinalStatus(5L, "REJECTED");
        assertThat(updated.getFinalStatus()).isEqualTo("REJECTED");

        verify(applicationClient, times(1)).updateApplicationStatus(eq(100L), eq(Map.of("status", "REJECTED")));
    }
}
