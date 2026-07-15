package controller;

import com.epam.gym.controller.TrainingController;
import com.epam.gym.dto.response.TrainingTypeResponse;
import com.epam.gym.exception.GlobalExceptionHandler;
import com.epam.gym.model.TrainingTypeName;
import com.epam.gym.service.TrainingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
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
        mockMvc = MockMvcBuilders
                .standaloneSetup(trainingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // LocalDate support
    }

    @Test
    void addTraining_shouldReturn200() throws Exception {
        Map<String, Object> body = Map.of(
                "username", "John.Smith",
                "password", "raw",
                "traineeUsername", "John.Smith",
                "trainerUsername", "Bruce.Wayne",
                "trainingName", "Strength Session",
                "trainingDate", "2024-06-01",
                "trainingDuration", 45);

        mockMvc.perform(post("/api/trainings")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        verify(trainingService).addTraining(
                eq("John.Smith"), eq("raw"),
                eq("John.Smith"), eq("Bruce.Wayne"),
                eq("Strength Session"), eq(LocalDate.of(2024, 6, 1)), eq(45));
    }

    @Test
    void addTraining_shouldReturn400_whenNameBlank() throws Exception {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("username", "John.Smith");
        body.put("password", "raw");
        body.put("traineeUsername", "John.Smith");
        body.put("trainerUsername", "Bruce.Wayne");
        body.put("trainingName", "");   // blank -> 400
        body.put("trainingDate", "2024-06-01");
        body.put("trainingDuration", 45);

        mockMvc.perform(post("/api/trainings")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(trainingService);
    }

    @Test
    void getTrainingTypes_shouldReturnList() throws Exception {
        when(trainingService.getTrainingTypes()).thenReturn(
                List.of(new TrainingTypeResponse(TrainingTypeName.STRENGTH, 1L)));

        mockMvc.perform(get("/api/trainings/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainingType").value("STRENGTH"))
                .andExpect(jsonPath("$[0].trainingTypeId").value(1));
    }
}