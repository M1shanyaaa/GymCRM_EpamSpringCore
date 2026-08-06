package com.epam.gym.security;

import com.epam.gym.dao.UserDao;
import com.epam.gym.model.Role;
import com.epam.gym.model.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserDao userDao;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Build a dummy user for the successful scenario
        // Note: Change Role.TRAINEE to your actual enum value if it differs
        testUser = User.builder()
                .username("John.Doe")
                .password("hashed_password")
                .role(Role.TRAINEE)
                .isActive(true)
                .build();
    }

    @Test
    void loadUserByUsername_shouldReturnUserDetails_whenUserExists() {
        when(userDao.findByUsername("John.Doe")).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("John.Doe");

        // Verify basic UserDetails properties
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("John.Doe");
        assertThat(userDetails.getPassword()).isEqualTo("hashed_password");

        // Verify that the role was correctly mapped to a SimpleGrantedAuthority
        // and prefixed with "ROLE_"
        assertThat(userDetails.getAuthorities())
                .hasSize(1)
                .extracting("authority")
                .containsExactly("ROLE_TRAINEE");

        verify(userDao).findByUsername("John.Doe");
    }

    @Test
    void loadUserByUsername_shouldThrowException_whenUserNotFound() {
        when(userDao.findByUsername("Ghost")).thenReturn(Optional.empty());

        // Verify that the correct exception is thrown when Optional is empty
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("Ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: Ghost");

        verify(userDao).findByUsername("Ghost");
    }
}