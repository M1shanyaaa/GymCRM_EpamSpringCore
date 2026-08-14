package com.epam.gym.workload.messaging;

import com.epam.gym.workload.dto.ActionType;
import com.epam.gym.workload.dto.WorkloadRequest;
import com.epam.gym.workload.exception.InvalidWorkloadMessageException;
import com.epam.gym.workload.security.JwtService;
import com.epam.gym.workload.service.WorkloadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkloadMessageListenerTest {

    private static final String DLQ = "workload.dlq";
    private static final String VALID_TOKEN = "Bearer valid.jwt.token";

    @Mock private WorkloadService workloadService;
    @Mock private JwtService jwtService;
    @Mock private WorkloadMessageValidator validator;
    @Mock private JmsTemplate jmsTemplate;

    private WorkloadMessageListener listener() {
        return new WorkloadMessageListener(
                workloadService, jwtService, validator, jmsTemplate, DLQ);
    }

    private WorkloadRequest valid() {
        return new WorkloadRequest(
                "Bruce.Wayne", "Bruce", "Wayne",
                true, LocalDate.of(2024, 1, 10), 45, ActionType.ADD);
    }

    // ---------- happy path ----------

    @Test
    void receiveWorkload_validAndAuthorized_processesMessage() {
        WorkloadRequest request = valid();
        // validator passes (no exception)
        when(jwtService.extractUsername("valid.jwt.token")).thenReturn("Bruce.Wayne");
        when(jwtService.isTokenValid(eq("valid.jwt.token"), any(UserDetails.class))).thenReturn(true);

        listener().receiveWorkload(request, VALID_TOKEN, "tx-1");

        verify(validator).validate(request);
        verify(workloadService).processWorkload(request);
        // no DLQ
        verify(jmsTemplate, never()).convertAndSend(anyString(), any(Object.class), any(MessagePostProcessor.class));
    }

    // ---------- invalid message -> DLQ, no retry ----------

    @Test
    void receiveWorkload_invalidMessage_routesToDlqWithoutProcessing() {
        WorkloadRequest request = valid();
        request.setTrainerUsername(null);
        doThrow(new InvalidWorkloadMessageException("Missing/invalid required fields: trainerUsername"))
                .when(validator).validate(request);

        listener().receiveWorkload(request, VALID_TOKEN, "tx-2");

        // routed to DLQ
        verify(jmsTemplate).convertAndSend(eq(DLQ), eq(request), any(MessagePostProcessor.class));
        // NOT processed, NOT authorized (returned early)
        verify(workloadService, never()).processWorkload(any());
        verifyNoInteractions(jwtService);
    }

    @Test
    void receiveWorkload_invalidMessage_doesNotRethrow_soNoRedelivery() {
        WorkloadRequest request = valid();
        request.setTrainerUsername(null);
        doThrow(new InvalidWorkloadMessageException("Missing/invalid required fields: trainerUsername"))
                .when(validator).validate(request);

        // must NOT throw -> message is acked -> broker does not redeliver
        listener().receiveWorkload(request, VALID_TOKEN, "tx-3");

        verify(jmsTemplate).convertAndSend(eq(DLQ), eq(request), any(MessagePostProcessor.class));
    }

    // ---------- unauthorized -> DLQ ----------

    @Test
    void receiveWorkload_missingAuthHeader_routesToDlq() {
        WorkloadRequest request = valid();
        // validator passes

        listener().receiveWorkload(request, null, "tx-4");

        verify(jmsTemplate).convertAndSend(eq(DLQ), eq(request), any(MessagePostProcessor.class));
        verify(workloadService, never()).processWorkload(any());
    }

    @Test
    void receiveWorkload_invalidToken_routesToDlq() {
        WorkloadRequest request = valid();
        when(jwtService.extractUsername("valid.jwt.token")).thenReturn("Bruce.Wayne");
        when(jwtService.isTokenValid(eq("valid.jwt.token"), any(UserDetails.class))).thenReturn(false);

        listener().receiveWorkload(request, VALID_TOKEN, "tx-5");

        verify(jmsTemplate).convertAndSend(eq(DLQ), eq(request), any(MessagePostProcessor.class));
        verify(workloadService, never()).processWorkload(any());
    }

    @Test
    void receiveWorkload_malformedAuthHeader_routesToDlq() {
        WorkloadRequest request = valid();
        // header without "Bearer " prefix

        listener().receiveWorkload(request, "NotBearer token", "tx-6");

        verify(jmsTemplate).convertAndSend(eq(DLQ), eq(request), any(MessagePostProcessor.class));
        verify(workloadService, never()).processWorkload(any());
    }

    // ---------- unexpected error -> rethrow (retryable) ----------

    @Test
    void receiveWorkload_processingThrows_rethrowsForRetry() {
        WorkloadRequest request = valid();
        when(jwtService.extractUsername("valid.jwt.token")).thenReturn("Bruce.Wayne");
        when(jwtService.isTokenValid(eq("valid.jwt.token"), any(UserDetails.class))).thenReturn(true);
        doThrow(new RuntimeException("DB down")).when(workloadService).processWorkload(request);

        assertThatThrownBy(() -> listener().receiveWorkload(request, VALID_TOKEN, "tx-7"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB down");

        // NOT sent to DLQ manually -> broker redelivery handles it
        verify(jmsTemplate, never()).convertAndSend(eq(DLQ), any(Object.class), any(MessagePostProcessor.class));
    }
}