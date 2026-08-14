package com.epam.gym.service;

import com.epam.gym.dao.TraineeDao;
import com.epam.gym.dao.TrainerDao;
import com.epam.gym.dao.TrainingDao;
import com.epam.gym.dao.TrainingTypeDao;
import com.epam.gym.dto.client.ActionType;
import com.epam.gym.dto.client.WorkloadRequest;
import com.epam.gym.dto.response.TrainerShortResponse;
import com.epam.gym.dto.response.TrainingResponse;
import com.epam.gym.dto.response.TrainingTypeResponse;
import com.epam.gym.exception.custom.EntityNotFoundException;
import com.epam.gym.mapper.TrainerMapper;
import com.epam.gym.mapper.TrainingMapper;
import com.epam.gym.messaging.WorkloadMessageProducer;
import com.epam.gym.model.*;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
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

    @Mock private TrainingDao trainingDao;
    @Mock private TraineeDao traineeDao;
    @Mock private TrainerDao trainerDao;
    @Mock private TrainingTypeDao trainingTypeDao;
    @Mock private TrainingMapper trainingMapper;
    @Mock private TrainerMapper trainerMapper;
    @Mock private WorkloadMessageProducer workloadMessageProducer;

    private MeterRegistry meterRegistry;
    private TrainingService trainingService;

    private Trainee trainee;
    private Trainer trainer;
    private TrainingType strengthType;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();

        trainingService = new TrainingService(
                trainingDao, traineeDao, trainerDao, trainingTypeDao,
                trainingMapper, trainerMapper, workloadMessageProducer,
                meterRegistry);

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
                "John.Smith", "Bruce.Wayne",
                "Strength Session", LocalDate.now(), 45);

        ArgumentCaptor<Training> captor = ArgumentCaptor.forClass(Training.class);
        verify(trainingDao).save(captor.capture());
        Training saved = captor.getValue();

        assertThat(saved.getTrainee()).isSameAs(trainee);
        assertThat(saved.getTrainer()).isSameAs(trainer);
        assertThat(trainee.getTrainers()).contains(trainer);
        verify(traineeDao).update(trainee);

        // workload message must be sent asynchronously
        verify(workloadMessageProducer).sendWorkload(any(WorkloadRequest.class));
    }

    @Test
    void addTraining_shouldSendWorkloadMessageWithCorrectData() {
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee));
        when(trainerDao.findByUsername("Bruce.Wayne")).thenReturn(Optional.of(trainer));
        when(trainingDao.save(any(Training.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDate date = LocalDate.of(2024, 1, 10);
        trainingService.addTraining(
                "John.Smith", "Bruce.Wayne",
                "Strength Session", date, 45);

        ArgumentCaptor<WorkloadRequest> captor = ArgumentCaptor.forClass(WorkloadRequest.class);
        verify(workloadMessageProducer).sendWorkload(captor.capture());

        WorkloadRequest sent = captor.getValue();
        assertThat(sent.getTrainerUsername()).isEqualTo("Bruce.Wayne");
        assertThat(sent.getTrainerFirstName()).isEqualTo("Bruce");
        assertThat(sent.getTrainerLastName()).isEqualTo("Wayne");
        assertThat(sent.getActionType()).isEqualTo(ActionType.ADD);
        assertThat(sent.getTrainingDate()).isEqualTo(date);
        assertThat(sent.getTrainingDuration()).isEqualTo(45);
        assertThat(sent.getIsActive()).isTrue();
    }

    @Test
    void addTraining_shouldThrow_whenTraineeNotFound() {
        when(traineeDao.findByUsername("Ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainingService.addTraining(
                "Ghost", "Bruce.Wayne",
                "Session", LocalDate.now(), 30))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Trainee not found");

        // no message should be sent if training was not created
        verify(workloadMessageProducer, never()).sendWorkload(any());
    }

    @Test
    void addTraining_shouldThrow_whenTrainerNotFound() {
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee));
        when(trainerDao.findByUsername("GhostTrainer")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainingService.addTraining(
                "John.Smith", "GhostTrainer",
                "Session", LocalDate.now(), 30))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Trainer not found");

        verify(workloadMessageProducer, never()).sendWorkload(any());
    }

    @Test
    void addTraining_shouldThrow_whenNameBlank() {
        assertThatThrownBy(() -> trainingService.addTraining(
                "John.Smith", "Bruce.Wayne",
                " ", LocalDate.now(), 30))
                .isInstanceOf(IllegalArgumentException.class);

        verify(workloadMessageProducer, never()).sendWorkload(any());
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