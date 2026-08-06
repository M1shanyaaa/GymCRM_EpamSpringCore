package com.epam.gym.workload.service;

import com.epam.gym.workload.dto.ActionType;
import com.epam.gym.workload.dto.response.MonthSummary;
import com.epam.gym.workload.dto.response.TrainerWorkloadResponse;
import com.epam.gym.workload.dto.WorkloadRequest;
import com.epam.gym.workload.dto.response.YearSummary;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WorkloadService {

    // In-memory database
    private final Map<String, TrainerData> storage = new ConcurrentHashMap<>();

    @Data
    private static class TrainerData {
        private String firstName;
        private String lastName;
        private boolean status;
        // Map<Year, Map<Month, Duration>>
        private Map<Integer, Map<String, Integer>> yearlyData = new ConcurrentHashMap<>();
    }

    public void processWorkload(WorkloadRequest request) {
        String username = request.getTrainerUsername();
        int year = request.getTrainingDate().getYear();
        String month = request.getTrainingDate().getMonth().name();
        int duration = request.getTrainingDuration();

        TrainerData trainerData = storage.computeIfAbsent(username, k -> {
            TrainerData td = new TrainerData();
            td.setFirstName(request.getTrainerFirstName());
            td.setLastName(request.getTrainerLastName());
            return td;
        });

        // Always update status to keep it fresh
        trainerData.setStatus(request.getIsActive());

        Map<String, Integer> monthData = trainerData.getYearlyData().computeIfAbsent(year, k -> new ConcurrentHashMap<>());

        if (request.getActionType() == ActionType.ADD) {
            monthData.merge(month, duration, Integer::sum);
        } else if (request.getActionType() == ActionType.DELETE) {
            monthData.computeIfPresent(month, (m, currentDuration) -> {
                int newDuration = currentDuration - duration;
                if (newDuration <= 0) {
                    return null; // Removes the month if duration drops to 0 or below
                }
                return newDuration;
            });
        }
    }

    public TrainerWorkloadResponse getSummary(String username) {
        TrainerData trainerData = storage.get(username);
        if (trainerData == null) {
            throw new IllegalArgumentException("Trainer workload not found for: " + username);
        }

        TrainerWorkloadResponse response = new TrainerWorkloadResponse();
        response.setUsername(username);
        response.setFirstName(trainerData.getFirstName());
        response.setLastName(trainerData.getLastName());
        response.setStatus(trainerData.isStatus());

        List<YearSummary> yearList = new ArrayList<>();

        for (Map.Entry<Integer, Map<String, Integer>> yearEntry : trainerData.getYearlyData().entrySet()) {
            YearSummary ys = new YearSummary();
            ys.setYear(yearEntry.getKey());

            List<MonthSummary> monthList = new ArrayList<>();
            for (Map.Entry<String, Integer> monthEntry : yearEntry.getValue().entrySet()) {
                MonthSummary ms = new MonthSummary();
                ms.setMonth(monthEntry.getKey());
                ms.setTrainingSummaryDuration(monthEntry.getValue());
                monthList.add(ms);
            }

            ys.setMonths(monthList);
            yearList.add(ys);
        }

        response.setYears(yearList);
        return response;
    }
}