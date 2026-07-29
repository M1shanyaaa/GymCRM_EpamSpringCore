package com.epam.gym.dao;

import com.epam.gym.dao.impl.TrainingDaoImpl;
import com.epam.gym.model.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TrainingDaoImpl.class)
class TrainingDaoITest {

    @Autowired
    private TrainingDao trainingDao;

    @Autowired
    private TestEntityManager entityManager; // Fully replaces Hibernate Session in tests

    // Helper: create full object graph inside the current test transaction
    private Training persistFullTraining(TestEntityManager entityManager,
                                         String traineeUsername,
                                         String trainerFirstName,
                                         String traineeFirstName,
                                         LocalDate date,
                                         TrainingTypeName typeName,
                                         String trainingName) {

        // 1. Find or create TrainingType
        List<TrainingType> types = entityManager.getEntityManager()
                .createQuery("from TrainingType where trainingTypeName = :name", TrainingType.class)
                .setParameter("name", typeName)
                .getResultList();

        TrainingType type = types.isEmpty() ? null : types.get(0);
        if (type == null) {
            type = new TrainingType(typeName);
            entityManager.persist(type);
        }

        // 2. Find or create Trainee
        List<Trainee> trainees = entityManager.getEntityManager()
                .createQuery("from Trainee where user.username = :username", Trainee.class)
                .setParameter("username", traineeUsername)
                .getResultList();

        Trainee trainee = trainees.isEmpty() ? null : trainees.get(0);
        if (trainee == null) {
            User traineeUser = User.builder()
                    .firstName(traineeFirstName).lastName("Smith")
                    .username(traineeUsername).password("hashed").isActive(true)
                    .build();
            trainee = Trainee.builder().user(traineeUser).build();
            entityManager.persist(trainee);
        }

        // 3. Find or create Trainer
        String trainerUsername = trainerFirstName + ".Wayne";
        List<Trainer> trainers = entityManager.getEntityManager()
                .createQuery("from Trainer where user.username = :username", Trainer.class)
                .setParameter("username", trainerUsername)
                .getResultList();

        Trainer trainer = trainers.isEmpty() ? null : trainers.get(0);
        if (trainer == null) {
            User trainerUser = User.builder()
                    .firstName(trainerFirstName).lastName("Wayne")
                    .username(trainerUsername).password("hashed").isActive(true)
                    .build();
            trainer = Trainer.builder()
                    .user(trainerUser).specialization(type).build();
            entityManager.persist(trainer);
        }

        // 4. Create the training session itself
        Training training = Training.builder()
                .trainee(trainee)
                .trainer(trainer)
                .trainingName(trainingName)
                .trainingType(type)
                .trainingDate(date)
                .trainingDuration(45)
                .build();

        entityManager.persist(training);
        entityManager.flush(); // Force flush to the database

        return training;
    }

    // ---------- findTraineeTrainings ----------

    @Test
    void findTraineeTrainings_shouldReturnAll_whenNoFilters() {
        persistFullTraining(entityManager, "John.Smith", "Bruce", "John",
                LocalDate.of(2024, 5, 1), TrainingTypeName.STRENGTH, "A");
        persistFullTraining(entityManager, "John.Smith", "Bruce", "John",
                LocalDate.of(2024, 6, 1), TrainingTypeName.STRENGTH, "B");

        List<Training> result = trainingDao.findTraineeTrainings(
                "John.Smith", null, null, null, null);

        assertThat(result).hasSize(2);
    }

    @Test
    void findTraineeTrainings_shouldFilterByDateRange() {
        persistFullTraining(entityManager, "John.Smith", "Bruce", "John",
                LocalDate.of(2024, 1, 1), TrainingTypeName.STRENGTH, "Old");
        persistFullTraining(entityManager, "John.Smith", "Bruce", "John",
                LocalDate.of(2024, 6, 15), TrainingTypeName.STRENGTH, "InRange");

        List<Training> result = trainingDao.findTraineeTrainings(
                "John.Smith",
                LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 30),
                null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTrainingName()).isEqualTo("InRange");
    }

    @Test
    void findTraineeTrainings_shouldFilterByTrainerName() {
        persistFullTraining(entityManager, "John.Smith", "Bruce", "John",
                LocalDate.now(), TrainingTypeName.STRENGTH, "S");

        assertThat(trainingDao.findTraineeTrainings(
                "John.Smith", null, null, "Bruce", null)).hasSize(1);
        assertThat(trainingDao.findTraineeTrainings(
                "John.Smith", null, null, "Nobody", null)).isEmpty();
    }

    @Test
    void findTraineeTrainings_shouldFilterByTrainingType() {
        persistFullTraining(entityManager, "John.Smith", "Bruce", "John",
                LocalDate.now(), TrainingTypeName.STRENGTH, "S");

        assertThat(trainingDao.findTraineeTrainings(
                "John.Smith", null, null, null, TrainingTypeName.STRENGTH)).hasSize(1);
        assertThat(trainingDao.findTraineeTrainings(
                "John.Smith", null, null, null, TrainingTypeName.YOGA)).isEmpty();
    }

    @Test
    void findTraineeTrainings_shouldReturnEmpty_forUnknownTrainee() {
        persistFullTraining(entityManager, "John.Smith", "Bruce", "John",
                LocalDate.now(), TrainingTypeName.STRENGTH, "S");

        assertThat(trainingDao.findTraineeTrainings(
                "Ghost", null, null, null, null)).isEmpty();
    }

    // ---------- findTrainerTrainings ----------

    @Test
    void findTrainerTrainings_shouldReturnAll_whenNoFilters() {
        persistFullTraining(entityManager, "John.Smith", "Bruce", "John",
                LocalDate.of(2024, 5, 1), TrainingTypeName.STRENGTH, "A");
        persistFullTraining(entityManager, "John.Smith", "Bruce", "John",
                LocalDate.of(2024, 6, 1), TrainingTypeName.STRENGTH, "B");

        assertThat(trainingDao.findTrainerTrainings(
                "Bruce.Wayne", null, null, null)).hasSize(2);
    }

    @Test
    void findTrainerTrainings_shouldFilterByDateRange() {
        persistFullTraining(entityManager, "John.Smith", "Bruce", "John",
                LocalDate.of(2024, 1, 1), TrainingTypeName.STRENGTH, "Old");
        persistFullTraining(entityManager, "John.Smith", "Bruce", "John",
                LocalDate.of(2024, 6, 15), TrainingTypeName.STRENGTH, "InRange");

        List<Training> result = trainingDao.findTrainerTrainings(
                "Bruce.Wayne",
                LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 30), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTrainingName()).isEqualTo("InRange");
    }

    @Test
    void findTrainerTrainings_shouldFilterByTraineeName() {
        persistFullTraining(entityManager, "John.Smith", "Bruce", "John",
                LocalDate.now(), TrainingTypeName.STRENGTH, "S");

        assertThat(trainingDao.findTrainerTrainings(
                "Bruce.Wayne", null, null, "John")).hasSize(1);
        assertThat(trainingDao.findTrainerTrainings(
                "Bruce.Wayne", null, null, "Nobody")).isEmpty();
    }

    @Test
    void findTrainerTrainings_shouldReturnEmpty_forUnknownTrainer() {
        persistFullTraining(entityManager, "John.Smith", "Bruce", "John",
                LocalDate.now(), TrainingTypeName.STRENGTH, "S");

        assertThat(trainingDao.findTrainerTrainings(
                "Ghost", null, null, null)).isEmpty();
    }
}