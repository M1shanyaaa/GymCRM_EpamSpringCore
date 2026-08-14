package com.epam.gym.workload.service;

import com.epam.gym.workload.dto.ActionType;
import com.epam.gym.workload.dto.WorkloadRequest;
import com.epam.gym.workload.dto.response.MonthSummary;
import com.epam.gym.workload.dto.response.TrainerWorkloadResponse;
import com.epam.gym.workload.dto.response.YearSummary;
import com.epam.gym.workload.exception.WorkloadNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WorkloadService {

    private final Map<String, TrainerData> storage = new ConcurrentHashMap<>();

    private static class TrainerData {
        private String firstName;
        private String lastName;
        private boolean status;
        private final Map<Integer, Map<String, Integer>> yearlyData = new HashMap<>();
    }

    public void processWorkload(WorkloadRequest request) {
        String username = request.getTrainerUsername();

        TrainerData trainerData = storage.computeIfAbsent(username, k -> new TrainerData());

        int year = request.getTrainingDate().getYear();
        String month = request.getTrainingDate().getMonth().name();
        int duration = request.getTrainingDuration();

        synchronized (trainerData) {
            trainerData.firstName = request.getTrainerFirstName();
            trainerData.lastName = request.getTrainerLastName();
            trainerData.status = Boolean.TRUE.equals(request.getIsActive());

            Map<String, Integer> monthData =
                    trainerData.yearlyData.computeIfAbsent(year, k -> new HashMap<>());

            if (request.getActionType() == ActionType.ADD) {
                monthData.merge(month, duration, Integer::sum);
            } else if (request.getActionType() == ActionType.DELETE) {
                monthData.computeIfPresent(month, (m, current) -> {
                    int newDuration = current - duration;
                    return newDuration <= 0 ? null : newDuration;
                });
                if (monthData.isEmpty()) {
                    trainerData.yearlyData.remove(year);
                }
            }
        }
    }

    public TrainerWorkloadResponse getSummary(String username) {
        TrainerData trainerData = storage.get(username);
        if (trainerData == null) {
            throw new WorkloadNotFoundException("Trainer workload not found for: " + username);
        }

        TrainerWorkloadResponse response = new TrainerWorkloadResponse();
        response.setUsername(username);

        synchronized (trainerData) {
            response.setFirstName(trainerData.firstName);
            response.setLastName(trainerData.lastName);
            response.setStatus(trainerData.status);

            List<YearSummary> yearList = new ArrayList<>();
            for (Map.Entry<Integer, Map<String, Integer>> yearEntry :
                    new TreeMap<>(trainerData.yearlyData).entrySet()) {

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
        }

        return response;
    }
}