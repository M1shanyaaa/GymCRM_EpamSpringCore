package service;

import com.epam.gym.dao.TraineeDao;
import com.epam.gym.model.Trainee;
import com.epam.gym.service.TraineeService;
import com.epam.gym.util.PasswordGenerator;
import com.epam.gym.util.UsernameGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TraineeServiceTest {

    @Mock
    private TraineeDao traineeDao;

    @Mock
    private UsernameGenerator usernameGenerator;

    @Mock
    private PasswordGenerator passwordGenerator;

    private TraineeService traineeService;

    @BeforeEach
    void setUp() {
        traineeService = new TraineeService();
        traineeService.setTraineeDao(traineeDao);
        traineeService.setUsernameGenerator(usernameGenerator);
        traineeService.setPasswordGenerator(passwordGenerator);
    }

    @Test
    void create_shouldGenerateUsernameAndPassword_andSave() {
        Trainee input = Trainee.builder()
                .firstName("John")
                .lastName("Smith")
                .build();

        when(usernameGenerator.generate("John", "Smith")).thenReturn("John.Smith");
        when(passwordGenerator.generate()).thenReturn("Abc1234xyz");
        when(traineeDao.save(any(Trainee.class))).thenAnswer(inv -> inv.getArgument(0));

        Trainee result = traineeService.create(input);

        assertThat(result.getUsername()).isEqualTo("John.Smith");
        assertThat(result.getPassword()).isEqualTo("Abc1234xyz");

        verify(usernameGenerator).generate("John", "Smith");
        verify(passwordGenerator).generate();
        verify(traineeDao).save(input);
    }

    @Test
    void create_shouldThrow_whenTraineeIsNull() {
        assertThatThrownBy(() -> traineeService.create(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");

        verifyNoInteractions(traineeDao, usernameGenerator, passwordGenerator);
    }

    @Test
    void create_shouldThrow_whenFirstNameIsBlank() {
        Trainee input = Trainee.builder().firstName("  ").lastName("Smith").build();

        assertThatThrownBy(() -> traineeService.create(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");

        verifyNoInteractions(traineeDao);
    }

    @Test
    void update_shouldDelegateToDao() {
        Trainee input = Trainee.builder().userId(1L).firstName("John").lastName("Smith").build();
        when(traineeDao.update(input)).thenReturn(input);

        Trainee result = traineeService.update(input);

        assertThat(result).isSameAs(input);
        verify(traineeDao).update(input);
    }

    @Test
    void update_shouldThrow_whenIdIsNull() {
        Trainee input = Trainee.builder().firstName("John").build();

        assertThatThrownBy(() -> traineeService.update(input))
                .isInstanceOf(IllegalArgumentException.class);

        verify(traineeDao, never()).update(any());
    }

    @Test
    void delete_shouldDelegateToDao() {
        traineeService.delete(1L);

        verify(traineeDao).deleteById(1L);
    }

    @Test
    void delete_shouldThrow_whenIdIsNull() {
        assertThatThrownBy(() -> traineeService.delete(null))
                .isInstanceOf(IllegalArgumentException.class);

        verify(traineeDao, never()).deleteById(any());
    }

    @Test
    void select_shouldReturnTrainee_whenFound() {
        Trainee trainee = Trainee.builder().userId(1L).username("John.Smith").build();
        when(traineeDao.findById(1L)).thenReturn(Optional.of(trainee));

        Trainee result = traineeService.select(1L);

        assertThat(result).isEqualTo(trainee);
    }

    @Test
    void select_shouldThrow_whenNotFound() {
        when(traineeDao.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> traineeService.select(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void selectAll_shouldReturnAllTrainees() {
        List<Trainee> trainees = List.of(
                Trainee.builder().userId(1L).build(),
                Trainee.builder().userId(2L).build()
        );
        when(traineeDao.findAll()).thenReturn(trainees);

        List<Trainee> result = traineeService.selectAll();

        assertThat(result).hasSize(2).containsExactlyElementsOf(trainees);
    }
}
