package com.epam.gym.util;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reads CSV files from the classpath and returns rows (excluding header).
 */
@Component
public class CsvReader {

    private static final Logger log = LoggerFactory.getLogger(CsvReader.class);

    /**
     * Reads all data rows from a classpath CSV file, skipping the header line.
     *
     * @param classpathLocation e.g. "data/trainees.csv"
     * @return list of rows, each as String[]; empty list if file is missing/empty
     */
    public List<String[]> readAll(String classpathLocation) {
        ClassPathResource resource = new ClassPathResource(classpathLocation);

        if (!resource.exists()) {
            log.warn("CSV file not found on classpath: {}", classpathLocation);
            return Collections.emptyList();
        }

        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReader(reader)) {

            List<String[]> rows = csvReader.readAll();

            if (rows.isEmpty()) {
                log.warn("CSV file is empty: {}", classpathLocation);
                return Collections.emptyList();
            }

            // Skip header row
            List<String[]> dataRows = new ArrayList<>(rows.subList(1, rows.size()));
            log.info("Read {} data rows from {}", dataRows.size(), classpathLocation);
            return dataRows;

        } catch (IOException | CsvException e) {
            log.error("Failed to read CSV file: {}", classpathLocation, e);
            return Collections.emptyList();
        }
    }
}