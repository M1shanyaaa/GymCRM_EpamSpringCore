package facade;

import com.epam.gym.facade.GymFacade;
import com.epam.gym.model.Trainee;
import com.epam.gym.model.Trainer;
import com.epam.gym.model.Training;
import com.epam.gym.model.TrainingTypeName;
import com.epam.gym.service.TraineeService;
import com.epam.gym.service.TrainerService;
import com.epam.gym.service.TrainingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GymFacadeTest {

    @Mock private TraineeService traineeService;
    @Mock private TrainerService trainerService;
    @Mock private TrainingService trainingService;

    @InjectMocks private GymFacade facade;

    // ===================== Trainee =====================

    @Test
    void createTrainee_shouldDelegateToTraineeService() {
        Trainee expected = new Trainee();
        LocalDate dob = LocalDate.of(1990, 1, 1);
        when(traineeService.create("John", "Smith", dob, "Kyiv")).thenReturn(expected);

        Trainee result = facade.createTrainee("John", "Smith", dob, "Kyiv");

        assertThat(result).isSameAs(expected);
        verify(traineeService).create("John", "Smith", dob, "Kyiv");
    }

    @Test
    void getTrainee_shouldDelegate() {
        Trainee expected = new Trainee();
        when(traineeService.findByUsername("John.Smith", "pass")).thenReturn(expected);

        Trainee result = facade.getTrainee("John.Smith", "pass");

        assertThat(result).isSameAs(expected);
        verify(traineeService).findByUsername("John.Smith", "pass");
    }

    @Test
    void updateTrainee_shouldDelegate() {
        Trainee expected = new Trainee();
        LocalDate dob = LocalDate.of(1990, 1, 1);
        when(traineeService.update("John.Smith", "pass", "John", "Smith", dob, "Kyiv"))
                .thenReturn(expected);

        Trainee result = facade.updateTrainee(
                "John.Smith", "pass", "John", "Smith", dob, "Kyiv");

        assertThat(result).isSameAs(expected);
        verify(traineeService).update("John.Smith", "pass", "John", "Smith", dob, "Kyiv");
    }

    @Test
    void changeTraineePassword_shouldDelegate() {
        facade.changeTraineePassword("John.Smith", "old", "new");
        verify(traineeService).changePassword("John.Smith", "old", "new");
    }

    @Test
    void toggleTraineeActive_shouldDelegate() {
        facade.toggleTraineeActive("John.Smith", "pass");
        verify(traineeService).toggleActive("John.Smith", "pass");
    }

    @Test
    void deleteTrainee_shouldDelegate() {
        facade.deleteTrainee("John.Smith", "pass");
        verify(traineeService).delete("John.Smith", "pass");
    }

    // ===================== Trainer =====================

    @Test
    void createTrainer_shouldDelegate() {
        Trainer expected = new Trainer();
        when(trainerService.create("Bruce", "Wayne", TrainingTypeName.STRENGTH))
                .thenReturn(expected);

        Trainer result = facade.createTrainer("Bruce", "Wayne", TrainingTypeName.STRENGTH);

        assertThat(result).isSameAs(expected);
        verify(trainerService).create("Bruce", "Wayne", TrainingTypeName.STRENGTH);
    }

    @Test
    void getTrainer_shouldDelegate() {
        Trainer expected = new Trainer();
        when(trainerService.findByUsername("Bruce.Wayne", "pass")).thenReturn(expected);

        Trainer result = facade.getTrainer("Bruce.Wayne", "pass");

        assertThat(result).isSameAs(expected);
        verify(trainerService).findByUsername("Bruce.Wayne", "pass");
    }

    @Test
    void updateTrainer_shouldDelegate() {
        Trainer expected = new Trainer();
        when(trainerService.update("Bruce.Wayne", "pass", "Bruce", "Wayne", TrainingTypeName.CARDIO))
                .thenReturn(expected);

        Trainer result = facade.updateTrainer(
                "Bruce.Wayne", "pass", "Bruce", "Wayne", TrainingTypeName.CARDIO);

        assertThat(result).isSameAs(expected);
        verify(trainerService).update("Bruce.Wayne", "pass", "Bruce", "Wayne", TrainingTypeName.CARDIO);
    }

    @Test
    void changeTrainerPassword_shouldDelegate() {
        facade.changeTrainerPassword("Bruce.Wayne", "old", "new");
        verify(trainerService).changePassword("Bruce.Wayne", "old", "new");
    }

    @Test
    void toggleTrainerActive_shouldDelegate() {
        facade.toggleTrainerActive("Bruce.Wayne", "pass");
        verify(trainerService).toggleActive("Bruce.Wayne", "pass");
    }

    @Test
    void getUnassignedTrainers_shouldDelegate() {
        List<Trainer> expected = List.of(new Trainer());
        when(trainerService.findUnassignedTrainers("John.Smith", "pass")).thenReturn(expected);

        List<Trainer> result = facade.getUnassignedTrainers("John.Smith", "pass");

        assertThat(result).isSameAs(expected);
        verify(trainerService).findUnassignedTrainers("John.Smith", "pass");
    }

    // ===================== Training =====================

    @Test
    void addTraining_shouldDelegate() {
        Training expected = new Training();
        LocalDate date = LocalDate.of(2024, 6, 1);
        when(trainingService.addTraining(
                "caller", "cpass", "John.Smith", "Bruce.Wayne", "Session", date, 45))
                .thenReturn(expected);

        Training result = facade.addTraining(
                "caller", "cpass", "John.Smith", "Bruce.Wayne", "Session", date, 45);

        assertThat(result).isSameAs(expected);
        verify(trainingService).addTraining(
                "caller", "cpass", "John.Smith", "Bruce.Wayne", "Session", date, 45);
    }

    @Test
    void getTraineeTrainings_shouldDelegate() {
        List<Training> expected = List.of(new Training());
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);
        when(trainingService.getTraineeTrainings(
                "John.Smith", "pass", from, to, "Bruce", TrainingTypeName.STRENGTH))
                .thenReturn(expected);

        List<Training> result = facade.getTraineeTrainings(
                "John.Smith", "pass", from, to, "Bruce", TrainingTypeName.STRENGTH);

        assertThat(result).isSameAs(expected);
        verify(trainingService).getTraineeTrainings(
                "John.Smith", "pass", from, to, "Bruce", TrainingTypeName.STRENGTH);
    }

    @Test
    void getTrainerTrainings_shouldDelegate() {
        List<Training> expected = List.of(new Training());
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);
        when(trainingService.getTrainerTrainings(
                "Bruce.Wayne", "pass", from, to, "John"))
                .thenReturn(expected);

        List<Training> result = facade.getTrainerTrainings(
                "Bruce.Wayne", "pass", from, to, "John");

        assertThat(result).isSameAs(expected);
        verify(trainingService).getTrainerTrainings(
                "Bruce.Wayne", "pass", from, to, "John");
    }

    @Test
    void updateTraineeTrainers_shouldDelegate() {
        Set<Trainer> expected = Set.of(new Trainer());
        List<String> usernames = List.of("Bruce.Wayne", "Clark.Kent");
        when(trainingService.updateTraineeTrainers("John.Smith", "pass", usernames))
                .thenReturn(expected);

        Set<Trainer> result = facade.updateTraineeTrainers("John.Smith", "pass", usernames);

        assertThat(result).isSameAs(expected);
        verify(trainingService).updateTraineeTrainers("John.Smith", "pass", usernames);
    }
}