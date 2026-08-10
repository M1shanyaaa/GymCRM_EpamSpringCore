package com.epam.gym.workload.messaging;

import com.epam.gym.workload.dto.WorkloadRequest;
import com.epam.gym.workload.exception.InvalidWorkloadMessageException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class WorkloadMessageValidator {

    /**
     * Validates required fields. Throws InvalidWorkloadMessageException
     * listing everything that is missing — this message can never succeed on retry.
     */
    public void validate(WorkloadRequest request) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            throw new InvalidWorkloadMessageException("Message payload is null");
        }
        if (isBlank(request.getTrainerUsername())) {
            errors.add("trainerUsername");
        }
        if (isBlank(request.getTrainerFirstName())) {
            errors.add("trainerFirstName");
        }
        if (isBlank(request.getTrainerLastName())) {
            errors.add("trainerLastName");
        }
        if (request.getIsActive() == null) {
            errors.add("isActive");
        }
        if (request.getTrainingDate() == null) {
            errors.add("trainingDate");
        }
        if (request.getTrainingDuration() == null || request.getTrainingDuration() <= 0) {
            errors.add("trainingDuration");
        }
        if (request.getActionType() == null) {
            errors.add("actionType");
        }

        if (!errors.isEmpty()) {
            throw new InvalidWorkloadMessageException(
                    "Missing/invalid required fields: " + String.join(", ", errors));
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}