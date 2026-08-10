package com.epam.gym.workload.messaging;

import com.epam.gym.workload.dto.WorkloadRequest;
import com.epam.gym.workload.exception.InvalidWorkloadMessageException;
import com.epam.gym.workload.security.JwtService;
import com.epam.gym.workload.service.WorkloadService;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;

@Component
public class WorkloadMessageListener {

    private static final Logger log = LoggerFactory.getLogger(WorkloadMessageListener.class);
    private static final String TRANSACTION_ID_KEY = "transactionId";

    private final WorkloadService workloadService;
    private final JwtService jwtService;
    private final WorkloadMessageValidator validator;
    private final JmsTemplate jmsTemplate;
    private final String deadLetterQueue;

    public WorkloadMessageListener(WorkloadService workloadService,
                                   JwtService jwtService,
                                   WorkloadMessageValidator validator,
                                   JmsTemplate jmsTemplate,
                                   @Value("${gym.messaging.workload-dlq}") String deadLetterQueue) {
        this.workloadService = workloadService;
        this.jwtService = jwtService;
        this.validator = validator;
        this.jmsTemplate = jmsTemplate;
        this.deadLetterQueue = deadLetterQueue;
    }

    @JmsListener(destination = "${gym.messaging.workload-queue}")
    public void receiveWorkload(
            @Payload WorkloadRequest request,
            @Header(name = "Authorization", required = false) String authHeader,
            @Header(name = TRANSACTION_ID_KEY, required = false) String transactionId) {

        MDC.put(TRANSACTION_ID_KEY, transactionId != null ? transactionId : UUID.randomUUID().toString());

        try {
            // --- 1. Validate required fields -> non-retryable ---
            try {
                validator.validate(request);
            } catch (InvalidWorkloadMessageException ex) {
                log.error("INVALID message (non-retryable), routing to DLQ. Reason: {}", ex.getMessage());
                sendToDeadLetterQueue(request, ex.getMessage());
                return; // ack original — do NOT trigger redelivery
            }

            // --- 2. Authorization (retryable-ish, but here treat invalid JWT as reject) ---
            if (!isAuthorized(authHeader)) {
                log.warn("Rejected workload message for trainer '{}': missing or invalid JWT",
                        request.getTrainerUsername());
                sendToDeadLetterQueue(request, "Unauthorized: missing or invalid JWT");
                return;
            }

            // --- 3. Process ---
            log.info("Received workload message [{}] for trainer '{}'",
                    request.getActionType(), request.getTrainerUsername());
            workloadService.processWorkload(request);
            log.info("Processed workload message for trainer '{}'", request.getTrainerUsername());

        } catch (Exception ex) {
            // Unexpected/temporary error -> rethrow so broker retries -> eventually DLQ
            log.error("Temporary failure processing workload message, will be retried: {}", ex.getMessage());
            throw ex;
        } finally {
            MDC.clear();
        }
    }

    private void sendToDeadLetterQueue(WorkloadRequest request, String reason) {
        jmsTemplate.convertAndSend(deadLetterQueue, request, message -> {
            message.setStringProperty("deadLetterReason", reason);
            message.setStringProperty(TRANSACTION_ID_KEY, MDC.get(TRANSACTION_ID_KEY));
            return message;
        });
    }

    private boolean isAuthorized(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        String jwt = authHeader.substring(7);
        try {
            String username = jwtService.extractUsername(jwt);
            if (username == null) {
                return false;
            }
            UserDetails virtualUser = new User(username, "", Collections.emptyList());
            return jwtService.isTokenValid(jwt, virtualUser);
        } catch (JwtException e) {
            log.warn("JWT validation failed for workload message: {}", e.getMessage());
            return false;
        }
    }
}