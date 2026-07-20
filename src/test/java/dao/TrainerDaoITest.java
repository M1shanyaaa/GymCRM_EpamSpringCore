package dao;

import com.epam.gym.dao.TraineeDao;
import com.epam.gym.dao.TrainerDao;
import com.epam.gym.dao.TrainingTypeDao;
import com.epam.gym.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TrainerDaoITest extends AbstractDaoITest {

    @Autowired private TrainerDao trainerDao;
    @Autowired private TraineeDao traineeDao;
    @Autowired private TrainingTypeDao trainingTypeDao;

    private TrainingType persistType(TrainingTypeName name) {
        return trainingTypeDao.save(new TrainingType(name));
    }

    private Trainer newTrainer(String username, TrainingType type) {
        User user = User.builder()
                .firstName("Bruce").lastName("Wayne")
                .username(username).password("hashed").isActive(true)
                .build();
        return Trainer.builder().user(user).specialization(type).build();
    }

    @Test
    void save_shouldPersistTrainer() {
        TrainingType type = persistType(TrainingTypeName.STRENGTH);
        Trainer saved = trainerDao.save(newTrainer("Bruce.Wayne", type));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser().getId()).isNotNull();
    }

    @Test
    void findByUsername_shouldReturnTrainer() {
        TrainingType type = persistType(TrainingTypeName.STRENGTH);
        trainerDao.save(newTrainer("Bruce.Wayne", type));

        Optional<Trainer> found = trainerDao.findByUsername("Bruce.Wayne");

        assertThat(found).isPresent();
        assertThat(found.get().getSpecialization().getTrainingTypeName())
                .isEqualTo(TrainingTypeName.STRENGTH);
    }

    @Test
    void findByUsername_shouldReturnEmpty_whenMissing() {
        assertThat(trainerDao.findByUsername("Ghost")).isEmpty();
    }

    @Test
    void findByUsernames_shouldReturnMatching() {
        TrainingType type = persistType(TrainingTypeName.STRENGTH);
        trainerDao.save(newTrainer("Bruce.Wayne", type));
        trainerDao.save(newTrainer("Clark.Kent", type));

        List<Trainer> result = trainerDao.findByUsernames(
                List.of("Bruce.Wayne", "Clark.Kent"));

        assertThat(result).hasSize(2);
    }

    @Test
    void findByUsernames_shouldReturnEmpty_whenListEmpty() {
        assertThat(trainerDao.findByUsernames(List.of())).isEmpty();
    }

    @Test
    void update_shouldModifyTrainer() {
        TrainingType type = persistType(TrainingTypeName.STRENGTH);
        Trainer saved = trainerDao.save(newTrainer("Bruce.Wayne", type));
        saved.getUser().setLastName("Banner");

        trainerDao.update(saved);

        Trainer reloaded = trainerDao.findByUsername("Bruce.Wayne").orElseThrow();
        assertThat(reloaded.getUser().getLastName()).isEqualTo("Banner");
    }

    @Test
    void findUnassignedTrainers_shouldReturnTrainersNotLinkedToTrainee() {
        TrainingType type = persistType(TrainingTypeName.STRENGTH);
        Trainer assigned = trainerDao.save(newTrainer("Assigned.Trainer", type));
        Trainer free = trainerDao.save(newTrainer("Free.Trainer", type));

        // create trainee linked to 'assigned'
        User traineeUser = User.builder()
                .firstName("John").lastName("Smith")
                .username("John.Smith").password("hashed").isActive(true)
                .build();
        Trainee trainee = Trainee.builder().user(traineeUser).build();
        trainee.getTrainers().add(assigned);
        traineeDao.save(trainee);

        List<Trainer> unassigned = trainerDao.findUnassignedTrainers("John.Smith");

        assertThat(unassigned).extracting(t -> t.getUser().getUsername())
                .contains("Free.Trainer")
                .doesNotContain("Assigned.Trainer");
    }
}
