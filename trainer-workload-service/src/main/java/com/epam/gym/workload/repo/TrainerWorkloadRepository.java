package com.epam.gym.workload.repo;

import com.epam.gym.workload.document.TrainerWorkloadDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainerWorkloadRepository extends MongoRepository<TrainerWorkloadDocument, String> {

    /**
     * Search by username (which is the document _id).
     * Kept as an explicit method for readability per task requirement.
     */
    Optional<TrainerWorkloadDocument> findByTrainerUsername(String trainerUsername);

    /**
     * Uses the compound index (trainerFirstName, trainerLastName) — note 1.
     */
    List<TrainerWorkloadDocument> findByTrainerFirstNameAndTrainerLastName(String firstName, String lastName);
}