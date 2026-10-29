package com.recruitment.recruitmentservice.repository;

import com.recruitment.recruitmentservice.entity.Recruitment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecruitmentRepository extends JpaRepository<Recruitment, Long> {
    Optional<Recruitment> findByApplicationId(Long applicationId);
    boolean existsByApplicationId(Long applicationId);
    List<Recruitment> findByCandidateId(String candidateId);
    List<Recruitment> findByJobId(Long jobId);
}
