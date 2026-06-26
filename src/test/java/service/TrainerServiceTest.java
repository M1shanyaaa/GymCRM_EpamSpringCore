package service;

import com.epam.gym.dao.TrainerDao;
import com.epam.gym.model.Trainer;
import com.epam.gym.service.TrainerService;
import com.epam.gym.util.PasswordGenerator;
import com.epam.gym.util.UsernameGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
class TrainerServiceTest {

    @Mock
    private TrainerDao trainerDao;

    @Mock
    private UsernameGenerator usernameGenerator;

    @Mock
    private PasswordGenerator passwordGenerator;

    @InjectMocks
    private TrainerService trainerService;

    @Test
    void create_shouldGenerateUsernameAndPassword_andSave() {
        Trainer input = Trainer.builder()
                .firstName("Mike")
                .lastName("Jordan")
                .build();

        when(usernameGenerator.generate("Mike", "Jordan")).thenReturn("Mike.Jordan");
        when(passwordGenerator.generate()).thenReturn("Pwd1234567");
        when(trainerDao.save(any(Trainer.class))).thenAnswer(inv -> inv.getArgument(0));

        Trainer result = trainerService.create(input);

        assertThat(result.getUsername()).isEqualTo("Mike.Jordan");
        assertThat(result.getPassword()).isEqualTo("Pwd1234567");

        verify(usernameGenerator).generate("Mike", "Jordan");
        verify(passwordGenerator).generate();
        verify(trainerDao).save(input);
    }

    @Test
    void create_shouldThrow_whenTrainerIsNull() {
        assertThatThrownBy(() -> trainerService.create(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");

        verifyNoInteractions(trainerDao, usernameGenerator, passwordGenerator);
    }

    @Test
    void create_shouldThrow_whenLastNameIsBlank() {
        Trainer input = Trainer.builder().firstName("Mike").lastName("  ").build();

        assertThatThrownBy(() -> trainerService.create(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");

        verifyNoInteractions(trainerDao);
    }

    @Test
    void update_shouldDelegateToDao() {
        Trainer input = Trainer.builder().userId(1L).firstName("Mike").lastName("Jordan").build();
        when(trainerDao.update(input)).thenReturn(input);

        Trainer result = trainerService.update(input);

        assertThat(result).isSameAs(input);
        verify(trainerDao).update(input);
    }

    @Test
    void update_shouldThrow_whenIdIsNull() {
        Trainer input = Trainer.builder().firstName("Mike").build();

        assertThatThrownBy(() -> trainerService.update(input))
                .isInstanceOf(IllegalArgumentException.class);

        verify(trainerDao, never()).update(any());
    }

    @Test
    void select_shouldReturnTrainer_whenFound() {
        Trainer trainer = Trainer.builder().userId(1L).username("Mike.Jordan").build();
        when(trainerDao.findById(1L)).thenReturn(Optional.of(trainer));

        Trainer result = trainerService.select(1L);

        assertThat(result).isEqualTo(trainer);
    }

    @Test
    void select_shouldThrow_whenNotFound() {
        when(trainerDao.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainerService.select(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void selectAll_shouldReturnAllTrainers() {
        List<Trainer> trainers = List.of(
                Trainer.builder().userId(1L).build(),
                Trainer.builder().userId(2L).build()
        );
        when(trainerDao.findAll()).thenReturn(trainers);

        List<Trainer> result = trainerService.selectAll();

        assertThat(result).hasSize(2).containsExactlyElementsOf(trainers);
    }
}
