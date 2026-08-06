package com.epam.gym.controller;

import com.epam.gym.dto.response.TrainingTypeResponse;
import com.epam.gym.model.TrainingTypeName;
import com.epam.gym.security.JwtService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TrainingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TrainingService trainingService;

    @MockBean
    private JwtService jwtService;

    private Map<String, Object> validAddTrainingBody() {
        return Map.of(
                "traineeUsername", "John.Smith",
                "trainerUsername", "Bruce.Wayne",
                "trainingName", "Strength Session",
                "trainingDate", LocalDate.now().plusDays(5).toString(),
                "trainingDuration", 45);
    }

    // ---------- addTraining ----------

    @Test
    @WithMockUser(username = "John.Smith")
    void addTraining_shouldReturn200_withValidToken() throws Exception {
        mockMvc.perform(post("/api/trainings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAddTrainingBody())))
                .andExpect(status().isOk());

        verify(trainingService).addTraining(
                eq("John.Smith"), eq("Bruce.Wayne"),
                eq("Strength Session"), eq(LocalDate.now().plusDays(5)), eq(45));
    }

    @Test
    void addTraining_shouldReturn401_whenNoToken() throws Exception {
        mockMvc.perform(post("/api/trainings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAddTrainingBody())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "John.Smith")
    void addTraining_shouldReturn400_whenDurationMissing() throws Exception {
        Map<String, Object> body = new HashMap<>(validAddTrainingBody());
        body.remove("trainingDuration");

        mockMvc.perform(post("/api/trainings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ---------- getTrainingTypes ----------

    @Test
    void getTrainingTypes_shouldReturn200_withoutAuthHeaders() throws Exception {
        when(trainingService.getTrainingTypes())
                .thenReturn(List.of(new TrainingTypeResponse(TrainingTypeName.STRENGTH, 1L)));

        mockMvc.perform(get("/api/trainings/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainingType").value("STRENGTH"));
    }
}