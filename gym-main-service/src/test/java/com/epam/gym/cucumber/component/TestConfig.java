package com.epam.gym.cucumber.component;

import com.epam.gym.messaging.WorkloadMessageProducer;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public WorkloadMessageProducer workloadMessageProducer() {
        return Mockito.mock(WorkloadMessageProducer.class);
    }
}