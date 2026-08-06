package com.epam.gym.workload.dto.response;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class TrainerWorkloadResponse {
    private String username;
    private String firstName;
    private String lastName;
    private boolean status;
    private List<YearSummary> years = new ArrayList<>();
}