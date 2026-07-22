package controller;

import com.epam.gym.controller.TrainerController;
import com.epam.gym.dto.response.*;
import com.epam.gym.exception.EntityNotFoundException;
import com.epam.gym.exception.GlobalExceptionHandler;
import com.epam.gym.filter.TransactionLoggingFilter;
import com.epam.gym.model.TrainingTypeName;
import com.epam.gym.service.TrainerService;
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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TrainerControllerTest {

    @Mock private TrainerService trainerService;
    @Mock private TrainingService trainingService;

    @InjectMocks private TrainerController trainerController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders
                .standaloneSetup(trainerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .addFilters(new TransactionLoggingFilter())
                .build();
    }

    @Test
    void register_shouldReturn200WithCredentials() throws Exception {
        when(trainerService.create("Bruce", "Wayne", TrainingTypeName.STRENGTH))
                .thenReturn(new CredentialsResponse("Bruce.Wayne", "genPass"));

        Map<String, Object> body = Map.of(
                "firstName", "Bruce",
                "lastName", "Wayne",
                "specialization", "STRENGTH");

        mockMvc.perform(post("/api/trainers")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("Bruce.Wayne"))
                .andExpect(jsonPath("$.password").value("genPass"));
    }

    @Test
    void register_shouldReturn400_whenFirstNameBlank() throws Exception {
        Map<String, Object> body = Map.of(
                "firstName", "",
                "lastName", "Wayne",
                "specialization", "STRENGTH");

        mockMvc.perform(post("/api/trainers")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(trainerService);
    }

    @Test
    void getProfile_shouldReturnProfile() throws Exception {
        when(trainerService.getProfile("Bruce.Wayne"))
                .thenReturn(new TrainerProfileResponse(
                        "Bruce", "Wayne", TrainingTypeName.STRENGTH, true, List.of()));

        mockMvc.perform(get("/api/trainers/Bruce.Wayne")
                        .header("X-Auth-Username", "Bruce.Wayne")
                        .header("X-Auth-Password", "raw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Bruce"))
                .andExpect(jsonPath("$.specialization").value("STRENGTH"));
    }

    @Test
    void getProfile_shouldReturn404_whenNotFound() throws Exception {
        when(trainerService.getProfile("Ghost"))
                .thenThrow(new EntityNotFoundException("Trainer not found: Ghost"));

        mockMvc.perform(get("/api/trainers/Ghost")
                        .header("X-Auth-Username", "Ghost")
                        .header("X-Auth-Password", "raw"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void update_shouldReturnUpdatedProfile() throws Exception {
        when(trainerService.update(eq("Bruce.Wayne"),
                eq("Bruce"), eq("Banner"), eq(false)))
                .thenReturn(new TrainerProfileResponse(
                        "Bruce", "Banner", TrainingTypeName.STRENGTH, false, List.of()));

        Map<String, Object> body = Map.of(
                "firstName", "Bruce",
                "lastName", "Banner",
                "isActive", false);

        mockMvc.perform(put("/api/trainers/Bruce.Wayne")
                        .header("X-Auth-Username", "Bruce.Wayne")
                        .header("X-Auth-Password", "raw")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Banner"))
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void setActive_shouldReturn200() throws Exception {
        Map<String, Object> body = Map.of("isActive", false);

        mockMvc.perform(patch("/api/trainers/Bruce.Wayne/status")
                        .header("X-Auth-Username", "Bruce.Wayne")
                        .header("X-Auth-Password", "raw")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        verify(trainerService).setActive("Bruce.Wayne", false);
    }

    @Test
    void getUnassigned_shouldReturnList() throws Exception {
        when(trainerService.findUnassignedTrainers("John.Smith"))
                .thenReturn(List.of(new TrainerShortResponse(
                        "Bruce.Wayne", "Bruce", "Wayne", TrainingTypeName.STRENGTH)));

        mockMvc.perform(get("/api/trainers/unassigned")
                        .header("X-Auth-Username", "John.Smith")
                        .header("X-Auth-Password", "raw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("Bruce.Wayne"));
    }

    @Test
    void getTrainings_shouldReturnList() throws Exception {
        when(trainingService.getTrainerTrainings(
                eq("Bruce.Wayne"), any(), any(), any()))
                .thenReturn(List.of(new TrainingResponse(
                        "S", java.time.LocalDate.now(),
                        TrainingTypeName.STRENGTH, 45, "Bruce", "John")));

        mockMvc.perform(get("/api/trainers/Bruce.Wayne/trainings")
                        .header("X-Auth-Username", "Bruce.Wayne")
                        .header("X-Auth-Password", "raw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainingName").value("S"));
    }

    @Test
    void getProfile_shouldReturn400_whenAuthHeaderMissing() throws Exception {
        // missing X-Auth-Password -> MissingRequestHeaderException -> 400
        mockMvc.perform(get("/api/trainers/Bruce.Wayne"))
                .andExpect(status().isBadRequest());
    }
}