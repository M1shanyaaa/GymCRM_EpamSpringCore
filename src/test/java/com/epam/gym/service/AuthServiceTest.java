package com.epam.gym.service;

import com.epam.gym.dao.UserDao;
import com.epam.gym.dto.response.JwtResponse;
import com.epam.gym.exception.custom.AuthenticationException;
import com.epam.gym.exception.custom.EntityNotFoundException;
import com.epam.gym.exception.custom.UserLockedException;
import com.epam.gym.model.User;
import com.epam.gym.security.CustomUserDetailsService;
import com.epam.gym.security.JwtProperties;
import com.epam.gym.security.JwtService;
import com.epam.gym.security.LoginAttemptService;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserDao userDao;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private CustomUserDetailsService userDetailsService;
    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private LoginAttemptService loginAttemptService;
    @Mock
    private UserDetails userDetails;

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

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

    // ---------- login ----------

    @Test
    void login_shouldReturnJwt_whenCredentialsValid() {
        when(loginAttemptService.isBlocked("John.Smith")).thenReturn(false);
        when(userDetailsService.loadUserByUsername("John.Smith")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("mock.jwt.token");
        when(jwtProperties.getExpiration()).thenReturn(3600000L);

        JwtResponse response = authService.login("John.Smith", "raw");

        assertThat(response.token()).isEqualTo("mock.jwt.token");

        verify(authenticationManager).authenticate(any());
        verify(loginAttemptService).loginSucceeded("John.Smith");
        assertThat(meterRegistry.counter("gym.auth.login.total", "status", "success").count())
                .isEqualTo(1.0);
    }

    @Test
    void login_shouldThrowUserLockedException_whenUserIsBlocked() {
        when(loginAttemptService.isBlocked("Locked.User")).thenReturn(true);

        assertThatThrownBy(() -> authService.login("Locked.User", "raw"))
                .isInstanceOf(UserLockedException.class)
                .hasMessageContaining("Account is temporarily locked");

        verifyNoInteractions(authenticationManager);
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_shouldThrowAuthenticationException_whenAuthFails() {
        when(loginAttemptService.isBlocked("John.Smith")).thenReturn(false);
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login("John.Smith", "wrong"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Invalid username or password");

        verify(loginAttemptService).loginFailed("John.Smith");
        assertThat(meterRegistry.counter("gym.auth.login.total", "status", "failure").count())
                .isEqualTo(1.0);
    }

    // ---------- changePassword ----------

    @Test
    void changePassword_shouldEncodeAndSave_whenOldPasswordValid() {
        when(userDao.findByUsername("John.Smith")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newRaw")).thenReturn("newHashed");

        authService.changePassword("John.Smith", "raw", "newRaw");

        assertThat(user.getPassword()).isEqualTo("newHashed");
        verify(userDao).update(user);
    }

    @Test
    void changePassword_shouldThrow_whenOldPasswordWrong() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.changePassword("John.Smith", "wrong", "newRaw"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Invalid old password");

        verify(userDao, never()).update(any());
    }

    @Test
    void changePassword_shouldThrow_whenUserNotFound() {
        when(userDao.findByUsername("Ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.changePassword("Ghost", "raw", "newRaw"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found: Ghost");
    }
}