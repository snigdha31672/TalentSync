package com.recruitment.recruitmentservice.service;

import com.recruitment.recruitmentservice.entity.Recruitment;
import java.util.List;

public interface RecruitmentService {
    Recruitment create(Recruitment recruitment);
    Recruitment getById(Long id);
    Recruitment getByApplicationId(Long applicationId);
    List<Recruitment> list();
    List<Recruitment> listByCandidateId(String candidateId);
    Recruitment update(Long id, Recruitment changes);
    Recruitment updateInterviewStatus(Long id, String interviewStatus);
    Recruitment updateFinalStatus(Long id, String finalStatus);
}
