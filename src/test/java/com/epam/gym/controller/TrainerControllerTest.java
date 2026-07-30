package com.epam.gym.controller;

import com.epam.gym.dto.response.*;
import com.epam.gym.model.TrainingTypeName;
import com.epam.gym.security.JwtService;
import com.epam.gym.service.TrainerService;
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

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TrainerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TrainerService trainerService;

    @MockBean
    private TrainingService trainingService;

    @MockBean
    private JwtService jwtService;

    @Test
    void register_shouldReturn200_withoutToken() throws Exception {
        when(trainerService.create("Bruce", "Wayne", TrainingTypeName.STRENGTH))
                .thenReturn(new CredentialsResponse("Bruce.Wayne", "genPass", "mock.jwt"));

        Map<String, Object> body = Map.of(
                "firstName", "Bruce", "lastName", "Wayne", "specialization", "STRENGTH");

        mockMvc.perform(post("/api/trainers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("Bruce.Wayne"))
                .andExpect(jsonPath("$.token").value("mock.jwt"));
    }

    @Test
    @WithMockUser(username = "Bruce.Wayne")
    void getProfile_shouldReturnProfile_whenUserMatches() throws Exception {
        when(trainerService.getProfile("Bruce.Wayne"))
                .thenReturn(new TrainerProfileResponse(
                        "Bruce", "Wayne", TrainingTypeName.STRENGTH, true, List.of()));

        mockMvc.perform(get("/api/trainers/Bruce.Wayne"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialization").value("STRENGTH"));
    }

    @Test
    @WithMockUser(username = "John.Smith")
    void getUnassigned_shouldReturnList_takingUserFromPrincipal() throws Exception {
        when(trainerService.findUnassignedTrainers("John.Smith"))
                .thenReturn(List.of(new TrainerShortResponse(
                        "Bruce.Wayne", "Bruce", "Wayne", TrainingTypeName.STRENGTH)));

        mockMvc.perform(get("/api/trainers/unassigned"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("Bruce.Wayne"));
    }
}