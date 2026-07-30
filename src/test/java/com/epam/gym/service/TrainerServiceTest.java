package com.epam.gym.service;

import com.epam.gym.dao.TrainerDao;
import com.epam.gym.dao.TrainingTypeDao;
import com.epam.gym.dto.response.CredentialsResponse;
import com.epam.gym.dto.response.TrainerProfileResponse;
import com.epam.gym.dto.response.TrainerShortResponse;
import com.epam.gym.exception.custom.EntityNotFoundException;
import com.epam.gym.mapper.TrainerMapper;
import com.epam.gym.model.Trainer;
import com.epam.gym.model.TrainingType;
import com.epam.gym.model.TrainingTypeName;
import com.epam.gym.model.User;
import com.epam.gym.security.JwtService;
import com.epam.gym.util.PasswordGenerator;
import com.epam.gym.util.UsernameGenerator;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
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
    private TrainingTypeDao trainingTypeDao;
    @Mock
    private UsernameGenerator usernameGenerator;
    @Mock
    private PasswordGenerator passwordGenerator;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TrainerMapper trainerMapper;
    @Mock
    private JwtService jwtService;

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

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
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("mock.jwt.token");

        CredentialsResponse result = trainerService.create("Bruce", "Wayne", TrainingTypeName.STRENGTH);

        ArgumentCaptor<Trainer> captor = ArgumentCaptor.forClass(Trainer.class);
        verify(trainerDao).save(captor.capture());
        Trainer saved = captor.getValue();

        assertThat(saved.getUser().getUsername()).isEqualTo("Bruce.Wayne");
        assertThat(result.password()).isEqualTo("rawPass");
        assertThat(result.token()).isEqualTo("mock.jwt.token");

        // ПЕРЕВІРКА МЕТРИКИ: реєстрація trainer +1
        assertThat(meterRegistry.counter("gym.trainer.registrations.total").count()).isEqualTo(1.0);
    }

    @Test
    void create_shouldThrow_whenSpecializationNotFound() {
        when(trainingTypeDao.findByName(TrainingTypeName.YOGA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                trainerService.create("Bruce", "Wayne", TrainingTypeName.YOGA))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void create_shouldThrow_whenFirstNameBlank() {
        assertThatThrownBy(() ->
                trainerService.create("", "Wayne", TrainingTypeName.STRENGTH))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_shouldThrow_whenSpecializationNull() {
        assertThatThrownBy(() -> trainerService.create("Bruce", "Wayne", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- getProfile ----------
    @Test
    void getProfile_shouldReturnProfile() {
        when(trainerDao.findByUsername("Bruce.Wayne")).thenReturn(Optional.of(trainer));
        when(trainerMapper.toProfile(trainer)).thenReturn(sampleProfile());

        TrainerProfileResponse result = trainerService.getProfile("Bruce.Wayne");
        assertThat(result.firstName()).isEqualTo("Bruce");
    }

    @Test
    void getProfile_shouldThrow_whenNotFound() {
        when(trainerDao.findByUsername("Ghost")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> trainerService.getProfile("Ghost"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------- update (specialization read-only) ----------
    @Test
    void update_shouldModifyFieldsButNotSpecialization() {
        when(trainerDao.findByUsername("Bruce.Wayne")).thenReturn(Optional.of(trainer));
        when(trainerDao.update(any(Trainer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(trainerMapper.toProfile(any(Trainer.class))).thenReturn(sampleProfile());

        TrainerProfileResponse result = trainerService.update(
                "Bruce.Wayne", "Bruce", "Banner", false);

        assertThat(trainer.getUser().getLastName()).isEqualTo("Banner");
        assertThat(result).isNotNull();
    }

    @Test
    void update_shouldThrow_whenNamesBlank() {
        assertThatThrownBy(() -> trainerService.update("Bruce.Wayne", "", "", true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update_shouldThrow_whenNotFound() {
        when(trainerDao.findByUsername("Ghost")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> trainerService.update("Ghost", "Bruce", "Wayne", true))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------- setActive ----------
    @Test
    void setActive_shouldDeactivate() {
        trainer.getUser().setActive(true);
        when(trainerDao.findByUsername("Bruce.Wayne")).thenReturn(Optional.of(trainer));
        trainerService.setActive("Bruce.Wayne", false);
        assertThat(trainer.getUser().isActive()).isFalse();
    }

    @Test
    void setActive_shouldActivate() {
        trainer.getUser().setActive(false);
        when(trainerDao.findByUsername("Bruce.Wayne")).thenReturn(Optional.of(trainer));
        trainerService.setActive("Bruce.Wayne", true);
        assertThat(trainer.getUser().isActive()).isTrue();
    }

    @Test
    void setActive_shouldThrow_whenNotFound() {
        when(trainerDao.findByUsername("Ghost")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> trainerService.setActive("Ghost", true))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------- findUnassignedTrainers ----------
    @Test
    void findUnassignedTrainers_shouldReturnList() {
        when(trainerDao.findUnassignedTrainers("John.Smith")).thenReturn(List.of(trainer));
        when(trainerMapper.toShortList(List.of(trainer)))
                .thenReturn(List.of(new TrainerShortResponse(
                        "Bruce.Wayne", "Bruce", "Wayne", TrainingTypeName.STRENGTH)));

        List<TrainerShortResponse> result = trainerService.findUnassignedTrainers("John.Smith");
        assertThat(result).hasSize(1);
    }
}