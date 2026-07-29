package com.epam.gym.actuator;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class MemoryHealthIndicator implements HealthIndicator {

    private static final double MIN_FREE_MEMORY_PERCENTAGE = 10.0;

    @Override
    public Health health() {
        long freeMemory = Runtime.getRuntime().freeMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        double freeMemoryPercent = ((double) freeMemory / (double) totalMemory) * 100;

        Health.Builder status = freeMemoryPercent >= MIN_FREE_MEMORY_PERCENTAGE
                ? Health.up()
                : Health.down().withDetail("warning", "Critically low memory!");

        return status
                .withDetail("free_memory_bytes", freeMemory)
                .withDetail("total_memory_bytes", totalMemory)
                .withDetail("free_memory_percent", String.format("%.2f%%", freeMemoryPercent))
                .build();
    }
}