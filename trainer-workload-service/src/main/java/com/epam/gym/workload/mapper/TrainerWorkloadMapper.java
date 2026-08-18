package com.epam.gym.workload.mapper;

import com.epam.gym.workload.document.TrainerWorkloadDocument;
import com.epam.gym.workload.dto.response.MonthSummary;
import com.epam.gym.workload.dto.response.TrainerWorkloadResponse;
import com.epam.gym.workload.dto.response.YearSummary;
import org.springframework.stereotype.Component;

import java.time.Month;
import java.util.Comparator;
import java.util.List;

/**
 * Maps the MongoDB {@link TrainerWorkloadDocument} to the API-facing
 * {@link TrainerWorkloadResponse}. Month is converted from numeric (1-12)
 * to its English name to preserve the existing API contract.
 */
@Component
public class TrainerWorkloadMapper {

    public TrainerWorkloadResponse toResponse(TrainerWorkloadDocument document) {
        TrainerWorkloadResponse response = new TrainerWorkloadResponse();
        response.setUsername(document.getTrainerUsername());
        response.setFirstName(document.getTrainerFirstName());
        response.setLastName(document.getTrainerLastName());
        response.setStatus(Boolean.TRUE.equals(document.getTrainerStatus()));

        List<YearSummary> years = document.getYears().stream()
                .sorted(Comparator.comparingInt(com.epam.gym.workload.document.YearSummary::getYear))
                .map(this::toYearSummary)
                .toList();

        response.setYears(years);
        return response;
    }

    private YearSummary toYearSummary(com.epam.gym.workload.document.YearSummary docYear) {
        YearSummary year = new YearSummary();
        year.setYear(docYear.getYear());

        List<MonthSummary> months = docYear.getMonths().stream()
                .sorted(Comparator.comparingInt(com.epam.gym.workload.document.MonthSummary::getMonth))
                .map(this::toMonthSummary)
                .toList();

        year.setMonths(months);
        return year;
    }

    private MonthSummary toMonthSummary(com.epam.gym.workload.document.MonthSummary docMonth) {
        MonthSummary month = new MonthSummary();
        month.setMonth(Month.of(docMonth.getMonth()).name());
        month.setTrainingSummaryDuration((int) docMonth.getSummaryDuration());
        return month;
    }
}