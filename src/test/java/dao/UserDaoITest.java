package dao;

import com.epam.gym.dao.TraineeDao;
import com.epam.gym.dao.UserDao;
import com.epam.gym.model.Trainee;
import com.epam.gym.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class UserDaoITest extends AbstractDaoITest {

    @Autowired private UserDao userDao;
    @Autowired private TraineeDao traineeDao;

    private void persistTraineeWithUsername(String username) {
        User user = User.builder()
                .firstName("John").lastName("Smith")
                .username(username).password("hashed").isActive(true)
                .build();
        traineeDao.save(Trainee.builder().user(user).build());
    }

    @Test
    void findByUsername_shouldReturnUser() {
        persistTraineeWithUsername("John.Smith");

        assertThat(userDao.findByUsername("John.Smith")).isPresent();
    }

    @Test
    void findByUsername_shouldReturnEmpty_whenMissing() {
        assertThat(userDao.findByUsername("Ghost")).isEmpty();
    }

    @Test
    void existsByUsername_shouldReturnTrue_whenExists() {
        persistTraineeWithUsername("John.Smith");

        assertThat(userDao.existsByUsername("John.Smith")).isTrue();
    }

    @Test
    void existsByUsername_shouldReturnFalse_whenMissing() {
        assertThat(userDao.existsByUsername("Ghost")).isFalse();
    }
}
