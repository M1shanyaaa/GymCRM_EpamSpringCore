package com.epam.gym.workload.document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregated training summary for a single trainer.
 * Trainer username is used as the document _id (natural key).
 * Structure: Trainer -> Years -> Months -> summaryDuration
 */
@Document(collection = "trainer_workloads")
@CompoundIndex(name = "trainer_name_idx", def = "{'trainerFirstName': 1, 'trainerLastName': 1}")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerWorkloadDocument {

    @Id
    private String trainerUsername;

    @NotBlank
    private String trainerFirstName;

    @NotBlank
    private String trainerLastName;

    @NotNull
    private Boolean trainerStatus;

    @Builder.Default
    private List<YearSummary> years = new ArrayList<>();
}