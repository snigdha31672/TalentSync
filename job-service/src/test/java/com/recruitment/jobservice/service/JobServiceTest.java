package com.recruitment.jobservice.service;

import com.recruitment.jobservice.entity.Job;
import com.recruitment.jobservice.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JobServiceTest {

    @Mock
    private JobRepository repository;

    @InjectMocks
    private JobServiceImpl jobService;

    private Job job;

    @BeforeEach
    void setUp() {
        job = new Job();
        job.setId(10L);
        job.setTitle("Senior Java Developer");
        job.setCompanyName("Google Inc.");
        job.setDescription("Build scalable cloud microservices");
        job.setRequiredSkills("Java, Spring Cloud, PostgreSQL");
        job.setLocation("Bengaluru");
        job.setEmploymentType("FULL_TIME");
        job.setStatus("OPEN");
        job.setRecruiterId("recruiter_bob");
    }

    @Test
    void testCreateJob_Success() {
        when(repository.save(any(Job.class))).thenReturn(job);

        Job created = jobService.create(job);
        assertThat(created).isNotNull();
        assertThat(created.getTitle()).isEqualTo("Senior Java Developer");
        assertThat(created.getStatus()).isEqualTo("OPEN");
        verify(repository, times(1)).save(job);
    }

    @Test
    void testGetJobById_Found() {
        when(repository.findById(10L)).thenReturn(Optional.of(job));

        Job found = jobService.getById(10L);
        assertThat(found).isNotNull();
        assertThat(found.getJobId()).isEqualTo(10L);
    }

    @Test
    void testGetJobById_NotFound_ThrowsException() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getById(999L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void testListJobsWithFilters() {
        when(repository.searchJobs("Java", "Bengaluru", "FULL_TIME", "OPEN"))
                .thenReturn(List.of(job));

        List<Job> results = jobService.list("Java", "Bengaluru", "FULL_TIME", "OPEN");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).contains("Java");
    }

    @Test
    void testUpdateJob_OwnerRecruiter_Success() {
        when(repository.findById(10L)).thenReturn(Optional.of(job));
        when(repository.save(any(Job.class))).thenReturn(job);

        Job changes = new Job();
        changes.setTitle("Lead Java Architect");
        changes.setLocation("Hyderabad");

        Job updated = jobService.update(10L, changes, "recruiter_bob", "RECRUITER");
        assertThat(updated).isNotNull();
        verify(repository).save(job);
    }

    @Test
    void testUpdateJob_DifferentRecruiter_ThrowsAccessDeniedException() {
        when(repository.findById(10L)).thenReturn(Optional.of(job));

        Job changes = new Job();
        changes.setTitle("Hacked Title");

        assertThatThrownBy(() -> jobService.update(10L, changes, "recruiter_evil", "RECRUITER"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not authorized");
    }

    @Test
    void testUpdateStatus_Success() {
        when(repository.findById(10L)).thenReturn(Optional.of(job));
        when(repository.save(any(Job.class))).thenReturn(job);

        Job updated = jobService.updateStatus(10L, "CLOSED", "recruiter_bob", "RECRUITER");
        assertThat(updated.getStatus()).isEqualTo("CLOSED");
    }

    @Test
    void testDeleteJob_Owner_Success() {
        when(repository.findById(10L)).thenReturn(Optional.of(job));
        doNothing().when(repository).delete(job);

        jobService.delete(10L, "recruiter_bob", "RECRUITER");
        verify(repository, times(1)).delete(job);
    }
}
