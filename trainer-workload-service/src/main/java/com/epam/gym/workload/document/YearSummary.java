package com.epam.gym.workload.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YearSummary {

    private int year;

    @Builder.Default
    private List<MonthSummary> months = new ArrayList<>();
}