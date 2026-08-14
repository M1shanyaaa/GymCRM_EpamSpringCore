package com.epam.gym.workload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkloadRequest {

    @NotBlank(message = "Username cannot be blank")
    private String trainerUsername;

    @NotBlank(message = "First name cannot be blank")
    private String trainerFirstName;

    @NotBlank(message = "Last name cannot be blank")
    private String trainerLastName;

    @NotNull(message = "Active status must be provided")
    private Boolean isActive;

    @NotNull(message = "Training date must be provided")
    private LocalDate trainingDate;

    @NotNull(message = "Training duration must be provided")
    @Positive(message = "Duration must be positive")
    private Integer trainingDuration;

    @NotNull(message = "Action type must be provided")
    private ActionType actionType;
}