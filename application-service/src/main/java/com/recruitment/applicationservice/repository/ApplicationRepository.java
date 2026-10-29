package com.recruitment.applicationservice.repository;

import com.recruitment.applicationservice.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByJobId(Long jobId);
    List<Application> findByCandidateId(String candidateId);
    Optional<Application> findByCandidateIdAndJobId(String candidateId, Long jobId);
    boolean existsByCandidateIdAndJobId(String candidateId, Long jobId);
}
