package com.epam.gym.workload.messaging;

import com.epam.gym.workload.dto.ActionType;
import com.epam.gym.workload.dto.WorkloadRequest;
import com.epam.gym.workload.exception.InvalidWorkloadMessageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkloadMessageValidatorTest {

    private WorkloadMessageValidator validator;

    @BeforeEach
    void setUp() {
        validator = new WorkloadMessageValidator();
    }

    private WorkloadRequest valid() {
        return new WorkloadRequest(
                "Bruce.Wayne", "Bruce", "Wayne",
                true, LocalDate.of(2024, 1, 10), 45, ActionType.ADD);
    }

    @Test
    void validate_allFieldsPresent_doesNotThrow() {
        assertThatCode(() -> validator.validate(valid()))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_nullPayload_throws() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(InvalidWorkloadMessageException.class)
                .hasMessageContaining("null");
    }

    @Test
    void validate_missingTrainerUsername_throwsListingField() {
        WorkloadRequest r = valid();
        r.setTrainerUsername(null);

        assertThatThrownBy(() -> validator.validate(r))
                .isInstanceOf(InvalidWorkloadMessageException.class)
                .hasMessageContaining("trainerUsername");
    }

    @Test
    void validate_blankTrainerUsername_throws() {
        WorkloadRequest r = valid();
        r.setTrainerUsername("   ");

        assertThatThrownBy(() -> validator.validate(r))
                .isInstanceOf(InvalidWorkloadMessageException.class)
                .hasMessageContaining("trainerUsername");
    }

    @Test
    void validate_missingIsActive_throws() {
        WorkloadRequest r = valid();
        r.setIsActive(null);

        assertThatThrownBy(() -> validator.validate(r))
                .isInstanceOf(InvalidWorkloadMessageException.class)
                .hasMessageContaining("isActive");
    }

    @Test
    void validate_nonPositiveDuration_throws() {
        WorkloadRequest r = valid();
        r.setTrainingDuration(0);

        assertThatThrownBy(() -> validator.validate(r))
                .isInstanceOf(InvalidWorkloadMessageException.class)
                .hasMessageContaining("trainingDuration");
    }

    @Test
    void validate_nullActionType_throws() {
        WorkloadRequest r = valid();
        r.setActionType(null);

        assertThatThrownBy(() -> validator.validate(r))
                .isInstanceOf(InvalidWorkloadMessageException.class)
                .hasMessageContaining("actionType");
    }

    @Test
    void validate_multipleMissingFields_listsAll() {
        WorkloadRequest r = valid();
        r.setTrainerUsername(null);
        r.setTrainingDate(null);

        assertThatThrownBy(() -> validator.validate(r))
                .isInstanceOf(InvalidWorkloadMessageException.class)
                .hasMessageContaining("trainerUsername")
                .hasMessageContaining("trainingDate");
    }
}