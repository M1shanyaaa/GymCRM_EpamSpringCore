package com.epam.gym.workload.cucumber.integration;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
public class CucumberSpringContextConfig {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", IntegrationTestConfig.MONGO::getReplicaSetUrl);
        registry.add("spring.activemq.broker-url", IntegrationTestConfig::activeMqBrokerUrl);
        registry.add("spring.activemq.user", () -> "admin");
        registry.add("spring.activemq.password", () -> "admin");
        registry.add("gym.messaging.workload-queue", () -> "workload.queue");
        registry.add("gym.messaging.workload-dlq", () -> "workload.dlq");
        registry.add("security.jwt.secret",
                () -> "dGVzdFNlY3JldEtleUZvckp3dEhTMjU2TXVzdEJlTG9uZ0Vub3VnaDEyMzQ1Ng==");
        registry.add("eureka.client.enabled", () -> "false");
    }
}