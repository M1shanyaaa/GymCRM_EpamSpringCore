package service;

import com.epam.gym.dao.TraineeDao;
import com.epam.gym.dao.TrainerDao;
import com.epam.gym.dao.TrainingDao;
import com.epam.gym.dao.TrainingTypeDao;
import com.epam.gym.dto.response.TrainerShortResponse;
import com.epam.gym.dto.response.TrainingResponse;
import com.epam.gym.dto.response.TrainingTypeResponse;
import com.epam.gym.exception.EntityNotFoundException;
import com.epam.gym.mapper.TrainerMapper;
import com.epam.gym.mapper.TrainingMapper;
import com.epam.gym.model.*;
import com.epam.gym.service.AuthService;
import com.epam.gym.service.TrainingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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
    @Mock private AuthService authService;
    @Mock private TrainingMapper trainingMapper;
    @Mock private TrainerMapper trainerMapper;

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
        assertThat(saved.getTrainingName()).isEqualTo("Strength Session");
        assertThat(saved.getTrainingType()).isEqualTo(strengthType);
        assertThat(saved.getTrainingDuration()).isEqualTo(45);
        assertThat(trainee.getTrainers()).contains(trainer);
        verify(traineeDao).update(trainee);
        verify(authService).authenticate("John.Smith", "raw");
    }

    @Test
    void addTraining_shouldThrow_whenTraineeNotFound() {
        when(traineeDao.findByUsername("Ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainingService.addTraining(
                "John.Smith", "raw", "Ghost", "Bruce.Wayne",
                "Session", LocalDate.now(), 30))
                .isInstanceOf(EntityNotFoundException.class);
        verify(trainingDao, never()).save(any());
    }

    @Test
    void addTraining_shouldThrow_whenTrainerNotFound() {
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee));
        when(trainerDao.findByUsername("Ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainingService.addTraining(
                "John.Smith", "raw", "John.Smith", "Ghost",
                "Session", LocalDate.now(), 30))
                .isInstanceOf(EntityNotFoundException.class);
        verify(trainingDao, never()).save(any());
    }

    @Test
    void addTraining_shouldThrow_whenNameBlank() {
        assertThatThrownBy(() -> trainingService.addTraining(
                "John.Smith", "raw", "John.Smith", "Bruce.Wayne",
                " ", LocalDate.now(), 30))
                .isInstanceOf(IllegalArgumentException.class);
        verify(trainingDao, never()).save(any());
    }

    @Test
    void addTraining_shouldThrow_whenDateNull() {
        assertThatThrownBy(() -> trainingService.addTraining(
                "John.Smith", "raw", "John.Smith", "Bruce.Wayne",
                "Session", null, 30))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addTraining_shouldThrow_whenDurationNotPositive() {
        assertThatThrownBy(() -> trainingService.addTraining(
                "John.Smith", "raw", "John.Smith", "Bruce.Wayne",
                "Session", LocalDate.now(), 0))
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
                "John.Smith", "raw", null, null, null, null);

        assertThat(result).isEqualTo(mapped);
        verify(authService).authenticate("John.Smith", "raw");
    }

    @Test
    void getTraineeTrainings_shouldPassFiltersToDao() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);
        when(trainingDao.findTraineeTrainings(
                "John.Smith", from, to, "Bruce", TrainingTypeName.STRENGTH))
                .thenReturn(List.of());
        when(trainingMapper.toResponseList(anyList())).thenReturn(List.of());

        trainingService.getTraineeTrainings(
                "John.Smith", "raw", from, to, "Bruce", TrainingTypeName.STRENGTH);

        verify(trainingDao).findTraineeTrainings(
                "John.Smith", from, to, "Bruce", TrainingTypeName.STRENGTH);
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
                "Bruce.Wayne", "raw", null, null, null);

        assertThat(result).isEqualTo(mapped);
        verify(authService).authenticate("Bruce.Wayne", "raw");
    }

    // ---------- updateTraineeTrainers ----------

    @Test
    void updateTraineeTrainers_shouldReplaceTrainersSetAndReturnShortList() {
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee));
        when(trainerDao.findByUsernames(List.of("Bruce.Wayne")))
                .thenReturn(List.of(trainer));
        when(traineeDao.update(any(Trainee.class))).thenAnswer(inv -> inv.getArgument(0));
        when(trainerMapper.toShortList(anyList()))
                .thenReturn(List.of(new TrainerShortResponse(
                        "Bruce.Wayne", "Bruce", "Wayne", TrainingTypeName.STRENGTH)));

        List<TrainerShortResponse> result = trainingService.updateTraineeTrainers(
                "John.Smith", "raw", List.of("Bruce.Wayne"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).username()).isEqualTo("Bruce.Wayne");
        assertThat(trainee.getTrainers()).containsExactly(trainer);
        verify(authService).authenticate("John.Smith", "raw");
    }

    @Test
    void updateTraineeTrainers_shouldThrow_whenListEmpty() {
        assertThatThrownBy(() -> trainingService.updateTraineeTrainers(
                "John.Smith", "raw", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(traineeDao, never()).update(any());
    }

    @Test
    void updateTraineeTrainers_shouldThrow_whenTraineeNotFound() {
        when(traineeDao.findByUsername("Ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainingService.updateTraineeTrainers(
                "Ghost", "raw", List.of("Bruce.Wayne")))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateTraineeTrainers_shouldThrow_whenSomeTrainerMissing() {
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee));
        when(trainerDao.findByUsernames(List.of("Bruce.Wayne", "Ghost")))
                .thenReturn(List.of(trainer));

        assertThatThrownBy(() -> trainingService.updateTraineeTrainers(
                "John.Smith", "raw", List.of("Bruce.Wayne", "Ghost")))
                .isInstanceOf(EntityNotFoundException.class);
        verify(traineeDao, never()).update(any());
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
        assertThat(result.get(0).trainingType()).isEqualTo(TrainingTypeName.STRENGTH);
    }
}