package com.recruitment.jobservice.repository;

import com.recruitment.jobservice.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByRecruiterId(String recruiterId);
    List<Job> findByStatus(String status);

    @Query("SELECT j FROM Job j WHERE " +
           "(:status IS NULL OR LOWER(j.status) = LOWER(:status)) AND " +
           "(:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:employmentType IS NULL OR LOWER(j.employmentType) = LOWER(:employmentType)) AND " +
           "(:search IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(j.companyName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(j.requiredSkills) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(j.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Job> searchJobs(
        @Param("search") String search,
        @Param("location") String location,
        @Param("employmentType") String employmentType,
        @Param("status") String status
    );
}
