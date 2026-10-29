package com.recruitment.jobservice.service;

import com.recruitment.jobservice.entity.Job;
import java.util.List;

public interface JobService {
    Job create(Job job);
    Job getById(Long id);
    List<Job> list(String search, String location, String employmentType, String status);
    List<Job> list();
    List<Job> getByRecruiter(String recruiterId);
    Job update(Long id, Job changes, String currentUsername, String currentRole);
    Job update(Long id, Job changes);
    Job updateStatus(Long id, String status, String currentUsername, String currentRole);
    void delete(Long id, String currentUsername, String currentRole);
}
