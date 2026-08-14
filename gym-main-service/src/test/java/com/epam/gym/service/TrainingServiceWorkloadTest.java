package com.epam.gym.service;

import com.epam.gym.client.WorkloadClient;
import com.epam.gym.dao.*;
import com.epam.gym.dto.client.WorkloadRequest;
import com.epam.gym.mapper.TrainerMapper;
import com.epam.gym.mapper.TrainingMapper;
import com.epam.gym.model.*;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingServiceWorkloadTest {

    @Mock private TrainingDao trainingDao;
    @Mock private TraineeDao traineeDao;
    @Mock private TrainerDao trainerDao;
    @Mock private TrainingTypeDao trainingTypeDao;
    @Mock private TrainingMapper trainingMapper;
    @Mock private TrainerMapper trainerMapper;
    @Mock private WorkloadClient workloadClient;

    private TrainingService trainingService;

    @BeforeEach
    void setUp() {
        trainingService = new TrainingService(
                trainingDao, traineeDao, trainerDao, trainingTypeDao,
                trainingMapper, trainerMapper, workloadClient,
                CircuitBreakerRegistry.ofDefaults(),
                new SimpleMeterRegistry());
    }

    private Trainee trainee() {
        User u = User.builder().username("trainee1").firstName("T").lastName("One").isActive(true).build();
        Trainee t = Trainee.builder().user(u).trainers(new HashSet<>()).build();
        return t;
    }

    private Trainer trainer() {
        User u = User.builder()
                .username("trainer1")
                .firstName("Tr")
                .lastName("Ainer")
                .isActive(true)
                .build();
        TrainingType type = mock(TrainingType.class);
        return Trainer.builder()
                .user(u)
                .specialization(type)
                .build();
    }

    @Test
    void addTraining_callsWorkloadClientWithAddAction() {
        when(traineeDao.findByUsername("trainee1")).thenReturn(Optional.of(trainee()));
        when(trainerDao.findByUsername("trainer1")).thenReturn(Optional.of(trainer()));

        trainingService.addTraining("trainee1", "trainer1",
                "Cardio", LocalDate.of(2024, 1, 10), 60);

        verify(workloadClient).updateWorkload(any(WorkloadRequest.class));
        verify(trainingDao).save(any(Training.class));
    }

    @Test
    void addTraining_whenWorkloadFails_fallbackPreventsException() {
        when(traineeDao.findByUsername("trainee1")).thenReturn(Optional.of(trainee()));
        when(trainerDao.findByUsername("trainer1")).thenReturn(Optional.of(trainer()));
        // симулюємо падіння downstream-сервісу
        doThrow(new RuntimeException("workload service down"))
                .when(workloadClient).updateWorkload(any(WorkloadRequest.class));

        // fallback (catch) не дає прокинути виняток — training все одно збережений
        trainingService.addTraining("trainee1", "trainer1",
                "Cardio", LocalDate.of(2024, 1, 10), 60);

        verify(trainingDao).save(any(Training.class)); // основна транзакція виконана
        verify(workloadClient).updateWorkload(any(WorkloadRequest.class));
    }
}