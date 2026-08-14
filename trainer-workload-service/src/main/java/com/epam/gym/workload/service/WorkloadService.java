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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class WorkloadService {

    private static final Logger log = LoggerFactory.getLogger(WorkloadService.class);
    private static final String TX_ID = "transactionId";

    private final TrainerWorkloadRepository repository;
    private final TrainerWorkloadMapper mapper;

    public WorkloadService(TrainerWorkloadRepository repository,
                           TrainerWorkloadMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public void processWorkload(WorkloadRequest request) {
        String txId = MDC.get(TX_ID);
        String username = request.getTrainerUsername();
        int year = request.getTrainingDate().getYear();
        int month = request.getTrainingDate().getMonthValue();   // 1-12
        int duration = request.getTrainingDuration();
        ActionType action = request.getActionType();

        log.info("[TxId: {}] Processing workload event [{}] for trainer '{}' (year={}, month={}, duration={})",
                txId, action, username, year, month, duration);

        TrainerWorkloadDocument document = repository.findByTrainerUsername(username)
                .orElse(null);

        if (document == null) {
            log.debug("[TxId: {}] No existing document for '{}', creating new", txId, username);
            document = createNewDocument(request);
        } else {
            document.setTrainerFirstName(request.getTrainerFirstName());
            document.setTrainerLastName(request.getTrainerLastName());
            document.setTrainerStatus(Boolean.TRUE.equals(request.getIsActive()));
        }

        applyAction(document, year, month, duration, action, txId);

        repository.save(document);
        log.info("[TxId: {}] Saved workload document for trainer '{}'", txId, username);
    }

    private TrainerWorkloadDocument createNewDocument(WorkloadRequest request) {
        return TrainerWorkloadDocument.builder()
                .trainerUsername(request.getTrainerUsername())
                .trainerFirstName(request.getTrainerFirstName())
                .trainerLastName(request.getTrainerLastName())
                .trainerStatus(Boolean.TRUE.equals(request.getIsActive()))
                .years(new ArrayList<>())
                .build();
    }

    private void applyAction(TrainerWorkloadDocument document, int year, int month,
                             int duration, ActionType action, String txId) {

        YearSummary yearSummary = findOrCreateYear(document, year, action, txId);
        if (yearSummary == null) {
            log.debug("[TxId: {}] DELETE requested for missing year {}, skipping", txId, year);
            return;
        }

        MonthSummary monthSummary = yearSummary.getMonths().stream()
                .filter(m -> m.getMonth() == month)
                .findFirst()
                .orElse(null);

        if (action == ActionType.ADD) {
            if (monthSummary == null) {
                yearSummary.getMonths().add(
                        MonthSummary.builder().month(month).summaryDuration(duration).build());
                log.debug("[TxId: {}] Added new month {} (year {}) with duration {}", txId, month, year, duration);
            } else {
                long updated = monthSummary.getSummaryDuration() + duration;   // 3d formula
                log.debug("[TxId: {}] Updated month {} (year {}): {} -> {}",
                        txId, month, year, monthSummary.getSummaryDuration(), updated);
                monthSummary.setSummaryDuration(updated);
            }
        } else { // DELETE
            if (monthSummary == null) {
                log.debug("[TxId: {}] DELETE requested for missing month {} (year {}), skipping", txId, month, year);
                return;
            }
            long updated = monthSummary.getSummaryDuration() - duration;
            if (updated <= 0) {
                yearSummary.getMonths().remove(monthSummary);
                log.debug("[TxId: {}] Removed month {} (year {}) — duration dropped to {}", txId, month, year, updated);
            } else {
                monthSummary.setSummaryDuration(updated);
                log.debug("[TxId: {}] Decremented month {} (year {}) to {}", txId, month, year, updated);
            }
            if (yearSummary.getMonths().isEmpty()) {
                document.getYears().remove(yearSummary);
                log.debug("[TxId: {}] Removed empty year {}", txId, year);
            }
        }
    }

    private YearSummary findOrCreateYear(TrainerWorkloadDocument document, int year,
                                         ActionType action, String txId) {
        YearSummary existing = document.getYears().stream()
                .filter(y -> y.getYear() == year)
                .findFirst()
                .orElse(null);

        if (existing != null) {
            return existing;
        }
        if (action == ActionType.DELETE) {
            return null;
        }
        YearSummary created = YearSummary.builder().year(year).months(new ArrayList<>()).build();
        document.getYears().add(created);
        log.debug("[TxId: {}] Added new year {}", txId, year);
        return created;
    }

    /**
     * Returns the aggregated summary for a trainer, mapped to the API response.
     */
    public TrainerWorkloadResponse getSummary(String username) {
        String txId = MDC.get(TX_ID);
        log.info("[TxId: {}] Fetching workload summary for trainer '{}'", txId, username);

        TrainerWorkloadDocument document = repository.findByTrainerUsername(username)
                .orElseThrow(() -> new WorkloadNotFoundException(
                        "Trainer workload not found for: " + username));

        return mapper.toResponse(document);
    }
}