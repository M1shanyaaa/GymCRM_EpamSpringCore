package com.epam.gym.workload.messaging;

import com.epam.gym.workload.dto.WorkloadRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class DeadLetterQueueListener {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterQueueListener.class);

    @JmsListener(destination = "${gym.messaging.workload-dlq}")
    public void handleDeadLetter(
            @Payload WorkloadRequest request,
            @Header(name = "deadLetterReason", required = false) String reason,
            @Header(name = "transactionId", required = false) String transactionId) {

        MDC.put("transactionId", transactionId != null ? transactionId : "N/A");
        try {
            log.error("DEAD LETTER received. Trainer='{}', reason='{}'. Message will not be processed further.",
                    request != null ? request.getTrainerUsername() : "unknown",
                    reason != null ? reason : "unknown");
            // Тут можна: зберегти в БД, надіслати алерт, тощо.
        } finally {
            MDC.clear();
        }
    }
}