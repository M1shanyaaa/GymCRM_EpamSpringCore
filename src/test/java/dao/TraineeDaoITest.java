package dao;

import com.epam.gym.dao.TraineeDao;
import com.epam.gym.model.Trainee;
import com.epam.gym.model.User;
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

}
