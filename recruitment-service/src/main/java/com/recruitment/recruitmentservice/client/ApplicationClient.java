package com.recruitment.recruitmentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "application-service", configuration = FeignClientConfig.class)
public interface ApplicationClient {

    @GetMapping("/api/applications/{applicationId}")
    Map<String, Object> getApplicationById(@PathVariable("applicationId") Long applicationId);

    @PatchMapping("/api/applications/{applicationId}/status")
    Map<String, Object> updateApplicationStatus(
        @PathVariable("applicationId") Long applicationId,
        @RequestBody Map<String, String> statusRequest
    );
}
