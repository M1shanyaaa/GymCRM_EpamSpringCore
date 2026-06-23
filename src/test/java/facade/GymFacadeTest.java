package facade;

import com.epam.gym.facade.GymFacade;
import com.epam.gym.model.Trainee;
import com.epam.gym.model.Trainer;
import com.epam.gym.model.Training;
import com.epam.gym.service.TraineeService;
import com.epam.gym.service.TrainerService;
import com.epam.gym.service.TrainingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GymFacadeTest {

    @Mock
    private TraineeService traineeService;
    @Mock
    private TrainerService trainerService;
    @Mock
    private TrainingService trainingService;

    @InjectMocks
    private GymFacade facade;

    // ---- Trainee ----

    @Test
    void createTrainee_shouldDelegateToService() {
        Trainee t = Trainee.builder().firstName("John").build();
        when(traineeService.create(t)).thenReturn(t);

        assertThat(facade.createTrainee(t)).isEqualTo(t);
        verify(traineeService).create(t);
    }

    @Test
    void updateTrainee_shouldDelegateToService() {
        Trainee t = Trainee.builder().userId(1L).build();
        when(traineeService.update(t)).thenReturn(t);

        assertThat(facade.updateTrainee(t)).isEqualTo(t);
        verify(traineeService).update(t);
    }

    @Test
    void deleteTrainee_shouldDelegateToService() {
        facade.deleteTrainee(1L);
        verify(traineeService).delete(1L);
    }

    @Test
    void getTrainee_shouldDelegateToService() {
        Trainee t = Trainee.builder().userId(1L).build();
        when(traineeService.select(1L)).thenReturn(t);

        assertThat(facade.getTrainee(1L)).isEqualTo(t);
        verify(traineeService).select(1L);
    }

    @Test
    void getAllTrainees_shouldDelegateToService() {
        List<Trainee> list = List.of(Trainee.builder().build());
        when(traineeService.selectAll()).thenReturn(list);

        assertThat(facade.getAllTrainees()).isEqualTo(list);
        verify(traineeService).selectAll();
    }

    // ---- Trainer ----

    @Test
    void createTrainer_shouldDelegateToService() {
        Trainer t = Trainer.builder().firstName("Mike").build();
        when(trainerService.create(t)).thenReturn(t);

        assertThat(facade.createTrainer(t)).isEqualTo(t);
        verify(trainerService).create(t);
    }

    @Test
    void updateTrainer_shouldDelegateToService() {
        Trainer t = Trainer.builder().userId(1L).build();
        when(trainerService.update(t)).thenReturn(t);

        assertThat(facade.updateTrainer(t)).isEqualTo(t);
        verify(trainerService).update(t);
    }

    @Test
    void getTrainer_shouldDelegateToService() {
        Trainer t = Trainer.builder().userId(1L).build();
        when(trainerService.select(1L)).thenReturn(t);

        assertThat(facade.getTrainer(1L)).isEqualTo(t);
        verify(trainerService).select(1L);
    }

    @Test
    void getAllTrainers_shouldDelegateToService() {
        List<Trainer> list = List.of(Trainer.builder().build());
        when(trainerService.selectAll()).thenReturn(list);

        assertThat(facade.getAllTrainers()).isEqualTo(list);
        verify(trainerService).selectAll();
    }

    // ---- Training ----

    @Test
    void createTraining_shouldDelegateToService() {
        Training t = Training.builder().traineeId(1L).trainerId(2L).build();
        when(trainingService.create(t)).thenReturn(t);

        assertThat(facade.createTraining(t)).isEqualTo(t);
        verify(trainingService).create(t);
    }

    @Test
    void getTraining_shouldDelegateToService() {
        Training t = Training.builder().traineeId(1L).build();
        when(trainingService.select(1L)).thenReturn(t);

        assertThat(facade.getTraining(1L)).isEqualTo(t);
        verify(trainingService).select(1L);
    }

    @Test
    void getAllTrainings_shouldDelegateToService() {
        List<Training> list = List.of(Training.builder().build());
        when(trainingService.selectAll()).thenReturn(list);

        assertThat(facade.getAllTrainings()).isEqualTo(list);
        verify(trainingService).selectAll();
    }
}
