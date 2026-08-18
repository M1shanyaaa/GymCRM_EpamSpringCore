package com.epam.gym.workload.mapper;

import com.epam.gym.workload.document.MonthSummary;
import com.epam.gym.workload.document.TrainerWorkloadDocument;
import com.epam.gym.workload.document.YearSummary;
import com.epam.gym.workload.dto.response.TrainerWorkloadResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrainerWorkloadMapperTest {

    private final TrainerWorkloadMapper mapper = new TrainerWorkloadMapper();

    @Test
    void toResponse_mapsFieldsAndConvertsMonthToName() {
        TrainerWorkloadDocument doc = TrainerWorkloadDocument.builder()
                .trainerUsername("Bruce.Wayne")
                .trainerFirstName("Bruce")
                .trainerLastName("Wayne")
                .trainerStatus(true)
                .years(List.of(
                        YearSummary.builder()
                                .year(2024)
                                .months(List.of(
                                        MonthSummary.builder().month(1).summaryDuration(90).build()))
                                .build()))
                .build();

        TrainerWorkloadResponse response = mapper.toResponse(doc);

        assertThat(response.getUsername()).isEqualTo("Bruce.Wayne");
        assertThat(response.getFirstName()).isEqualTo("Bruce");
        assertThat(response.getLastName()).isEqualTo("Wayne");
        assertThat(response.isStatus()).isTrue();
        assertThat(response.getYears()).hasSize(1);
        assertThat(response.getYears().get(0).getYear()).isEqualTo(2024);

        var month = response.getYears().get(0).getMonths().get(0);
        assertThat(month.getMonth()).isEqualTo("JANUARY");       // 1 -> JANUARY
        assertThat(month.getTrainingSummaryDuration()).isEqualTo(90);
    }

    @Test
    void toResponse_sortsYearsAndMonthsAscending() {
        TrainerWorkloadDocument doc = TrainerWorkloadDocument.builder()
                .trainerUsername("u").trainerFirstName("f").trainerLastName("l").trainerStatus(true)
                .years(List.of(
                        YearSummary.builder().year(2025)
                                .months(List.of(MonthSummary.builder().month(3).summaryDuration(10).build()))
                                .build(),
                        YearSummary.builder().year(2024)
                                .months(List.of(
                                        MonthSummary.builder().month(5).summaryDuration(10).build(),
                                        MonthSummary.builder().month(2).summaryDuration(10).build()))
                                .build()))
                .build();

        TrainerWorkloadResponse response = mapper.toResponse(doc);

        assertThat(response.getYears().get(0).getYear()).isEqualTo(2024);
        assertThat(response.getYears().get(1).getYear()).isEqualTo(2025);
        assertThat(response.getYears().get(0).getMonths().get(0).getMonth()).isEqualTo("FEBRUARY"); // 2
        assertThat(response.getYears().get(0).getMonths().get(1).getMonth()).isEqualTo("MAY");      // 5
    }

    @Test
    void toResponse_nullStatus_mapsToFalse() {
        TrainerWorkloadDocument doc = TrainerWorkloadDocument.builder()
                .trainerUsername("u").trainerFirstName("f").trainerLastName("l")
                .trainerStatus(null)
                .years(List.of())
                .build();

        assertThat(mapper.toResponse(doc).isStatus()).isFalse();
    }
}