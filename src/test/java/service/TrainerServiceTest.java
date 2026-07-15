package service;

import com.epam.gym.dao.TrainerDao;
import com.epam.gym.dao.TrainingTypeDao;
import com.epam.gym.dto.response.CredentialsResponse;
import com.epam.gym.dto.response.TrainerProfileResponse;
import com.epam.gym.dto.response.TrainerShortResponse;
import com.epam.gym.exception.AuthenticationException;
import com.epam.gym.exception.EntityNotFoundException;
import com.epam.gym.mapper.TrainerMapper;
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
    @Mock private TrainerMapper trainerMapper;

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

    private TrainerProfileResponse sampleProfile() {
        return new TrainerProfileResponse(
                "Bruce", "Wayne", TrainingTypeName.STRENGTH, true, List.of());
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

        CredentialsResponse result = trainerService.create("Bruce", "Wayne", TrainingTypeName.STRENGTH);

        ArgumentCaptor<Trainer> captor = ArgumentCaptor.forClass(Trainer.class);
        verify(trainerDao).save(captor.capture());
        Trainer saved = captor.getValue();

        assertThat(saved.getUser().getUsername()).isEqualTo("Bruce.Wayne");
        assertThat(saved.getUser().getPassword()).isEqualTo("hashed");
        assertThat(saved.getUser().isActive()).isTrue();
        assertThat(saved.getSpecialization()).isEqualTo(strengthType);

        assertThat(result.username()).isEqualTo("Bruce.Wayne");
        assertThat(result.password()).isEqualTo("rawPass");
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

    // ---------- getProfile ----------

    @Test
    void getProfile_shouldReturnProfile_whenAuthenticated() {
        when(trainerDao.findByUsername("Bruce.Wayne")).thenReturn(Optional.of(trainer));
        when(trainerMapper.toProfile(trainer)).thenReturn(sampleProfile());

        TrainerProfileResponse result = trainerService.getProfile("Bruce.Wayne", "raw");

        assertThat(result.firstName()).isEqualTo("Bruce");
        assertThat(result.specialization()).isEqualTo(TrainingTypeName.STRENGTH);
        verify(authService).authenticate("Bruce.Wayne", "raw");
    }

    @Test
    void getProfile_shouldThrow_whenNotFound() {
        when(trainerDao.findByUsername("Ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainerService.getProfile("Ghost", "raw"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getProfile_shouldThrow_whenAuthFails() {
        doThrow(new AuthenticationException("bad"))
                .when(authService).authenticate("Bruce.Wayne", "wrong");

        assertThatThrownBy(() -> trainerService.getProfile("Bruce.Wayne", "wrong"))
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

    // ---------- update (specialization read-only) ----------

    @Test
    void update_shouldModifyFieldsButNotSpecialization() {
        when(trainerDao.findByUsername("Bruce.Wayne")).thenReturn(Optional.of(trainer));
        when(trainerDao.update(any(Trainer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(trainerMapper.toProfile(any(Trainer.class))).thenReturn(sampleProfile());

        TrainerProfileResponse result = trainerService.update(
                "Bruce.Wayne", "raw", "Bruce", "Banner", false);

        assertThat(trainer.getUser().getLastName()).isEqualTo("Banner");
        assertThat(trainer.getUser().isActive()).isFalse();
        // specialization unchanged
        assertThat(trainer.getSpecialization()).isEqualTo(strengthType);
        assertThat(result).isNotNull();
        verify(authService).authenticate("Bruce.Wayne", "raw");
        // specialization lookup must NOT be triggered on update
        verify(trainingTypeDao, never()).findByName(any());
    }

    @Test
    void update_shouldThrow_whenNamesBlank() {
        assertThatThrownBy(() ->
                trainerService.update("Bruce.Wayne", "raw", "", "", true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- setActive ----------

    @Test
    void setActive_shouldDeactivate() {
        trainer.getUser().setActive(true);
        when(trainerDao.findByUsername("Bruce.Wayne")).thenReturn(Optional.of(trainer));

        trainerService.setActive("Bruce.Wayne", "raw", false);

        assertThat(trainer.getUser().isActive()).isFalse();
        verify(trainerDao).update(trainer);
    }

    @Test
    void setActive_shouldActivate() {
        trainer.getUser().setActive(false);
        when(trainerDao.findByUsername("Bruce.Wayne")).thenReturn(Optional.of(trainer));

        trainerService.setActive("Bruce.Wayne", "raw", true);

        assertThat(trainer.getUser().isActive()).isTrue();
    }

    // ---------- findUnassignedTrainers ----------

    @Test
    void findUnassignedTrainers_shouldReturnList_whenAuthenticated() {
        when(trainerDao.findUnassignedTrainers("John.Smith")).thenReturn(List.of(trainer));
        when(trainerMapper.toShortList(List.of(trainer)))
                .thenReturn(List.of(new TrainerShortResponse(
                        "Bruce.Wayne", "Bruce", "Wayne", TrainingTypeName.STRENGTH)));

        List<TrainerShortResponse> result =
                trainerService.findUnassignedTrainers("John.Smith", "raw");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).username()).isEqualTo("Bruce.Wayne");
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