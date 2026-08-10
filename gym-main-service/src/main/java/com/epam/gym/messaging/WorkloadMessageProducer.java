package com.epam.gym.messaging;

import com.epam.gym.dto.client.WorkloadRequest;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class WorkloadMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(WorkloadMessageProducer.class);

    private static final String TRANSACTION_ID_KEY = "transactionId";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final JmsTemplate jmsTemplate;
    private final String workloadQueue;

    public WorkloadMessageProducer(JmsTemplate jmsTemplate,
                                   @Value("${gym.messaging.workload-queue}") String workloadQueue) {
        this.jmsTemplate = jmsTemplate;
        this.workloadQueue = workloadQueue;
    }

    public void sendWorkload(WorkloadRequest request) {
        String authHeader = extractAuthHeader();
        String transactionId = MDC.get(TRANSACTION_ID_KEY);

        try {
            jmsTemplate.convertAndSend(workloadQueue, request, message -> {
                if (authHeader != null) {
                    message.setStringProperty(AUTHORIZATION_HEADER, authHeader);
                }
                if (transactionId != null) {
                    message.setStringProperty(TRANSACTION_ID_KEY, transactionId);
                }
                return message;
            });

            log.info("Sent workload message [{}] for trainer '{}' to queue '{}' (txId={})",
                    request.getActionType(), request.getTrainerUsername(), workloadQueue, transactionId);

        } catch (JmsException ex) {
            log.error("CRITICAL: Failed to send workload message [{}] for trainer '{}' (txId={}). "
                            + "Message will NOT be published. Enclosing transaction will roll back. "
                            + "Error: {}",
                    request.getActionType(), request.getTrainerUsername(), transactionId, ex.getMessage(), ex);
            throw ex;  // Triggering rollback
        }
    }

    private String extractAuthHeader() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            return request.getHeader(AUTHORIZATION_HEADER);
        }
        return null;
    }
}