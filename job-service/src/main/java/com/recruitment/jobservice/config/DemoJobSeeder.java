package com.recruitment.jobservice.config;

import com.recruitment.jobservice.entity.Job;
import com.recruitment.jobservice.repository.JobRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;

/**
 * Adds a small set of clearly labelled demo listings to an empty database,
 * so a fresh TalentSync deployment is usable before recruiters post jobs.
 * Existing job data is never changed or duplicated.
 */
@Configuration
public class DemoJobSeeder {

    @Bean
    CommandLineRunner seedDemoJobs(JobRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }

            LocalDate deadline = LocalDate.now().plusDays(30);
            List<Job> jobs = List.of(
                job("Java Backend Developer", "TalentSync Demo Partner", "Build and maintain REST APIs and backend services for a growing product team.", "Java, Spring Boot, REST APIs, PostgreSQL, Git", "Remote (India)", "FULL_TIME", "₹6–10 LPA", "0–2 years", deadline),
                job("Frontend Developer Intern", "TalentSync Demo Partner", "Help build responsive web interfaces and improve user experience.", "HTML, CSS, JavaScript, React, Git", "Hybrid — Bengaluru", "INTERNSHIP", "₹15,000–25,000/month", "Fresher", deadline),
                job("Cybersecurity Analyst", "TalentSync Demo Partner", "Support security monitoring, vulnerability assessment, and incident documentation.", "Networking, Linux, SIEM, OWASP, Security fundamentals", "Hyderabad", "FULL_TIME", "₹5–8 LPA", "0–2 years", deadline),
                job("Data Analyst Intern", "TalentSync Demo Partner", "Prepare reports and dashboards, clean datasets, and communicate insights.", "Python, SQL, Excel, Power BI, Statistics", "Remote (India)", "INTERNSHIP", "₹12,000–20,000/month", "Fresher", deadline),
                job("QA Automation Engineer", "TalentSync Demo Partner", "Create automated tests and help ensure reliable releases.", "Java, Selenium, API testing, Postman, SQL", "Pune", "FULL_TIME", "₹5–9 LPA", "1–3 years", deadline),
                job("Cloud & DevOps Associate", "TalentSync Demo Partner", "Assist with CI/CD pipelines, container deployments, and cloud monitoring.", "Linux, Docker, GitHub Actions, AWS, CI/CD", "Remote (India)", "FULL_TIME", "₹5–8 LPA", "0–2 years", deadline)
            );

            repository.saveAll(jobs);
        };
    }

    private Job job(String title, String company, String description, String skills,
                    String location, String type, String salary, String experience,
                    LocalDate deadline) {
        Job job = new Job();
        job.setTitle(title);
        job.setCompanyName(company);
        job.setDescription(description);
        job.setRequiredSkills(skills);
        job.setLocation(location);
        job.setEmploymentType(type);
        job.setSalaryRange(salary);
        job.setExperienceRequired(experience);
        job.setApplicationDeadline(deadline);
        job.setStatus("OPEN");
        job.setRecruiterId("demo-recruiter");
        job.setPostedDate(LocalDate.now());
        return job;
    }
}
