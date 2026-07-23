package controller;

import com.epam.gym.controller.AuthController;
import com.epam.gym.exception.AuthenticationException;
import com.epam.gym.exception.EntityNotFoundException;
import com.epam.gym.exception.GlobalExceptionHandler;
import com.epam.gym.filter.TransactionLoggingFilter;
import com.epam.gym.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .addFilters(new TransactionLoggingFilter())
                .build();
    }

    // ---------- login (POST, credentials in JSON body) ----------

    @Test
    void login_shouldReturn200_whenValid() throws Exception {
        Map<String, String> body = Map.of("username", "John.Smith", "password", "raw");

        mockMvc.perform(post("/api/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        verify(authService).authenticate("John.Smith", "raw");
    }

    @Test
    void login_shouldReturn401_whenInvalid() throws Exception {
        doThrow(new AuthenticationException("Invalid username or password"))
                .when(authService).authenticate("John.Smith", "wrong");

        Map<String, String> body = Map.of("username", "John.Smith", "password", "wrong");

        mockMvc.perform(post("/api/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_shouldReturn400_whenUsernameBlank() throws Exception {
        Map<String, String> body = Map.of("username", "", "password", "raw");

        mockMvc.perform(post("/api/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void login_shouldReturn400_whenPasswordMissing() throws Exception {
        Map<String, String> body = Map.of("username", "John.Smith");

        mockMvc.perform(post("/api/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    // ---------- changePassword (PUT, username in path, not in body) ----------

    @Test
    void changePassword_shouldReturn200_whenValid() throws Exception {
        Map<String, String> body = Map.of("oldPassword", "raw", "newPassword", "newRaw");

        mockMvc.perform(put("/api/users/John.Smith/password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        verify(authService).changePassword("John.Smith", "raw", "newRaw");
    }

    @Test
    void changePassword_shouldReturn401_whenOldPasswordWrong() throws Exception {
        doThrow(new AuthenticationException("Invalid username or password"))
                .when(authService).changePassword("John.Smith", "wrong", "newRaw");

        Map<String, String> body = Map.of("oldPassword", "wrong", "newPassword", "newRaw");

        mockMvc.perform(put("/api/users/John.Smith/password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePassword_shouldReturn404_whenUserNotFound() throws Exception {
        doThrow(new EntityNotFoundException("User not found: Ghost"))
                .when(authService).changePassword("Ghost", "raw", "newRaw");

        Map<String, String> body = Map.of("oldPassword", "raw", "newPassword", "newRaw");

        mockMvc.perform(put("/api/users/Ghost/password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void changePassword_shouldReturn400_whenNewPasswordBlank() throws Exception {
        Map<String, String> body = Map.of("oldPassword", "raw", "newPassword", "");

        mockMvc.perform(put("/api/users/John.Smith/password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void changePassword_shouldReturn400_whenBodyContainsUnknownUsernameField() throws Exception {
        Map<String, String> body = Map.of(
                "username", "John.Smith", "oldPassword", "raw", "newPassword", "newRaw");

        mockMvc.perform(put("/api/users/John.Smith/password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }
}