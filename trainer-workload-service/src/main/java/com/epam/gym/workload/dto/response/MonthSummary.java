package com.epam.gym.workload.dto.response;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class MonthSummary {
    @Min(1)
    @Max(12)
    private String month;
    private int trainingSummaryDuration;
}