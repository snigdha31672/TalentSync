package com.recruitment.applicationservice.service;

import com.recruitment.applicationservice.entity.Application;
import java.util.List;

public interface ApplicationService {
    Application create(Application application);
    Application getById(Long id);
    Application getById(Long id, String currentUsername, String currentRole);
    List<Application> list();
    List<Application> listByCandidateId(String candidateId);
    List<Application> listByJobId(Long jobId);
    Application update(Long id, Application changes);
    Application updateStatus(Long id, String status);
}
