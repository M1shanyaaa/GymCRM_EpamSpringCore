package controller;

import com.epam.gym.controller.TrainingController;
import com.epam.gym.dto.response.TrainingTypeResponse;
import com.epam.gym.exception.AuthenticationException;
import com.epam.gym.exception.EntityNotFoundException;
import com.epam.gym.exception.GlobalExceptionHandler;
import com.epam.gym.filter.TransactionLoggingFilter;
import com.epam.gym.model.TrainingTypeName;
import com.epam.gym.service.TrainingService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TrainingControllerTest {

    @Mock private TrainingService trainingService;

    @InjectMocks private TrainingController trainingController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders
                .standaloneSetup(trainingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .addFilters(new TransactionLoggingFilter())
                .build();
    }

    private Map<String, Object> validAddTrainingBody() {
        return Map.of(
                "username", "John.Smith",
                "password", "raw",
                "traineeUsername", "John.Smith",
                "trainerUsername", "Bruce.Wayne",
                "trainingName", "Strength Session",
                "trainingDate", LocalDate.now().toString(),
                "trainingDuration", 45);
    }

    // ---------- addTraining (@NoAuth, credentials in body) ----------

    @Test
    void addTraining_shouldReturn200_withoutAnyAuthHeaders() throws Exception {
        // no X-Auth-* headers sent at all — endpoint is @NoAuth by design
        mockMvc.perform(post("/api/trainings")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validAddTrainingBody())))
                .andExpect(status().isOk());

        verify(trainingService).addTraining(
                eq("John.Smith"), eq("raw"),
                eq("John.Smith"), eq("Bruce.Wayne"),
                eq("Strength Session"), eq(LocalDate.now()), eq(45));
    }

    @Test
    void addTraining_shouldReturn401_whenServiceRejectsCredentials() throws Exception {
        doThrow(new AuthenticationException("Invalid username or password"))
                .when(trainingService).addTraining(
                        eq("John.Smith"), eq("wrong"),
                        anyString(), anyString(), anyString(), any(), anyInt());

        Map<String, Object> body = new java.util.HashMap<>(validAddTrainingBody());
        body.put("password", "wrong");

        mockMvc.perform(post("/api/trainings")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addTraining_shouldReturn404_whenTraineeOrTrainerNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Trainee not found: Ghost"))
                .when(trainingService).addTraining(
                        anyString(), anyString(),
                        eq("Ghost"), anyString(), anyString(), any(), anyInt());

        Map<String, Object> body = new java.util.HashMap<>(validAddTrainingBody());
        body.put("traineeUsername", "Ghost");

        mockMvc.perform(post("/api/trainings")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void addTraining_shouldReturn400_whenTrainingNameBlank() throws Exception {
        Map<String, Object> body = new java.util.HashMap<>(validAddTrainingBody());
        body.put("trainingName", "");

        mockMvc.perform(post("/api/trainings")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(trainingService);
    }

    @Test
    void addTraining_shouldReturn400_whenDurationMissing() throws Exception {
        Map<String, Object> body = new java.util.HashMap<>(validAddTrainingBody());
        body.remove("trainingDuration");

        mockMvc.perform(post("/api/trainings")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(trainingService);
    }

    // ---------- getTrainingTypes (@NoAuth) ----------

    @Test
    void getTrainingTypes_shouldReturn200WithList_withoutAuthHeaders() throws Exception {
        when(trainingService.getTrainingTypes())
                .thenReturn(List.of(new TrainingTypeResponse(TrainingTypeName.STRENGTH, 1L)));

        mockMvc.perform(get("/api/trainings/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainingType").value("STRENGTH"));
    }
}