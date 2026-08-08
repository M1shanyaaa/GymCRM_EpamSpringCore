package com.epam.gym.workload.service;

import com.epam.gym.workload.dto.ActionType;
import com.epam.gym.workload.dto.WorkloadRequest;
import com.epam.gym.workload.dto.response.MonthSummary;
import com.epam.gym.workload.dto.response.TrainerWorkloadResponse;
import com.epam.gym.workload.exception.WorkloadNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkloadServiceTest {

    private WorkloadService service;

    @BeforeEach
    void setUp() {
        service = new WorkloadService();
    }

    private WorkloadRequest request(ActionType type, LocalDate date, int duration) {
        return new WorkloadRequest(
                "john.doe", "John", "Doe", true, date, duration, type);
    }

    private int durationFor(TrainerWorkloadResponse resp, int year, String month) {
        return resp.getYears().stream()
                .filter(y -> y.getYear() == year)
                .findFirst()
                .flatMap(y -> y.getMonths().stream()
                        .filter(m -> m.getMonth().equals(month))
                        .findFirst())
                .map(MonthSummary::getTrainingSummaryDuration)
                .orElse(0);
    }

    @Test
    void add_createsTrainerAndAccumulatesDuration() {
        service.processWorkload(request(ActionType.ADD, LocalDate.of(2024, 1, 10), 60));

        TrainerWorkloadResponse resp = service.getSummary("john.doe");

        assertThat(resp.getUsername()).isEqualTo("john.doe");
        assertThat(resp.getFirstName()).isEqualTo("John");
        assertThat(resp.getLastName()).isEqualTo("Doe");
        assertThat(resp.isStatus()).isTrue();
        assertThat(durationFor(resp, 2024, "JANUARY")).isEqualTo(60);
    }

    @Test
    void add_sameMonth_sumsDurations() {
        service.processWorkload(request(ActionType.ADD, LocalDate.of(2024, 1, 5), 60));
        service.processWorkload(request(ActionType.ADD, LocalDate.of(2024, 1, 20), 30));

        TrainerWorkloadResponse resp = service.getSummary("john.doe");

        assertThat(durationFor(resp, 2024, "JANUARY")).isEqualTo(90);
    }

    @Test
    void add_differentMonthsAndYears_areSeparated() {
        service.processWorkload(request(ActionType.ADD, LocalDate.of(2024, 1, 5), 60));
        service.processWorkload(request(ActionType.ADD, LocalDate.of(2024, 2, 5), 45));
        service.processWorkload(request(ActionType.ADD, LocalDate.of(2025, 1, 5), 15));

        TrainerWorkloadResponse resp = service.getSummary("john.doe");

        assertThat(resp.getYears()).hasSize(2);
        assertThat(durationFor(resp, 2024, "JANUARY")).isEqualTo(60);
        assertThat(durationFor(resp, 2024, "FEBRUARY")).isEqualTo(45);
        assertThat(durationFor(resp, 2025, "JANUARY")).isEqualTo(15);
    }

    @Test
    void delete_subtractsDuration() {
        service.processWorkload(request(ActionType.ADD, LocalDate.of(2024, 1, 5), 100));
        service.processWorkload(request(ActionType.DELETE, LocalDate.of(2024, 1, 20), 30));

        TrainerWorkloadResponse resp = service.getSummary("john.doe");

        assertThat(durationFor(resp, 2024, "JANUARY")).isEqualTo(70);
    }

    @Test
    void delete_toZero_removesMonth() {
        service.processWorkload(request(ActionType.ADD, LocalDate.of(2024, 1, 5), 60));
        service.processWorkload(request(ActionType.DELETE, LocalDate.of(2024, 1, 20), 60));

        TrainerWorkloadResponse resp = service.getSummary("john.doe");

        assertThat(durationFor(resp, 2024, "JANUARY")).isZero();
    }

    @Test
    void delete_belowZero_doesNotGoNegative_removesMonth() {
        service.processWorkload(request(ActionType.ADD, LocalDate.of(2024, 1, 5), 30));
        service.processWorkload(request(ActionType.DELETE, LocalDate.of(2024, 1, 20), 100));

        TrainerWorkloadResponse resp = service.getSummary("john.doe");

        // month removed because result <= 0
        assertThat(durationFor(resp, 2024, "JANUARY")).isZero();
        assertThat(resp.getYears())
                .allSatisfy(y -> assertThat(y.getMonths())
                        .noneMatch(m -> m.getMonth().equals("JANUARY")));
    }

    @Test
    void delete_onNonExistingMonth_doesNothing() {
        // no ADD before
        service.processWorkload(request(ActionType.DELETE, LocalDate.of(2024, 1, 5), 50));

        TrainerWorkloadResponse resp = service.getSummary("john.doe");
        assertThat(durationFor(resp, 2024, "JANUARY")).isZero();
    }

    @Test
    void processWorkload_updatesStatus() {
        service.processWorkload(request(ActionType.ADD, LocalDate.of(2024, 1, 5), 60));
        // now inactive
        service.processWorkload(new WorkloadRequest(
                "john.doe", "John", "Doe", false,
                LocalDate.of(2024, 2, 5), 30, ActionType.ADD));

        TrainerWorkloadResponse resp = service.getSummary("john.doe");
        assertThat(resp.isStatus()).isFalse();
    }

    @Test
    void getSummary_unknownTrainer_throws() {
        assertThatThrownBy(() -> service.getSummary("ghost"))
                .isInstanceOf(WorkloadNotFoundException.class)
                .hasMessageContaining("ghost");
    }
}