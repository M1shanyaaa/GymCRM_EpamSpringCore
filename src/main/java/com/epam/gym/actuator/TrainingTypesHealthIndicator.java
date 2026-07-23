package com.epam.gym.actuator;

import com.epam.gym.service.TrainingService;
import com.epam.gym.dto.response.TrainingTypeResponse;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TrainingTypesHealthIndicator implements HealthIndicator {

    private final TrainingService trainingService;

    public TrainingTypesHealthIndicator(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @Override
    public Health health() {
        try {
            List<TrainingTypeResponse> types = trainingService.getTrainingTypes();
            if (types != null && !types.isEmpty()) {
                return Health.up()
                        .withDetail("message", "Training types are populated")
                        .withDetail("count", types.size())
                        .build();
            } else {
                return Health.down()
                        .withDetail("message", "Training types dictionary is empty! Application might not function correctly.")
                        .build();
            }
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("message", "Failed to fetch training types from database")
                    .build();
        }
    }
}