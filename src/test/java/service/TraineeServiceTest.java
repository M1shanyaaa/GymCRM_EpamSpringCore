package service;

import com.epam.gym.dao.TraineeDao;
import com.epam.gym.exception.AuthenticationException;
import com.epam.gym.exception.EntityNotFoundException;
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

    // ---------- create ----------

    @Test
    void create_shouldGenerateUsernameHashPasswordAndSave() {
        when(usernameGenerator.generate("John", "Smith")).thenReturn("John.Smith");
        when(passwordGenerator.generate()).thenReturn("rawPass");
        when(passwordEncoder.encode("rawPass")).thenReturn("hashed");
        when(traineeDao.save(any(Trainee.class))).thenAnswer(inv -> inv.getArgument(0));

        Trainee result = traineeService.create("John", "Smith",
                LocalDate.of(1990, 1, 1), "Kyiv");

        ArgumentCaptor<Trainee> captor = ArgumentCaptor.forClass(Trainee.class);
        verify(traineeDao).save(captor.capture());
        Trainee saved = captor.getValue();

        assertThat(saved.getUser().getUsername()).isEqualTo("John.Smith");
        assertThat(saved.getUser().getPassword()).isEqualTo("hashed");
        assertThat(saved.getUser().isActive()).isTrue();
        assertThat(saved.getAddress()).isEqualTo("Kyiv");
        assertThat(result).isNotNull();
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

    // ---------- findByUsername ----------

    @Test
    void findByUsername_shouldReturnTrainee_whenAuthenticated() {
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee));

        Trainee result = traineeService.findByUsername("John.Smith", "raw");

        assertThat(result).isSameAs(trainee);
        verify(authService).authenticate("John.Smith", "raw");
    }

    @Test
    void findByUsername_shouldThrow_whenNotFound() {
        when(traineeDao.findByUsername("Ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> traineeService.findByUsername("Ghost", "raw"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findByUsername_shouldThrow_whenAuthFails() {
        doThrow(new AuthenticationException("bad"))
                .when(authService).authenticate("John.Smith", "wrong");

        assertThatThrownBy(() -> traineeService.findByUsername("John.Smith", "wrong"))
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
    void update_shouldModifyFieldsAndSave() {
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee));
        when(traineeDao.update(any(Trainee.class))).thenAnswer(inv -> inv.getArgument(0));

        Trainee result = traineeService.update("John.Smith", "raw",
                "Johnny", "Smithson", LocalDate.of(1985, 5, 5), "Lviv");

        assertThat(result.getUser().getFirstName()).isEqualTo("Johnny");
        assertThat(result.getUser().getLastName()).isEqualTo("Smithson");
        assertThat(result.getAddress()).isEqualTo("Lviv");
        verify(authService).authenticate("John.Smith", "raw");
    }

    @Test
    void update_shouldThrow_whenNamesBlank() {
        assertThatThrownBy(() ->
                traineeService.update("John.Smith", "raw", "", "", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- toggleActive ----------

    @Test
    void toggleActive_shouldFlipStatus_fromActiveToInactive() {
        trainee.getUser().setActive(true);
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee));

        traineeService.toggleActive("John.Smith", "raw");

        assertThat(trainee.getUser().isActive()).isFalse();
        verify(traineeDao).update(trainee);
    }

    @Test
    void toggleActive_shouldFlipStatus_fromInactiveToActive() {
        trainee.getUser().setActive(false);
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee));

        traineeService.toggleActive("John.Smith", "raw");

        assertThat(trainee.getUser().isActive()).isTrue();
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