package com.epam.gym.workload.integration;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "security.jwt.secret=dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtMTIzNA==",
        "security.jwt.expiration=3600000",
        "eureka.client.enabled=false"
})
class WorkloadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String SECRET =
            "dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtMTIzNA==";

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
    void protectedEndpoint_withoutToken_returns401or403() throws Exception {
        mockMvc.perform(get("/api/workload/john.doe/summary"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void fullFlow_addWorkload_thenGetSummary() throws Exception {
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

        // ADD
        mockMvc.perform(post("/api/workload")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // GET summary
        mockMvc.perform(get("/api/workload/john.doe/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john.doe"))
                .andExpect(jsonPath("$.years[0].year").value(2024))
                .andExpect(jsonPath("$.years[0].months[0].month").value("JANUARY"))
                .andExpect(jsonPath("$.years[0].months[0].trainingSummaryDuration").value(60));
    }

    @Test
    void withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/workload/john.doe/summary")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().is4xxClientError());
    }
}