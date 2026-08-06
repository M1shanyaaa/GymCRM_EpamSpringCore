package com.epam.gym.actuator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryHealthIndicatorTest {

    private final MemoryHealthIndicator indicator = new MemoryHealthIndicator();

    @Test
    void health_shouldReturnStatusWithMemoryDetails() {
        Health health = indicator.health();

        assertThat(health.getStatus()).isIn(Status.UP, Status.DOWN);

        assertThat(health.getDetails()).containsKey("free_memory_bytes");
        assertThat(health.getDetails()).containsKey("total_memory_bytes");
        assertThat(health.getDetails()).containsKey("free_memory_percent");
    }
}