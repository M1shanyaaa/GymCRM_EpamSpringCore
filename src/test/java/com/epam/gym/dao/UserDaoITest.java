package com.epam.gym.dao;

import com.epam.gym.dao.impl.TraineeDaoImpl;
import com.epam.gym.dao.impl.UserDaoImpl;
import com.epam.gym.model.Trainee;
import com.epam.gym.model.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TraineeDaoImpl.class, UserDaoImpl.class})
class UserDaoITest {

    @Autowired
    private UserDao userDao;
    @Autowired
    private TraineeDao traineeDao;

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

    // ---------- new tests for countAll ----------

    @Test
    void countAll_shouldReturnTotalNumberOfUsers() {
        persistTraineeWithUsername("User.One");
        persistTraineeWithUsername("User.Two");

        long count = userDao.countAll();

        assertThat(count).isEqualTo(2L);
    }

    @Test
    void countAll_shouldReturnZero_whenNoUsersExist() {
        long count = userDao.countAll();

        assertThat(count).isZero();
    }
}