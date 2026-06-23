package com.epam.gym.util;

import com.epam.gym.model.Trainee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Maps a CSV row into a {@link Trainee}.
 * Expected columns:
 * userId, firstName, lastName, username, password, isActive, dateOfBirth, address
 */
@Component
public class TraineeCsvParser {

    private static final Logger log = LoggerFactory.getLogger(TraineeCsvParser.class);

    public Trainee parse(String[] row) {
        try {
            return Trainee.builder()
                    .userId(Long.parseLong(row[0].trim()))
                    .firstName(row[1].trim())
                    .lastName(row[2].trim())
                    .username(row[3].trim())
                    .password(row[4].trim())
                    .isActive(Boolean.parseBoolean(row[5].trim()))
                    .dateOfBirth(LocalDate.parse(row[6].trim()))
                    .address(row[7].trim())
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse trainee row: {}", String.join(",", row), e);
            return null;
        }
    }
}