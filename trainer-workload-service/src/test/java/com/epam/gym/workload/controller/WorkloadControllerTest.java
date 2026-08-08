package com.epam.gym.workload.controller;

import com.epam.gym.workload.dto.ActionType;
import com.epam.gym.workload.dto.WorkloadRequest;
import com.epam.gym.workload.dto.response.TrainerWorkloadResponse;
import com.epam.gym.workload.security.JwtAuthenticationFilter;
import com.epam.gym.workload.service.WorkloadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkloadController.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkloadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkloadService workloadService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void updateWorkload_valid_returns200() throws Exception {
        WorkloadRequest req = new WorkloadRequest(
                "john.doe", "John", "Doe", true,
                LocalDate.of(2024, 1, 10), 60, ActionType.ADD);

        mockMvc.perform(post("/api/workload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(workloadService).processWorkload(any(WorkloadRequest.class));
    }

    @Test
    void updateWorkload_invalidBody_returns400() throws Exception {
        WorkloadRequest req = new WorkloadRequest(
                "", null, "Doe", null, null, -5, null);

        mockMvc.perform(post("/api/workload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(workloadService);
    }

    @Test
    void getSummary_returns200WithBody() throws Exception {
        TrainerWorkloadResponse resp = new TrainerWorkloadResponse();
        resp.setUsername("john.doe");
        resp.setFirstName("John");
        resp.setLastName("Doe");
        resp.setStatus(true);

        when(workloadService.getSummary("john.doe")).thenReturn(resp);

        mockMvc.perform(get("/api/workload/john.doe/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john.doe"))
                .andExpect(jsonPath("$.firstName").value("John"));
    }
}