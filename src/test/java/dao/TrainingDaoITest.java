package dao;

import com.epam.gym.dao.TrainingDao;
import com.epam.gym.model.*;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingDaoITest extends AbstractDaoITest {

    @Autowired private TrainingDao trainingDao;

    // Helper: create full graph inside the current test transaction
    private Training persistFullTraining(Session session,
                                         String traineeUsername,
                                         String trainerFirstName,
                                         String traineeFirstName,
                                         LocalDate date,
                                         TrainingTypeName typeName,
                                         String trainingName) {

        TrainingType type = session.createQuery("from TrainingType where trainingTypeName = :name", TrainingType.class)
                .setParameter("name", typeName)
                .uniqueResult();
        if (type == null) {
            type = new TrainingType(typeName);
            session.persist(type);
        }

        Trainee trainee = session.createQuery("from Trainee where user.username = :username", Trainee.class)
                .setParameter("username", traineeUsername)
                .uniqueResult();
        if (trainee == null) {
            User traineeUser = User.builder()
                    .firstName(traineeFirstName).lastName("Smith")
                    .username(traineeUsername).password("hashed").isActive(true)
                    .build();
            trainee = Trainee.builder().user(traineeUser).build();
            session.persist(trainee);
        }

        String trainerUsername = trainerFirstName + ".Wayne";
        Trainer trainer = session.createQuery("from Trainer where user.username = :username", Trainer.class)
                .setParameter("username", trainerUsername)
                .uniqueResult();
        if (trainer == null) {
            User trainerUser = User.builder()
                    .firstName(trainerFirstName).lastName("Wayne")
                    .username(trainerUsername).password("hashed").isActive(true)
                    .build();
            trainer = Trainer.builder()
                    .user(trainerUser).specialization(type).build();
            session.persist(trainer);
        }

        Training training = Training.builder()
                .trainee(trainee)
                .trainer(trainer)
                .trainingName(trainingName)
                .trainingType(type)
                .trainingDate(date)
                .trainingDuration(45)
                .build();
        session.persist(training);

        session.flush();
        return training;
    }

    // ---------- findTraineeTrainings ----------

    @Test
    void findTraineeTrainings_shouldReturnAll_whenNoFilters() {
        Session session = getSession();
        persistFullTraining(session, "John.Smith", "Bruce", "John",
                LocalDate.of(2024, 5, 1), TrainingTypeName.STRENGTH, "A");
        persistFullTraining(session, "John.Smith", "Bruce", "John",
                LocalDate.of(2024, 6, 1), TrainingTypeName.STRENGTH, "B");

        List<Training> result = trainingDao.findTraineeTrainings(
                "John.Smith", null, null, null, null);

        assertThat(result).hasSize(2);
    }

    @Test
    void findTraineeTrainings_shouldFilterByDateRange() {
        Session session = getSession();
        persistFullTraining(session, "John.Smith", "Bruce", "John",
                LocalDate.of(2024, 1, 1), TrainingTypeName.STRENGTH, "Old");
        persistFullTraining(session, "John.Smith", "Bruce", "John",
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
        Session session = getSession();
        persistFullTraining(session, "John.Smith", "Bruce", "John",
                LocalDate.now(), TrainingTypeName.STRENGTH, "S");

        assertThat(trainingDao.findTraineeTrainings(
                "John.Smith", null, null, "Bruce", null)).hasSize(1);
        assertThat(trainingDao.findTraineeTrainings(
                "John.Smith", null, null, "Nobody", null)).isEmpty();
    }

    @Test
    void findTraineeTrainings_shouldFilterByTrainingType() {
        Session session = getSession();
        persistFullTraining(session, "John.Smith", "Bruce", "John",
                LocalDate.now(), TrainingTypeName.STRENGTH, "S");

        assertThat(trainingDao.findTraineeTrainings(
                "John.Smith", null, null, null, TrainingTypeName.STRENGTH)).hasSize(1);
        assertThat(trainingDao.findTraineeTrainings(
                "John.Smith", null, null, null, TrainingTypeName.YOGA)).isEmpty();
    }

    @Test
    void findTraineeTrainings_shouldReturnEmpty_forUnknownTrainee() {
        Session session = getSession();
        persistFullTraining(session, "John.Smith", "Bruce", "John",
                LocalDate.now(), TrainingTypeName.STRENGTH, "S");

        assertThat(trainingDao.findTraineeTrainings(
                "Ghost", null, null, null, null)).isEmpty();
    }

    // ---------- findTrainerTrainings ----------

    @Test
    void findTrainerTrainings_shouldReturnAll_whenNoFilters() {
        Session session = getSession();
        persistFullTraining(session, "John.Smith", "Bruce", "John",
                LocalDate.of(2024, 5, 1), TrainingTypeName.STRENGTH, "A");
        persistFullTraining(session, "John.Smith", "Bruce", "John",
                LocalDate.of(2024, 6, 1), TrainingTypeName.STRENGTH, "B");

        assertThat(trainingDao.findTrainerTrainings(
                "Bruce.Wayne", null, null, null)).hasSize(2);
    }

    @Test
    void findTrainerTrainings_shouldFilterByDateRange() {
        Session session = getSession();
        persistFullTraining(session, "John.Smith", "Bruce", "John",
                LocalDate.of(2024, 1, 1), TrainingTypeName.STRENGTH, "Old");
        persistFullTraining(session, "John.Smith", "Bruce", "John",
                LocalDate.of(2024, 6, 15), TrainingTypeName.STRENGTH, "InRange");

        List<Training> result = trainingDao.findTrainerTrainings(
                "Bruce.Wayne",
                LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 30), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTrainingName()).isEqualTo("InRange");
    }

    @Test
    void findTrainerTrainings_shouldFilterByTraineeName() {
        Session session = getSession();
        persistFullTraining(session, "John.Smith", "Bruce", "John",
                LocalDate.now(), TrainingTypeName.STRENGTH, "S");

        assertThat(trainingDao.findTrainerTrainings(
                "Bruce.Wayne", null, null, "John")).hasSize(1);
        assertThat(trainingDao.findTrainerTrainings(
                "Bruce.Wayne", null, null, "Nobody")).isEmpty();
    }

    @Test
    void findTrainerTrainings_shouldReturnEmpty_forUnknownTrainer() {
        Session session = getSession();
        persistFullTraining(session, "John.Smith", "Bruce", "John",
                LocalDate.now(), TrainingTypeName.STRENGTH, "S");

        assertThat(trainingDao.findTrainerTrainings(
                "Ghost", null, null, null)).isEmpty();
    }
}