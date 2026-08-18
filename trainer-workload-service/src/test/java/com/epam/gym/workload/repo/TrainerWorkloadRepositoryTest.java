package com.epam.gym.workload.repo;

import com.epam.gym.workload.document.MonthSummary;
import com.epam.gym.workload.document.TrainerWorkloadDocument;
import com.epam.gym.workload.document.YearSummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
class TrainerWorkloadRepositoryTest {

    @Autowired
    private TrainerWorkloadRepository repository;

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    private TrainerWorkloadDocument doc(String username, String first, String last) {
        return TrainerWorkloadDocument.builder()
                .trainerUsername(username)
                .trainerFirstName(first)
                .trainerLastName(last)
                .trainerStatus(true)
                .years(List.of(YearSummary.builder()
                        .year(2024)
                        .months(List.of(MonthSummary.builder().month(1).summaryDuration(60).build()))
                        .build()))
                .build();
    }

    @Test
    void saveAndFindByUsername() {
        repository.save(doc("Bruce.Wayne", "Bruce", "Wayne"));

        Optional<TrainerWorkloadDocument> found = repository.findByTrainerUsername("Bruce.Wayne");

        assertThat(found).isPresent();
        assertThat(found.get().getTrainerFirstName()).isEqualTo("Bruce");
        assertThat(found.get().getYears().get(0).getMonths().get(0).getSummaryDuration())
                .isEqualTo(60);
    }

    @Test
    void findByUsername_notFound_returnsEmpty() {
        assertThat(repository.findByTrainerUsername("Nobody")).isEmpty();
    }

    @Test
    void findByFirstNameAndLastName_usesNameIndex() {
        repository.save(doc("Bruce.Wayne", "Bruce", "Wayne"));
        repository.save(doc("Clark.Kent", "Clark", "Kent"));

        List<TrainerWorkloadDocument> found =
                repository.findByTrainerFirstNameAndTrainerLastName("Bruce", "Wayne");

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getTrainerUsername()).isEqualTo("Bruce.Wayne");
    }

    @Test
    void save_updatesExistingDocumentById() {
        repository.save(doc("Bruce.Wayne", "Bruce", "Wayne"));

        TrainerWorkloadDocument updated = doc("Bruce.Wayne", "Bruce", "Wayne-Updated");
        repository.save(updated);

        assertThat(repository.findAll()).hasSize(1); // same _id -> upsert, not duplicate
        assertThat(repository.findByTrainerUsername("Bruce.Wayne").get().getTrainerLastName())
                .isEqualTo("Wayne-Updated");
    }
}