package util;

import com.epam.gym.dao.TraineeDao;
import com.epam.gym.dao.TrainerDao;
import com.epam.gym.model.Trainee;
import com.epam.gym.model.Trainer;
import com.epam.gym.util.UsernameGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsernameGeneratorTest {

    @Mock
    private TraineeDao traineeDao;

    @Mock
    private TrainerDao trainerDao;

    @InjectMocks
    private UsernameGenerator usernameGenerator;

    @BeforeEach
    void setUp() {
        // @InjectMocks injects mocks; but UsernameGenerator uses setter injection,
        // so we set them explicitly to be safe.
        usernameGenerator.setTraineeDao(traineeDao);
        usernameGenerator.setTrainerDao(trainerDao);
    }

    @Test
    void generate_shouldReturnBaseUsername_whenNoCollision() {
        when(traineeDao.findAll()).thenReturn(Collections.emptyList());
        when(trainerDao.findAll()).thenReturn(Collections.emptyList());

        String username = usernameGenerator.generate("John", "Smith");

        assertThat(username).isEqualTo("John.Smith");
    }

    @Test
    void generate_shouldAppendSerial_whenBaseUsernameExists() {
        Trainee existing = Trainee.builder().username("John.Smith").build();
        when(traineeDao.findAll()).thenReturn(List.of(existing));
        when(trainerDao.findAll()).thenReturn(Collections.emptyList());

        String username = usernameGenerator.generate("John", "Smith");

        assertThat(username).isEqualTo("John.Smith1");
    }

    @Test
    void generate_shouldFindNextFreeSerial_whenMultipleExist() {
        Trainee t1 = Trainee.builder().username("John.Smith").build();
        Trainee t2 = Trainee.builder().username("John.Smith1").build();
        when(traineeDao.findAll()).thenReturn(List.of(t1, t2));
        when(trainerDao.findAll()).thenReturn(Collections.emptyList());

        String username = usernameGenerator.generate("John", "Smith");

        assertThat(username).isEqualTo("John.Smith2");
    }

    @Test
    void generate_shouldCheckBothTraineesAndTrainers_forCollision() {
        Trainer trainer = Trainer.builder().username("John.Smith").build();
        when(traineeDao.findAll()).thenReturn(Collections.emptyList());
        when(trainerDao.findAll()).thenReturn(List.of(trainer));

        String username = usernameGenerator.generate("John", "Smith");

        // Collision comes from a TRAINER — generator must detect it too
        assertThat(username).isEqualTo("John.Smith1");
    }
}