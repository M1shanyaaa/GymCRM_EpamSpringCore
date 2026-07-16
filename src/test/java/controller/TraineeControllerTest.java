package controller;

import com.epam.gym.controller.TraineeController;
import com.epam.gym.dto.response.*;
import com.epam.gym.exception.EntityNotFoundException;
import com.epam.gym.exception.GlobalExceptionHandler;
import com.epam.gym.model.TrainingTypeName;
import com.epam.gym.service.TraineeService;
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
class TraineeControllerTest {

    @Mock private TraineeService traineeService;
    @Mock private TrainingService trainingService;

    @InjectMocks private TraineeController traineeController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders
                .standaloneSetup(traineeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void register_shouldReturn200() throws Exception {
        when(traineeService.create(eq("John"), eq("Smith"), any(), any()))
                .thenReturn(new CredentialsResponse("John.Smith", "genPass"));

        Map<String, Object> body = Map.of(
                "firstName", "John",
                "lastName", "Smith",
                "dateOfBirth", "1990-01-01",
                "address", "Kyiv");

        mockMvc.perform(post("/api/trainees")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("John.Smith"))
                .andExpect(jsonPath("$.password").value("genPass"));
    }

    @Test
    void register_shouldReturn400_whenFirstNameBlank() throws Exception {
        Map<String, Object> body = Map.of("firstName", "", "lastName", "Smith");

        mockMvc.perform(post("/api/trainees")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(traineeService);
    }

    @Test
    void getProfile_shouldReturnProfile() throws Exception {
        when(traineeService.getProfile("John.Smith"))
                .thenReturn(new TraineeProfileResponse(
                        "John", "Smith", LocalDate.of(1990, 1, 1),
                        "Kyiv", true, List.of()));

        mockMvc.perform(get("/api/trainees/John.Smith")
                        .header("X-Auth-Username", "John.Smith")
                        .header("X-Auth-Password", "raw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void getProfile_shouldReturn404_whenNotFound() throws Exception {
        when(traineeService.getProfile("Ghost"))
                .thenThrow(new EntityNotFoundException("Trainee not found: Ghost"));

        mockMvc.perform(get("/api/trainees/Ghost")
                        .header("X-Auth-Username", "Ghost")
                        .header("X-Auth-Password", "raw"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getProfile_shouldReturn400_whenPasswordHeaderMissing() throws Exception {
        mockMvc.perform(get("/api/trainees/John.Smith"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(traineeService);
    }

    @Test
    void update_shouldReturnUpdatedProfile() throws Exception {
        when(traineeService.update(eq("John.Smith"),
                eq("John"), eq("Doe"), any(), eq("Lviv"), eq(true)))
                .thenReturn(new TraineeProfileResponse(
                        "John", "Doe", LocalDate.of(1990, 1, 1),
                        "Lviv", true, List.of()));

        Map<String, Object> body = Map.of(
                "firstName", "John",
                "lastName", "Doe",
                "dateOfBirth", "1990-01-01",
                "address", "Lviv",
                "isActive", true);

        mockMvc.perform(put("/api/trainees/John.Smith")
                        .header("X-Auth-Username", "John.Smith")
                        .header("X-Auth-Password", "raw")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.address").value("Lviv"));
    }

    @Test
    void delete_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/trainees/John.Smith")
                        .header("X-Auth-Username", "John.Smith")
                        .header("X-Auth-Password", "raw"))
                .andExpect(status().isOk());

        verify(traineeService).delete("John.Smith");
    }

    @Test
    void setActive_shouldReturn200() throws Exception {
        Map<String, Object> body = Map.of("isActive", false);

        mockMvc.perform(patch("/api/trainees/John.Smith/status")
                        .header("X-Auth-Username", "John.Smith")
                        .header("X-Auth-Password", "raw")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        verify(traineeService).setActive("John.Smith", false);
    }

    @Test
    void updateTrainers_shouldReturnList() throws Exception {
        when(trainingService.updateTraineeTrainers(
                eq("John.Smith"), eq(List.of("Bruce.Wayne"))))
                .thenReturn(List.of(new TrainerShortResponse(
                        "Bruce.Wayne", "Bruce", "Wayne", TrainingTypeName.STRENGTH)));

        Map<String, Object> body = Map.of(
                "trainerUsernames", List.of("Bruce.Wayne"));

        mockMvc.perform(put("/api/trainees/John.Smith/trainers")
                        .header("X-Auth-Username", "John.Smith")
                        .header("X-Auth-Password", "raw")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("Bruce.Wayne"));
    }

    @Test
    void updateTrainers_shouldReturn400_whenListEmpty() throws Exception {
        Map<String, Object> body = Map.of("trainerUsernames", List.of());

        mockMvc.perform(put("/api/trainees/John.Smith/trainers")
                        .header("X-Auth-Username", "John.Smith")
                        .header("X-Auth-Password", "raw")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(trainingService);
    }

    @Test
    void getTrainings_shouldReturnList() throws Exception {
        when(trainingService.getTraineeTrainings(
                eq("John.Smith"), any(), any(), any(), any()))
                .thenReturn(List.of(new TrainingResponse(
                        "S", LocalDate.now(),
                        TrainingTypeName.STRENGTH, 45, "Bruce", "John")));

        mockMvc.perform(get("/api/trainees/John.Smith/trainings")
                        .header("X-Auth-Username", "John.Smith")
                        .header("X-Auth-Password", "raw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainingName").value("S"));
    }
}