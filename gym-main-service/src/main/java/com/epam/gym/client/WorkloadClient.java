package com.epam.gym.client;

import com.epam.gym.dto.client.WorkloadRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "trainer-workload-service")
public interface WorkloadClient {

    @PostMapping("/api/workload")
    void updateWorkload(@RequestBody WorkloadRequest request);
}