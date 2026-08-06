package com.epam.gym.workload.controller;

import com.epam.gym.workload.dto.response.TrainerWorkloadResponse;
import com.epam.gym.workload.dto.WorkloadRequest;
import com.epam.gym.workload.service.WorkloadService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workload")
public class WorkloadController {

    private static final Logger log = LoggerFactory.getLogger(WorkloadController.class);
    private final WorkloadService workloadService;

    public WorkloadController(WorkloadService workloadService) {
        this.workloadService = workloadService;
    }

    @PostMapping
    public ResponseEntity<Void> updateWorkload(@Valid @RequestBody WorkloadRequest request) {
        log.info("Received request to {} workload for trainer: {}",
                request.getActionType(), request.getTrainerUsername());

        workloadService.processWorkload(request);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{username}/summary")
    public ResponseEntity<TrainerWorkloadResponse> getWorkloadSummary(@PathVariable String username) {
        log.info("Received request to get workload summary for trainer: {}", username);

        try {
            TrainerWorkloadResponse summary = workloadService.getSummary(username);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            log.warn(e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}