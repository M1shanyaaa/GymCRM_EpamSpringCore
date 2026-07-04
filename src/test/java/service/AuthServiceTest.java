package service;

import com.epam.gym.dao.UserDao;
import com.epam.gym.exception.AuthenticationException;
import com.epam.gym.model.User;
import com.epam.gym.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserDao userDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("John.Smith")
                .password("hashed")
                .isActive(true)
                .build();
    }

    @Test
    void matches_shouldReturnTrue_whenCredentialsValid() {
        when(userDao.findByUsername("John.Smith")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw", "hashed")).thenReturn(true);

        assertThat(authService.matches("John.Smith", "raw")).isTrue();
    }

    @Test
    void matches_shouldReturnFalse_whenPasswordWrong() {
        when(userDao.findByUsername("John.Smith")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThat(authService.matches("John.Smith", "wrong")).isFalse();
    }

    @Test
    void matches_shouldReturnFalse_whenUserNotFound() {
        when(userDao.findByUsername("Ghost")).thenReturn(Optional.empty());

        assertThat(authService.matches("Ghost", "raw")).isFalse();
    }

    @Test
    void matches_shouldReturnFalse_whenUsernameNull() {
        assertThat(authService.matches(null, "raw")).isFalse();
    }

    @Test
    void matches_shouldReturnFalse_whenPasswordNull() {
        assertThat(authService.matches("John.Smith", null)).isFalse();
    }

    @Test
    void authenticate_shouldPass_whenCredentialsValid() {
        when(userDao.findByUsername("John.Smith")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw", "hashed")).thenReturn(true);

        authService.authenticate("John.Smith", "raw"); // should not throw
    }

    @Test
    void authenticate_shouldThrow_whenCredentialsInvalid() {
        when(userDao.findByUsername("John.Smith")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.authenticate("John.Smith", "bad"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Invalid username or password");
    }
}
