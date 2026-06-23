package service;

import com.epam.gym.dao.TrainingDao;
import com.epam.gym.model.Training;
import com.epam.gym.model.TrainingType;
import com.epam.gym.model.TrainingTypeName;
import com.epam.gym.service.TrainingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingServiceTest {

    @Mock
    private TrainingDao trainingDao;

    private TrainingService trainingService;

    @BeforeEach
    void setUp() {
        trainingService = new TrainingService();
        trainingService.setTrainingDao(trainingDao);
    }

    private Training validTraining() {
        return Training.builder()
                .traineeId(1L)
                .trainerId(2L)
                .trainingName("Morning Fitness")
                .trainingType(new TrainingType(TrainingTypeName.FITNESS))
                .trainingDate(LocalDate.now())
                .trainingDuration(Duration.ofHours(1))
                .build();
    }

    @Test
    void create_shouldSaveTraining() {
        Training input = validTraining();
        when(trainingDao.save(any(Training.class))).thenAnswer(inv -> inv.getArgument(0));

        Training result = trainingService.create(input);

        assertThat(result).isEqualTo(input);
        verify(trainingDao).save(input);
    }

    @Test
    void create_shouldThrow_whenTrainingIsNull() {
        assertThatThrownBy(() -> trainingService.create(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");

        verifyNoInteractions(trainingDao);
    }

    @Test
    void create_shouldThrow_whenTraineeIdIsNull() {
        Training input = validTraining();
        input.setTraineeId(null);

        assertThatThrownBy(() -> trainingService.create(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trainee and trainer");

        verifyNoInteractions(trainingDao);
    }

    @Test
    void create_shouldThrow_whenTrainerIdIsNull() {
        Training input = validTraining();
        input.setTrainerId(null);

        assertThatThrownBy(() -> trainingService.create(input))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(trainingDao);
    }

    @Test
    void select_shouldReturnTraining_whenFound() {
        Training training = validTraining();
        when(trainingDao.findById(1L)).thenReturn(Optional.of(training));

        Training result = trainingService.select(1L);

        assertThat(result).isEqualTo(training);
    }

    @Test
    void select_shouldThrow_whenNotFound() {
        when(trainingDao.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainingService.select(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void selectAll_shouldReturnAllTrainings() {
        List<Training> trainings = List.of(validTraining(), validTraining());
        when(trainingDao.findAll()).thenReturn(trainings);

        List<Training> result = trainingService.selectAll();

        assertThat(result).hasSize(2);
    }
}
