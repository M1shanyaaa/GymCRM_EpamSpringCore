package com.epam.gym.workload.messaging;

import com.epam.gym.workload.dto.ActionType;
import com.epam.gym.workload.dto.WorkloadRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;

class DeadLetterQueueListenerTest {

    private final DeadLetterQueueListener listener = new DeadLetterQueueListener();

    private WorkloadRequest sample() {
        return new WorkloadRequest(
                "Bruce.Wayne", "Bruce", "Wayne",
                true, LocalDate.of(2024, 1, 10), 45, ActionType.ADD);
    }

    @Test
    void handleDeadLetter_withFullData_doesNotThrow() {
        assertThatCode(() -> listener.handleDeadLetter(
                sample(), "Missing/invalid required fields: trainerUsername", "tx-1"))
                .doesNotThrowAnyException();
    }

    @Test
    void handleDeadLetter_withNullReason_doesNotThrow() {
        assertThatCode(() -> listener.handleDeadLetter(sample(), null, "tx-2"))
                .doesNotThrowAnyException();
    }

    @Test
    void handleDeadLetter_withNullPayload_doesNotThrow() {
        assertThatCode(() -> listener.handleDeadLetter(null, "some reason", null))
                .doesNotThrowAnyException();
    }

    @Test
    void handleDeadLetter_withNullTransactionId_doesNotThrow() {
        assertThatCode(() -> listener.handleDeadLetter(sample(), "reason", null))
                .doesNotThrowAnyException();
    }
}