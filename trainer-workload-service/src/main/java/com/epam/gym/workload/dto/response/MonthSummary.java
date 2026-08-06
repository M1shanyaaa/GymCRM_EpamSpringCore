package com.epam.gym.workload.dto.response;

import lombok.Data;

@Data
public class MonthSummary {
    private String month;
    private int trainingSummaryDuration;
}