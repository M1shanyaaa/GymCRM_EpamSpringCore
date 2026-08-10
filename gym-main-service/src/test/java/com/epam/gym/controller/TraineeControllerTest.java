package com.epam.gym.controller;

import com.epam.gym.dto.response.*;
import com.epam.gym.messaging.WorkloadMessageProducer;
import com.epam.gym.security.JwtService;
import com.epam.gym.service.TraineeService;
import com.epam.gym.service.TrainingService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TraineeControllerTest {

    @MockBean
    private WorkloadMessageProducer workloadMessageProducer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TraineeService traineeService;

    @MockBean
    private TrainingService trainingService;

    @MockBean
    private JwtService jwtService;

    @Test
    void register_shouldReturn200_withoutToken() throws Exception {
        when(traineeService.create(eq("John"), eq("Smith"), any(), any()))
                .thenReturn(new CredentialsResponse("John.Smith", "genPass", "mock.jwt"));

        Map<String, Object> body = Map.of(
                "firstName", "John", "lastName", "Smith",
                "dateOfBirth", "1990-01-01", "address", "Kyiv");

        mockMvc.perform(post("/api/trainees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("John.Smith"))
                .andExpect(jsonPath("$.token").value("mock.jwt"));
    }

    @Test
    @WithMockUser(username = "John.Smith")
    void getProfile_shouldReturnProfile_whenUserMatches() throws Exception {
        when(traineeService.getProfile("John.Smith"))
                .thenReturn(new TraineeProfileResponse(
                        "John", "Smith", LocalDate.of(1990, 1, 1),
                        "Kyiv", true, List.of()));

        mockMvc.perform(get("/api/trainees/John.Smith"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    @WithMockUser(username = "Hacker.User")
    void getProfile_shouldReturn403_whenUserDoesNotMatch() throws Exception {
        mockMvc.perform(get("/api/trainees/John.Smith"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getProfile_shouldReturn401_whenNoToken() throws Exception {
        mockMvc.perform(get("/api/trainees/John.Smith"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "John.Smith")
    void update_shouldReturnUpdatedProfile() throws Exception {
        when(traineeService.update(eq("John.Smith"), eq("John"), eq("Doe"), any(), eq("Lviv"), eq(true)))
                .thenReturn(new TraineeProfileResponse(
                        "John", "Doe", LocalDate.of(1990, 1, 1), "Lviv", true, List.of()));

        Map<String, Object> body = Map.of(
                "firstName", "John", "lastName", "Doe",
                "dateOfBirth", "1990-01-01", "address", "Lviv", "isActive", true);

        mockMvc.perform(put("/api/trainees/John.Smith")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }
}