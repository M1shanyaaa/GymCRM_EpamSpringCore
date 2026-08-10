package com.epam.gym.messaging;

import com.epam.gym.dto.client.WorkloadRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class WorkloadMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(WorkloadMessageProducer.class);

    private final JmsTemplate jmsTemplate;
    private final String workloadQueue;

    public WorkloadMessageProducer(JmsTemplate jmsTemplate,
                                   @Value("${gym.messaging.workload-queue}") String workloadQueue) {
        this.jmsTemplate = jmsTemplate;
        this.workloadQueue = workloadQueue;
    }

    public void sendWorkload(WorkloadRequest request) {
        jmsTemplate.convertAndSend(workloadQueue, request);
        log.info("Sent workload message [{}] for trainer '{}' to queue '{}'",
                request.getActionType(), request.getTrainerUsername(), workloadQueue);
    }
}