package dao;

import com.epam.gym.dao.TraineeDao;
import com.epam.gym.model.*;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TraineeDaoITest extends AbstractDaoITest {

    @Autowired
    private TraineeDao traineeDao;

    private Trainee newTrainee(String username) {
        User user = User.builder()
                .firstName("John")
                .lastName("Smith")
                .username(username)
                .password("hashed")
                .isActive(true)
                .build();
        return Trainee.builder()
                .user(user)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .address("Kyiv")
                .build();
    }

    @Test
    void save_shouldPersistTraineeAndUser() {
        Trainee saved = traineeDao.save(newTrainee("John.Smith"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser().getId()).isNotNull();
    }

    @Test
    void findByUsername_shouldReturnTrainee() {
        traineeDao.save(newTrainee("John.Smith"));

        Optional<Trainee> found = traineeDao.findByUsername("John.Smith");

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getUsername()).isEqualTo("John.Smith");
        assertThat(found.get().getAddress()).isEqualTo("Kyiv");
    }

    @Test
    void findByUsername_shouldReturnEmpty_whenMissing() {
        assertThat(traineeDao.findByUsername("Ghost")).isEmpty();
    }

    @Test
    void update_shouldModifyExistingTrainee() {
        Trainee saved = traineeDao.save(newTrainee("John.Smith"));
        saved.setAddress("Lviv");
        saved.getUser().setFirstName("Johnny");

        traineeDao.update(saved);

        Trainee reloaded = traineeDao.findByUsername("John.Smith").orElseThrow();
        assertThat(reloaded.getAddress()).isEqualTo("Lviv");
        assertThat(reloaded.getUser().getFirstName()).isEqualTo("Johnny");
    }

    @Test
    void delete_shouldRemoveTraineeAndCascadeUser() {
        Trainee saved = traineeDao.save(newTrainee("John.Smith"));

        traineeDao.delete(saved);

        assertThat(traineeDao.findByUsername("John.Smith")).isEmpty();
    }

    @Test
    void delete_shouldCascadeRemoveUserAndTrainings() {
        Session session = getSession();

        // Arrange: persist full graph
        TrainingType type = new TrainingType(TrainingTypeName.STRENGTH);
        session.persist(type);

        User traineeUser = User.builder()
                .firstName("John").lastName("Smith")
                .username("John.Smith").password("hash").isActive(true).build();
        Trainee trainee = Trainee.builder().user(traineeUser).build();
        session.persist(trainee);

        User trainerUser = User.builder()
                .firstName("Bruce").lastName("Wayne")
                .username("Bruce.Wayne").password("hash").isActive(true).build();
        Trainer trainer = Trainer.builder().user(trainerUser).specialization(type).build();
        session.persist(trainer);

        Training training = Training.builder()
                .trainee(trainee).trainer(trainer)
                .trainingName("S").trainingType(type)
                .trainingDate(LocalDate.now()).trainingDuration(45).build();
        session.persist(training);

        // Flush changes to the DB
        session.flush();

        Long userId = traineeUser.getId();
        Long trainingId = training.getId();
        Long traineeId = trainee.getId();

        // FIX: Clear the first-level cache (memory).
        // This forces Hibernate to fetch the Trainee anew, along with its trainings collection.
        session.clear();

        // Act
        // Load a fresh Trainee object from the database
        Trainee managedTrainee = session.get(Trainee.class, traineeId);

        // Now Hibernate sees the trainings and will generate the correct cascading DELETE queries
        traineeDao.delete(managedTrainee);

        session.flush();
        session.clear();

        // Assert: cascade removed user + training, trainee gone
        assertThat(session.get(Trainee.class, traineeId)).isNull();
        assertThat(session.get(User.class, userId)).isNull();
        assertThat(session.get(Training.class, trainingId)).isNull();

        // trainer + trainerUser MUST survive (not cascaded)
        assertThat(session.get(Trainer.class, trainer.getId())).isNotNull();
    }
}