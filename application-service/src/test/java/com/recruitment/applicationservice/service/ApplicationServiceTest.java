package com.recruitment.applicationservice.service;

import com.recruitment.applicationservice.client.JobClient;
import com.recruitment.applicationservice.entity.Application;
import com.recruitment.applicationservice.repository.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApplicationServiceTest {

    @Mock
    private ApplicationRepository repository;

    @Mock
    private JobClient jobClient;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    private Application application;

    @BeforeEach
    void setUp() {
        application = new Application();
        application.setId(100L);
        application.setJobId(1L);
        application.setCandidateId("candidate1");
        application.setCandidateName("Alice Candidate");
        application.setResumeUrl("https://talentsync.local/resumes/alice.pdf");
        application.setStatus("SUBMITTED");
    }

    @Test
    void testSubmitApplication_Success() {
        when(repository.existsByCandidateIdAndJobId("candidate1", 1L)).thenReturn(false);
        when(jobClient.getJobById(1L)).thenReturn(Map.of("id", 1, "status", "OPEN", "title", "Software Engineer"));
        when(repository.save(any(Application.class))).thenReturn(application);

        Application created = applicationService.create(application);
        assertThat(created).isNotNull();
        assertThat(created.getStatus()).isEqualTo("SUBMITTED");
        verify(jobClient, times(1)).getJobById(1L);
        verify(repository, times(1)).save(application);
    }

    @Test
    void testSubmitApplication_DuplicateApplication_ThrowsException() {
        when(repository.existsByCandidateIdAndJobId("candidate1", 1L)).thenReturn(true);

        assertThatThrownBy(() -> applicationService.create(application))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already submitted an application");
        verify(repository, never()).save(any());
    }

    @Test
    void testSubmitApplication_ClosedJob_ThrowsException() {
        when(repository.existsByCandidateIdAndJobId("candidate1", 1L)).thenReturn(false);
        when(jobClient.getJobById(1L)).thenReturn(Map.of("id", 1, "status", "CLOSED", "title", "Software Engineer"));

        assertThatThrownBy(() -> applicationService.create(application))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CLOSED");
        verify(repository, never()).save(any());
    }

    @Test
    void testGetById_CandidateAccessingOwn_Success() {
        when(repository.findById(100L)).thenReturn(Optional.of(application));

        Application found = applicationService.getById(100L, "candidate1", "CANDIDATE");
        assertThat(found).isNotNull();
        assertThat(found.getCandidateId()).isEqualTo("candidate1");
    }

    @Test
    void testGetById_CandidateAccessingAnotherCandidate_ThrowsAccessDenied() {
        when(repository.findById(100L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationService.getById(100L, "other_candidate", "CANDIDATE"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("only view your own applications");
    }
}
