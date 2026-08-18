package com.epam.gym.workload.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthSummary {

    /** 1-12 */
    private int month;

    /** Accumulated duration (minutes). Number type per spec; long to avoid overflow. */
    private long summaryDuration;
}