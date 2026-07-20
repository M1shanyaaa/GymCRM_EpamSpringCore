package service;

import com.epam.gym.dao.TrainerDao;
import com.epam.gym.dao.TrainingTypeDao;
import com.epam.gym.exception.AuthenticationException;
import com.epam.gym.exception.EntityNotFoundException;
import com.epam.gym.model.Trainer;
import com.epam.gym.model.TrainingType;
import com.epam.gym.model.TrainingTypeName;
import com.epam.gym.model.User;
import com.epam.gym.service.AuthService;
import com.epam.gym.service.TrainerService;
import com.epam.gym.util.PasswordGenerator;
import com.epam.gym.util.UsernameGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainerServiceTest {

    @Mock private TrainerDao trainerDao;
    @Mock private TrainingTypeDao trainingTypeDao;
    @Mock private UsernameGenerator usernameGenerator;
    @Mock private PasswordGenerator passwordGenerator;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthService authService;

    @InjectMocks
    private TrainerService trainerService;

    private Trainer trainer;
    private TrainingType strengthType;

    @BeforeEach
    void setUp() {
        strengthType = new TrainingType(TrainingTypeName.STRENGTH);

        User user = User.builder()
                .firstName("Bruce")
                .lastName("Wayne")
                .username("Bruce.Wayne")
                .password("hashed")
                .isActive(true)
                .build();

        trainer = Trainer.builder()
                .user(user)
                .specialization(strengthType)
                .build();
    }

    // ---------- create ----------

    @Test
    void create_shouldResolveSpecializationGenerateCredentialsAndSave() {
        when(trainingTypeDao.findByName(TrainingTypeName.STRENGTH))
                .thenReturn(Optional.of(strengthType));
        when(usernameGenerator.generate("Bruce", "Wayne")).thenReturn("Bruce.Wayne");
        when(passwordGenerator.generate()).thenReturn("rawPass");
        when(passwordEncoder.encode("rawPass")).thenReturn("hashed");
        when(trainerDao.save(any(Trainer.class))).thenAnswer(inv -> inv.getArgument(0));

        Trainer result = trainerService.create("Bruce", "Wayne", TrainingTypeName.STRENGTH);

        ArgumentCaptor<Trainer> captor = ArgumentCaptor.forClass(Trainer.class);
        verify(trainerDao).save(captor.capture());
        Trainer saved = captor.getValue();

        assertThat(saved.getUser().getUsername()).isEqualTo("Bruce.Wayne");
        assertThat(saved.getUser().getPassword()).isEqualTo("hashed");
        assertThat(saved.getUser().isActive()).isTrue();
        assertThat(saved.getSpecialization()).isEqualTo(strengthType);
        assertThat(result).isNotNull();
    }

    @Test
    void create_shouldThrow_whenSpecializationNotFound() {
        when(trainingTypeDao.findByName(TrainingTypeName.YOGA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                trainerService.create("Bruce", "Wayne", TrainingTypeName.YOGA))
                .isInstanceOf(EntityNotFoundException.class);
        verify(trainerDao, never()).save(any());
    }

    @Test
    void create_shouldThrow_whenFirstNameBlank() {
        assertThatThrownBy(() ->
                trainerService.create("", "Wayne", TrainingTypeName.STRENGTH))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(trainerDao);
    }

    @Test
    void create_shouldThrow_whenSpecializationNull() {
        assertThatThrownBy(() ->
                trainerService.create("Bruce", "Wayne", null))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(trainerDao);
    }

    // ---------- findByUsername ----------

    @Test
    void findByUsername_shouldReturnTrainer_whenAuthenticated() {
        when(trainerDao.findByUsername("Bruce.Wayne")).thenReturn(Optional.of(trainer));

        Trainer result = trainerService.findByUsername("Bruce.Wayne", "raw");

        assertThat(result).isSameAs(trainer);
        verify(authService).authenticate("Bruce.Wayne", "raw");
    }

    @Test
    void findByUsername_shouldThrow_whenNotFound() {
        when(trainerDao.findByUsername("Ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainerService.findByUsername("Ghost", "raw"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findByUsername_shouldThrow_whenAuthFails() {
        doThrow(new AuthenticationException("bad"))
                .when(authService).authenticate("Bruce.Wayne", "wrong");

        assertThatThrownBy(() -> trainerService.findByUsername("Bruce.Wayne", "wrong"))
                .isInstanceOf(AuthenticationException.class);
        verify(trainerDao, never()).findByUsername(anyString());
    }

    // ---------- changePassword ----------

    @Test
    void changePassword_shouldEncodeAndUpdate() {
        when(trainerDao.findByUsername("Bruce.Wayne")).thenReturn(Optional.of(trainer));
        when(passwordEncoder.encode("newPass")).thenReturn("newHashed");

        trainerService.changePassword("Bruce.Wayne", "old", "newPass");

        assertThat(trainer.getUser().getPassword()).isEqualTo("newHashed");
        verify(authService).authenticate("Bruce.Wayne", "old");
        verify(trainerDao).update(trainer);
    }

    @Test
    void changePassword_shouldThrow_whenNewPasswordBlank() {
        assertThatThrownBy(() ->
                trainerService.changePassword("Bruce.Wayne", "old", ""))
                .isInstanceOf(IllegalArgumentException.class);
        verify(trainerDao, never()).update(any());
    }

    // ---------- update ----------

    @Test
    void update_shouldModifyFieldsAndSpecialization() {
        TrainingType yoga = new TrainingType(TrainingTypeName.YOGA);
        when(trainerDao.findByUsername("Bruce.Wayne")).thenReturn(Optional.of(trainer));
        when(trainingTypeDao.findByName(TrainingTypeName.YOGA)).thenReturn(Optional.of(yoga));
        when(trainerDao.update(any(Trainer.class))).thenAnswer(inv -> inv.getArgument(0));

        Trainer result = trainerService.update("Bruce.Wayne", "raw",
                "Bruce", "Banner", TrainingTypeName.YOGA);

        assertThat(result.getUser().getLastName()).isEqualTo("Banner");
        assertThat(result.getSpecialization()).isEqualTo(yoga);
        verify(authService).authenticate("Bruce.Wayne", "raw");
    }

    @Test
    void update_shouldThrow_whenSpecializationNotFound() {
        when(trainingTypeDao.findByName(TrainingTypeName.CARDIO)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                trainerService.update("Bruce.Wayne", "raw", "Bruce", "Wayne", TrainingTypeName.CARDIO))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------- toggleActive ----------

    @Test
    void toggleActive_shouldFlipStatus_activeToInactive() {
        trainer.getUser().setActive(true);
        when(trainerDao.findByUsername("Bruce.Wayne")).thenReturn(Optional.of(trainer));

        trainerService.toggleActive("Bruce.Wayne", "raw");

        assertThat(trainer.getUser().isActive()).isFalse();
        verify(trainerDao).update(trainer);
    }

    @Test
    void toggleActive_shouldFlipStatus_inactiveToActive() {
        trainer.getUser().setActive(false);
        when(trainerDao.findByUsername("Bruce.Wayne")).thenReturn(Optional.of(trainer));

        trainerService.toggleActive("Bruce.Wayne", "raw");

        assertThat(trainer.getUser().isActive()).isTrue();
    }

    // ---------- findUnassignedTrainers ----------

    @Test
    void findUnassignedTrainers_shouldReturnList_whenAuthenticated() {
        List<Trainer> unassigned = List.of(trainer);
        when(trainerDao.findUnassignedTrainers("John.Smith")).thenReturn(unassigned);

        List<Trainer> result = trainerService.findUnassignedTrainers("John.Smith", "raw");

        assertThat(result).hasSize(1).containsExactly(trainer);
        verify(authService).authenticate("John.Smith", "raw");
    }

    @Test
    void findUnassignedTrainers_shouldThrow_whenAuthFails() {
        doThrow(new AuthenticationException("bad"))
                .when(authService).authenticate("John.Smith", "wrong");

        assertThatThrownBy(() ->
                trainerService.findUnassignedTrainers("John.Smith", "wrong"))
                .isInstanceOf(AuthenticationException.class);
        verify(trainerDao, never()).findUnassignedTrainers(anyString());
    }
}
