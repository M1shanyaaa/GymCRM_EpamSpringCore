package com.epam.gym.workload.messaging;

import com.epam.gym.workload.dto.WorkloadRequest;
import com.epam.gym.workload.service.WorkloadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class WorkloadMessageListener {

    private static final Logger log = LoggerFactory.getLogger(WorkloadMessageListener.class);

    private final WorkloadService workloadService;

    public WorkloadMessageListener(WorkloadService workloadService) {
        this.workloadService = workloadService;
    }

    @JmsListener(destination = "${gym.messaging.workload-queue}")
    public void receiveWorkload(WorkloadRequest request) {
        log.info("Received workload message [{}] for trainer '{}'",
                request.getActionType(), request.getTrainerUsername());
        workloadService.processWorkload(request);
        log.info("Processed workload message for trainer '{}'", request.getTrainerUsername());
    }
}