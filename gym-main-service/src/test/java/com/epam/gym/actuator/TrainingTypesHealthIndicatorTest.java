package com.epam.gym.actuator;

import com.epam.gym.dto.response.TrainingTypeResponse;
import com.epam.gym.model.TrainingTypeName;
import com.epam.gym.service.TrainingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingTypesHealthIndicatorTest {

    @Mock
    private TrainingService trainingService;

    @InjectMocks
    private TrainingTypesHealthIndicator indicator;

    @Test
    void health_shouldReturnUp_whenTypesExist() {
        when(trainingService.getTrainingTypes())
                .thenReturn(List.of(new TrainingTypeResponse(TrainingTypeName.FITNESS, 1L)));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("message", "Training types are populated");
        assertThat(health.getDetails()).containsEntry("count", 1);
    }

    @Test
    void health_shouldReturnDown_whenTypesEmpty() {
        when(trainingService.getTrainingTypes()).thenReturn(Collections.emptyList());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsEntry("message", "Training types dictionary is empty! Application might not function correctly.");
    }

    @Test
    void health_shouldReturnDown_whenExceptionThrown() {
        when(trainingService.getTrainingTypes()).thenThrow(new RuntimeException("DB Connection Refused"));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("message", "Failed to fetch training types from database");
    }
}