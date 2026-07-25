package com.epam.gym.service;

import com.epam.gym.dao.TraineeDao;
import com.epam.gym.dao.TrainerDao;
import com.epam.gym.dao.TrainingDao;
import com.epam.gym.dao.TrainingTypeDao;
import com.epam.gym.dto.response.TrainerShortResponse;
import com.epam.gym.dto.response.TrainingResponse;
import com.epam.gym.dto.response.TrainingTypeResponse;
import com.epam.gym.exception.AuthenticationException;
import com.epam.gym.exception.EntityNotFoundException;
import com.epam.gym.mapper.TrainerMapper;
import com.epam.gym.mapper.TrainingMapper;
import com.epam.gym.model.*;
import com.epam.gym.service.AuthService;
import com.epam.gym.service.TrainingService;


import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingServiceTest {

    @Mock
    private TrainingDao trainingDao;
    @Mock
    private TraineeDao traineeDao;
    @Mock
    private TrainerDao trainerDao;
    @Mock
    private TrainingTypeDao trainingTypeDao;
    @Mock
    private AuthService authService;
    @Mock
    private TrainingMapper trainingMapper;
    @Mock
    private TrainerMapper trainerMapper;

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private TrainingService trainingService;

    private Trainee trainee;
    private Trainer trainer;
    private TrainingType strengthType;

    @BeforeEach
    void setUp() {
        strengthType = new TrainingType(TrainingTypeName.STRENGTH);

        User traineeUser = User.builder()
                .firstName("John").lastName("Smith")
                .username("John.Smith").password("hashed").isActive(true)
                .build();
        trainee = Trainee.builder()
                .user(traineeUser)
                .trainers(new HashSet<>())
                .build();

        User trainerUser = User.builder()
                .firstName("Bruce").lastName("Wayne")
                .username("Bruce.Wayne").password("hashed").isActive(true)
                .build();
        trainer = Trainer.builder()
                .user(trainerUser)
                .specialization(strengthType)
                .build();
    }

    // ---------- addTraining ----------

    @Test
    void addTraining_shouldPersistAndLinkTrainerToTrainee() {
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee));
        when(trainerDao.findByUsername("Bruce.Wayne")).thenReturn(Optional.of(trainer));
        when(trainingDao.save(any(Training.class))).thenAnswer(inv -> inv.getArgument(0));

        trainingService.addTraining(
                "John.Smith", "raw",
                "John.Smith", "Bruce.Wayne",
                "Strength Session", LocalDate.now(), 45);

        ArgumentCaptor<Training> captor = ArgumentCaptor.forClass(Training.class);
        verify(trainingDao).save(captor.capture());
        Training saved = captor.getValue();

        assertThat(saved.getTrainee()).isSameAs(trainee);
        assertThat(saved.getTrainer()).isSameAs(trainer);
        verify(authService).authenticate("John.Smith", "raw");
    }

    @Test
    void addTraining_shouldThrow_whenAuthFails() {
        doThrow(new AuthenticationException("bad"))
                .when(authService).authenticate("John.Smith", "wrong");

        assertThatThrownBy(() -> trainingService.addTraining(
                "John.Smith", "wrong", "John.Smith", "Bruce.Wayne",
                "Session", LocalDate.now(), 30))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    void addTraining_shouldThrow_whenTraineeNotFound() {
        when(traineeDao.findByUsername("Ghost")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> trainingService.addTraining(
                "John.Smith", "raw", "Ghost", "Bruce.Wayne",
                "Session", LocalDate.now(), 30))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void addTraining_shouldThrow_whenNameBlank() {
        assertThatThrownBy(() -> trainingService.addTraining(
                "John.Smith", "raw", "John.Smith", "Bruce.Wayne",
                " ", LocalDate.now(), 30))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- getTraineeTrainings ----------

    @Test
    void getTraineeTrainings_shouldDelegateToDaoAndMap() {
        List<Training> entities = List.of(new Training());
        List<TrainingResponse> mapped = List.of(new TrainingResponse(
                "S", LocalDate.now(), TrainingTypeName.STRENGTH, 45, "Bruce", "John"));
        when(trainingDao.findTraineeTrainings("John.Smith", null, null, null, null))
                .thenReturn(entities);
        when(trainingMapper.toResponseList(entities)).thenReturn(mapped);

        List<TrainingResponse> result = trainingService.getTraineeTrainings(
                "John.Smith", null, null, null, null);

        assertThat(result).isEqualTo(mapped);

        // ПЕРЕВІРКА МЕТРИКИ: виклик timer.record() +1
        assertThat(meterRegistry.timer("gym.training.search.time").count()).isEqualTo(1L);
    }

    // ---------- getTrainerTrainings ----------

    @Test
    void getTrainerTrainings_shouldDelegateToDaoAndMap() {
        List<Training> entities = List.of(new Training());
        List<TrainingResponse> mapped = List.of(new TrainingResponse(
                "S", LocalDate.now(), TrainingTypeName.STRENGTH, 45, "Bruce", "John"));
        when(trainingDao.findTrainerTrainings("Bruce.Wayne", null, null, null))
                .thenReturn(entities);
        when(trainingMapper.toResponseList(entities)).thenReturn(mapped);

        List<TrainingResponse> result = trainingService.getTrainerTrainings(
                "Bruce.Wayne", null, null, null);

        assertThat(result).isEqualTo(mapped);

        // ПЕРЕВІРКА МЕТРИКИ: виклик timer.record() +1
        assertThat(meterRegistry.timer("gym.training.search.time").count()).isEqualTo(1L);
    }

    // ---------- updateTraineeTrainers ----------

    @Test
    void updateTraineeTrainers_shouldReplaceTrainersSetAndReturnShortList() {
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee));
        when(trainerDao.findByUsernames(List.of("Bruce.Wayne"))).thenReturn(List.of(trainer));
        when(traineeDao.update(any(Trainee.class))).thenAnswer(inv -> inv.getArgument(0));
        when(trainerMapper.toShortList(anyList()))
                .thenReturn(List.of(new TrainerShortResponse(
                        "Bruce.Wayne", "Bruce", "Wayne", TrainingTypeName.STRENGTH)));

        List<TrainerShortResponse> result = trainingService.updateTraineeTrainers(
                "John.Smith", List.of("Bruce.Wayne"));

        assertThat(result).hasSize(1);
    }

    @Test
    void updateTraineeTrainers_shouldThrow_whenListEmpty() {
        assertThatThrownBy(() -> trainingService.updateTraineeTrainers(
                "John.Smith", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- getTrainingTypes ----------

    @Test
    void getTrainingTypes_shouldReturnMappedList() {
        List<TrainingType> types = List.of(strengthType);
        when(trainingTypeDao.findAll()).thenReturn(types);
        when(trainingMapper.toTypeResponseList(types))
                .thenReturn(List.of(new TrainingTypeResponse(TrainingTypeName.STRENGTH, 1L)));

        List<TrainingTypeResponse> result = trainingService.getTrainingTypes();
        assertThat(result).hasSize(1);
    }
}