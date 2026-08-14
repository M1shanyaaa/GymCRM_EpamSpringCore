package com.epam.gym.workload.service;

import com.epam.gym.workload.document.MonthSummary;
import com.epam.gym.workload.document.TrainerWorkloadDocument;
import com.epam.gym.workload.document.YearSummary;
import com.epam.gym.workload.dto.ActionType;
import com.epam.gym.workload.dto.WorkloadRequest;
import com.epam.gym.workload.dto.response.TrainerWorkloadResponse;
import com.epam.gym.workload.exception.WorkloadNotFoundException;
import com.epam.gym.workload.mapper.TrainerWorkloadMapper;
import com.epam.gym.workload.repo.TrainerWorkloadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkloadServiceTest {

    @Mock private TrainerWorkloadRepository repository;
    @Mock private TrainerWorkloadMapper mapper;

    @InjectMocks private WorkloadService service;

    private WorkloadRequest request(ActionType action, LocalDate date, int duration) {
        return new WorkloadRequest(
                "Bruce.Wayne", "Bruce", "Wayne",
                true, date, duration, action);
    }

    private TrainerWorkloadDocument existingDoc(int year, int month, long duration) {
        return TrainerWorkloadDocument.builder()
                .trainerUsername("Bruce.Wayne")
                .trainerFirstName("Bruce")
                .trainerLastName("Wayne")
                .trainerStatus(true)
                .years(new ArrayList<>(List.of(
                        YearSummary.builder()
                                .year(year)
                                .months(new ArrayList<>(List.of(
                                        MonthSummary.builder().month(month).summaryDuration(duration).build())))
                                .build())))
                .build();
    }

    private TrainerWorkloadDocument captureSaved() {
        ArgumentCaptor<TrainerWorkloadDocument> captor =
                ArgumentCaptor.forClass(TrainerWorkloadDocument.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    // ---------- ADD: new trainer ----------

    @Test
    void processWorkload_add_newTrainer_createsDocumentWithYearMonth() {
        when(repository.findByTrainerUsername("Bruce.Wayne")).thenReturn(Optional.empty());

        service.processWorkload(request(ActionType.ADD, LocalDate.of(2024, 1, 15), 60));

        TrainerWorkloadDocument saved = captureSaved();
        assertThat(saved.getTrainerUsername()).isEqualTo("Bruce.Wayne");
        assertThat(saved.getTrainerStatus()).isTrue();
        assertThat(saved.getYears()).hasSize(1);
        YearSummary year = saved.getYears().get(0);
        assertThat(year.getYear()).isEqualTo(2024);
        assertThat(year.getMonths()).hasSize(1);
        assertThat(year.getMonths().get(0).getMonth()).isEqualTo(1);
        assertThat(year.getMonths().get(0).getSummaryDuration()).isEqualTo(60);
    }

    // ---------- ADD: existing month -> sum ----------

    @Test
    void processWorkload_add_existingMonth_accumulatesDuration() {
        when(repository.findByTrainerUsername("Bruce.Wayne"))
                .thenReturn(Optional.of(existingDoc(2024, 1, 60)));

        service.processWorkload(request(ActionType.ADD, LocalDate.of(2024, 1, 20), 30));

        TrainerWorkloadDocument saved = captureSaved();
        assertThat(saved.getYears().get(0).getMonths().get(0).getSummaryDuration())
                .isEqualTo(90); // 60 + 30
    }

    // ---------- ADD: existing year, new month ----------

    @Test
    void processWorkload_add_existingYearNewMonth_addsMonth() {
        when(repository.findByTrainerUsername("Bruce.Wayne"))
                .thenReturn(Optional.of(existingDoc(2024, 1, 60)));

        service.processWorkload(request(ActionType.ADD, LocalDate.of(2024, 2, 5), 45));

        TrainerWorkloadDocument saved = captureSaved();
        assertThat(saved.getYears().get(0).getMonths()).hasSize(2);
    }

    // ---------- ADD: new year ----------

    @Test
    void processWorkload_add_newYear_addsYear() {
        when(repository.findByTrainerUsername("Bruce.Wayne"))
                .thenReturn(Optional.of(existingDoc(2024, 1, 60)));

        service.processWorkload(request(ActionType.ADD, LocalDate.of(2025, 3, 10), 50));

        TrainerWorkloadDocument saved = captureSaved();
        assertThat(saved.getYears()).hasSize(2);
    }

    // ---------- DELETE: partial ----------

    @Test
    void processWorkload_delete_reducesDuration() {
        when(repository.findByTrainerUsername("Bruce.Wayne"))
                .thenReturn(Optional.of(existingDoc(2024, 1, 100)));

        service.processWorkload(request(ActionType.DELETE, LocalDate.of(2024, 1, 20), 30));

        TrainerWorkloadDocument saved = captureSaved();
        assertThat(saved.getYears().get(0).getMonths().get(0).getSummaryDuration())
                .isEqualTo(70); // 100 - 30
    }

    // ---------- DELETE: drops to zero -> month removed, year cleaned ----------

    @Test
    void processWorkload_delete_downToZero_removesMonthAndEmptyYear() {
        when(repository.findByTrainerUsername("Bruce.Wayne"))
                .thenReturn(Optional.of(existingDoc(2024, 1, 30)));

        service.processWorkload(request(ActionType.DELETE, LocalDate.of(2024, 1, 20), 30));

        TrainerWorkloadDocument saved = captureSaved();
        assertThat(saved.getYears()).isEmpty(); // month removed -> year removed
    }

    // ---------- DELETE: non-existing month -> no-op ----------

    @Test
    void processWorkload_delete_missingMonth_savesWithoutChange() {
        when(repository.findByTrainerUsername("Bruce.Wayne"))
                .thenReturn(Optional.of(existingDoc(2024, 1, 60)));

        service.processWorkload(request(ActionType.DELETE, LocalDate.of(2024, 5, 1), 30));

        TrainerWorkloadDocument saved = captureSaved();
        assertThat(saved.getYears().get(0).getMonths().get(0).getSummaryDuration())
                .isEqualTo(60); // unchanged
    }

    // ---------- getSummary ----------

    @Test
    void getSummary_existingTrainer_returnsMappedResponse() {
        TrainerWorkloadDocument doc = existingDoc(2024, 1, 60);
        TrainerWorkloadResponse expected = new TrainerWorkloadResponse();
        when(repository.findByTrainerUsername("Bruce.Wayne")).thenReturn(Optional.of(doc));
        when(mapper.toResponse(doc)).thenReturn(expected);

        TrainerWorkloadResponse result = service.getSummary("Bruce.Wayne");

        assertThat(result).isSameAs(expected);
        verify(mapper).toResponse(doc);
    }

    @Test
    void getSummary_notFound_throws() {
        when(repository.findByTrainerUsername("Ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSummary("Ghost"))
                .isInstanceOf(WorkloadNotFoundException.class)
                .hasMessageContaining("Ghost");

        verifyNoInteractions(mapper);
    }
}