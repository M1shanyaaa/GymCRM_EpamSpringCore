package com.epam.gym.workload.integration;

import com.epam.gym.workload.repo.TrainerWorkloadRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "security.jwt.secret=dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtMTIzNA==",
        "security.jwt.expiration=3600000",
        "eureka.client.enabled=false",
})
class WorkloadIntegrationTest {

    private static final String SECRET =
            "dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtMTIzNA==";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TrainerWorkloadRepository repository;

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    private String validToken(String username) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        Date now = new Date();
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + 3600000))
                .signWith(key)
                .compact();
    }

    @Test
    void protectedEndpoint_withoutToken_returns4xx() throws Exception {
        mockMvc.perform(get("/api/workload/john.doe/summary"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void withInvalidToken_returns4xx() throws Exception {
        mockMvc.perform(get("/api/workload/john.doe/summary")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void fullFlow_addWorkload_thenGetSummary_persistsInMongo() throws Exception {
        String token = validToken("service-account");
        String body = """
                {
                  "trainerUsername": "john.doe",
                  "trainerFirstName": "John",
                  "trainerLastName": "Doe",
                  "isActive": true,
                  "trainingDate": "2024-01-10",
                  "trainingDuration": 60,
                  "actionType": "ADD"
                }
                """;

        // ADD -> persisted in embedded Mongo
        mockMvc.perform(post("/api/workload")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // GET summary -> read back from Mongo, month converted to name
        mockMvc.perform(get("/api/workload/john.doe/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john.doe"))
                .andExpect(jsonPath("$.years[0].year").value(2024))
                .andExpect(jsonPath("$.years[0].months[0].month").value("JANUARY"))
                .andExpect(jsonPath("$.years[0].months[0].trainingSummaryDuration").value(60));
    }

    @Test
    void fullFlow_addTwice_sameMonth_accumulatesDuration() throws Exception {
        String token = validToken("service-account");
        String body = """
                {
                  "trainerUsername": "john.doe",
                  "trainerFirstName": "John",
                  "trainerLastName": "Doe",
                  "isActive": true,
                  "trainingDate": "2024-01-10",
                  "trainingDuration": %d,
                  "actionType": "ADD"
                }
                """;

        mockMvc.perform(post("/api/workload")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted(60)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/workload")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted(30)))
                .andExpect(status().isOk());

        // 60 + 30 = 90
        mockMvc.perform(get("/api/workload/john.doe/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.years[0].months[0].trainingSummaryDuration").value(90));
    }

    @Test
    void getSummary_unknownTrainer_returns4xx() throws Exception {
        String token = validToken("service-account");

        mockMvc.perform(get("/api/workload/ghost.user/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is4xxClientError()); // WorkloadNotFoundException -> 404/4xx
    }
}