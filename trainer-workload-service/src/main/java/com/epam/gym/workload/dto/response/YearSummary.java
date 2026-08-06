package com.epam.gym.workload.dto.response;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class YearSummary {
    private int year;
    private List<MonthSummary> months = new ArrayList<>();
}