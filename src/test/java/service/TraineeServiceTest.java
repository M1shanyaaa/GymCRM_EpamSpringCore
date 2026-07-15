package service;

import com.epam.gym.dao.TraineeDao;
import com.epam.gym.dto.response.CredentialsResponse;
import com.epam.gym.dto.response.TraineeProfileResponse;
import com.epam.gym.exception.AuthenticationException;
import com.epam.gym.exception.EntityNotFoundException;
import com.epam.gym.mapper.TraineeMapper;
import com.epam.gym.model.Trainee;
import com.epam.gym.model.User;
import com.epam.gym.service.AuthService;
import com.epam.gym.service.TraineeService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TraineeServiceTest {

    @Mock private TraineeDao traineeDao;
    @Mock private UsernameGenerator usernameGenerator;
    @Mock private PasswordGenerator passwordGenerator;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthService authService;
    @Mock private TraineeMapper traineeMapper;

    @InjectMocks
    private TraineeService traineeService;

    private Trainee trainee;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .firstName("John")
                .lastName("Smith")
                .username("John.Smith")
                .password("hashed")
                .isActive(true)
                .build();

        trainee = Trainee.builder()
                .user(user)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .address("Kyiv")
                .build();
    }

    private TraineeProfileResponse sampleProfile() {
        return new TraineeProfileResponse(
                "John", "Smith", LocalDate.of(1990, 1, 1), "Kyiv", true, List.of());
    }

    // ---------- create ----------

    @Test
    void create_shouldGenerateUsernameHashPasswordAndReturnCredentials() {
        when(usernameGenerator.generate("John", "Smith")).thenReturn("John.Smith");
        when(passwordGenerator.generate()).thenReturn("rawPass");
        when(passwordEncoder.encode("rawPass")).thenReturn("hashed");
        when(traineeDao.save(any(Trainee.class))).thenAnswer(inv -> inv.getArgument(0));

        CredentialsResponse result = traineeService.create("John", "Smith",
                LocalDate.of(1990, 1, 1), "Kyiv");

        ArgumentCaptor<Trainee> captor = ArgumentCaptor.forClass(Trainee.class);
        verify(traineeDao).save(captor.capture());
        Trainee saved = captor.getValue();

        assertThat(saved.getUser().getUsername()).isEqualTo("John.Smith");
        assertThat(saved.getUser().getPassword()).isEqualTo("hashed");
        assertThat(saved.getUser().isActive()).isTrue();
        assertThat(saved.getAddress()).isEqualTo("Kyiv");

        // credentials response returns raw password (not the hash)
        assertThat(result.username()).isEqualTo("John.Smith");
        assertThat(result.password()).isEqualTo("rawPass");
    }

    @Test
    void create_shouldThrow_whenFirstNameBlank() {
        assertThatThrownBy(() -> traineeService.create("", "Smith", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(traineeDao);
    }

    @Test
    void create_shouldThrow_whenLastNameBlank() {
        assertThatThrownBy(() -> traineeService.create("John", " ", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(traineeDao);
    }

    // ---------- getProfile ----------

    @Test
    void getProfile_shouldReturnProfile_whenAuthenticated() {
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee));
        when(traineeMapper.toProfile(trainee)).thenReturn(sampleProfile());

        TraineeProfileResponse result = traineeService.getProfile("John.Smith", "raw");

        assertThat(result.firstName()).isEqualTo("John");
        assertThat(result.lastName()).isEqualTo("Smith");
        assertThat(result.isActive()).isTrue();
        verify(authService).authenticate("John.Smith", "raw");
    }

    @Test
    void getProfile_shouldThrow_whenNotFound() {
        when(traineeDao.findByUsername("Ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> traineeService.getProfile("Ghost", "raw"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getProfile_shouldThrow_whenAuthFails() {
        doThrow(new AuthenticationException("bad"))
                .when(authService).authenticate("John.Smith", "wrong");

        assertThatThrownBy(() -> traineeService.getProfile("John.Smith", "wrong"))
                .isInstanceOf(AuthenticationException.class);
        verify(traineeDao, never()).findByUsername(anyString());
    }

    // ---------- changePassword ----------

    @Test
    void changePassword_shouldEncodeAndUpdate() {
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee));
        when(passwordEncoder.encode("newPass")).thenReturn("newHashed");

        traineeService.changePassword("John.Smith", "old", "newPass");

        assertThat(trainee.getUser().getPassword()).isEqualTo("newHashed");
        verify(authService).authenticate("John.Smith", "old");
        verify(traineeDao).update(trainee);
    }

    @Test
    void changePassword_shouldThrow_whenNewPasswordBlank() {
        assertThatThrownBy(() ->
                traineeService.changePassword("John.Smith", "old", " "))
                .isInstanceOf(IllegalArgumentException.class);
        verify(traineeDao, never()).update(any());
    }

    // ---------- update ----------

    @Test
    void update_shouldModifyFieldsAndReturnProfile() {
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee));
        when(traineeDao.update(any(Trainee.class))).thenAnswer(inv -> inv.getArgument(0));
        when(traineeMapper.toProfile(any(Trainee.class))).thenReturn(sampleProfile());

        TraineeProfileResponse result = traineeService.update("John.Smith", "raw",
                "Johnny", "Smithson", LocalDate.of(1985, 5, 5), "Lviv", false);

        // entity was mutated correctly
        assertThat(trainee.getUser().getFirstName()).isEqualTo("Johnny");
        assertThat(trainee.getUser().getLastName()).isEqualTo("Smithson");
        assertThat(trainee.getUser().isActive()).isFalse();
        assertThat(trainee.getAddress()).isEqualTo("Lviv");
        assertThat(trainee.getDateOfBirth()).isEqualTo(LocalDate.of(1985, 5, 5));

        assertThat(result).isNotNull();
        verify(authService).authenticate("John.Smith", "raw");
    }

    @Test
    void update_shouldThrow_whenNamesBlank() {
        assertThatThrownBy(() ->
                traineeService.update("John.Smith", "raw", "", "", null, null, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- setActive ----------

    @Test
    void setActive_shouldDeactivate() {
        trainee.getUser().setActive(true);
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee));

        traineeService.setActive("John.Smith", "raw", false);

        assertThat(trainee.getUser().isActive()).isFalse();
        verify(authService).authenticate("John.Smith", "raw");
        verify(traineeDao).update(trainee);
    }

    @Test
    void setActive_shouldActivate() {
        trainee.getUser().setActive(false);
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee));

        traineeService.setActive("John.Smith", "raw", true);

        assertThat(trainee.getUser().isActive()).isTrue();
        verify(traineeDao).update(trainee);
    }

    // ---------- delete ----------

    @Test
    void delete_shouldRemoveTrainee_whenAuthenticated() {
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee));

        traineeService.delete("John.Smith", "raw");

        verify(authService).authenticate("John.Smith", "raw");
        verify(traineeDao).delete(trainee);
    }

    @Test
    void delete_shouldThrow_whenNotFound() {
        when(traineeDao.findByUsername("Ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> traineeService.delete("Ghost", "raw"))
                .isInstanceOf(EntityNotFoundException.class);
        verify(traineeDao, never()).delete(any());
    }
}