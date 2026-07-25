package com.epam.gym.dto.response;

/**
 * Short trainee view used inside trainer profile / trainees lists.
 */
public record TraineeShortResponse(
        String username,
        String firstName,
        String lastName
) {
}