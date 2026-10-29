package com.recruitment.applicationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "job-service", configuration = FeignClientConfig.class)
public interface JobClient {

    @GetMapping("/api/jobs/{jobId}")
    Map<String, Object> getJobById(@PathVariable("jobId") Long jobId);
}
