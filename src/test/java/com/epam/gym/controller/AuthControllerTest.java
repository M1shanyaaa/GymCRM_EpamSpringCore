package com.epam.gym.controller;

import com.epam.gym.dto.response.JwtResponse;
import com.epam.gym.exception.custom.AuthenticationException;
import com.epam.gym.exception.custom.UserLockedException;
import com.epam.gym.security.JwtService;
import com.epam.gym.security.TokenBlacklistService;
import com.epam.gym.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @MockBean
    private JwtService jwtService; // Виправлення IllegalArgumentDecode

    // ---------- login (POST) ----------

    @Test
    void login_shouldReturn200AndToken_whenValid() throws Exception {
        Map<String, String> body = Map.of("username", "John.Smith", "password", "raw");
        when(authService.login("John.Smith", "raw")).thenReturn(new JwtResponse("mock.jwt.token", 360000L));

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock.jwt.token"));
    }

    @Test
    void login_shouldReturn401_whenInvalid() throws Exception {
        Map<String, String> body = Map.of("username", "John.Smith", "password", "wrong");
        when(authService.login("John.Smith", "wrong"))
                .thenThrow(new AuthenticationException("Invalid username or password"));

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_shouldReturn423_whenBruteForceBlocked() throws Exception {
        Map<String, String> body = Map.of("username", "Locked.User", "password", "raw");
        when(authService.login("Locked.User", "raw"))
                .thenThrow(new UserLockedException("Account is temporarily locked"));

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isLocked()) // 423
                .andExpect(jsonPath("$.message").value("Account is temporarily locked"));
    }

    @Test
    void login_shouldReturn400_whenUsernameBlank() throws Exception {
        Map<String, String> body = Map.of("username", "", "password", "raw");

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ---------- logout (POST) ----------

    @Test
    @WithMockUser
    void logout_shouldReturn200_andBlacklistToken() throws Exception {
        mockMvc.perform(post("/api/logout")
                        .header("Authorization", "Bearer valid.mock.token"))
                .andExpect(status().isOk());

        verify(tokenBlacklistService).blacklistToken("valid.mock.token");
    }

    // ---------- changePassword (PUT) ----------

    @Test
    @WithMockUser(username = "John.Smith")
    void changePassword_shouldReturn200_whenValid() throws Exception {
        Map<String, String> body = Map.of("oldPassword", "raw", "newPassword", "newRaw");

        mockMvc.perform(put("/api/users/John.Smith/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        verify(authService).changePassword("John.Smith", "raw", "newRaw");
    }

    @Test
    void changePassword_shouldReturn401_whenNoTokenProvided() throws Exception {
        Map<String, String> body = Map.of("oldPassword", "raw", "newPassword", "newRaw");

        mockMvc.perform(put("/api/users/John.Smith/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }
}