package dao;

import com.epam.gym.dao.impl.TrainingDaoImpl;
import com.epam.gym.model.Training;
import com.epam.gym.model.TrainingType;
import com.epam.gym.model.TrainingTypeName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingDaoImplTest {

    private Map<Long, Training> storage;
    private TrainingDaoImpl dao;

    @BeforeEach
    void setUp() {
        storage = new HashMap<>();
        dao = new TrainingDaoImpl(storage);
    }

    private Training sampleTraining() {
        return Training.builder()
                .traineeId(1L)
                .trainerId(2L)
                .trainingName("Morning Fitness")
                .trainingType(new TrainingType(TrainingTypeName.FITNESS))
                .trainingDate(LocalDate.now())
                .trainingDuration(Duration.ofHours(1))
                .build();
    }

    @Test
    void save_shouldStoreWithIncrementingIds() {
        dao.save(sampleTraining());
        dao.save(sampleTraining());

        assertThat(storage).containsKeys(1L, 2L);
        assertThat(dao.findAll()).hasSize(2);
    }

    @Test
    void findById_shouldReturnTraining_whenExists() {
        dao.save(sampleTraining());

        assertThat(dao.findById(1L))
                .isPresent()
                .get()
                .extracting(Training::getTrainingName)
                .isEqualTo("Morning Fitness");
    }

    @Test
    void findById_shouldReturnEmpty_whenNotExists() {
        assertThat(dao.findById(99L)).isEmpty();
    }

    @Test
    void findAll_shouldReturnEmptyList_whenStorageEmpty() {
        assertThat(dao.findAll()).isEmpty();
    }

    @Test
    void syncIdCounter_shouldContinueFromMaxExistingId() {
        storage.put(2L, sampleTraining());

        dao.syncIdCounter();
        dao.save(sampleTraining());

        assertThat(storage).containsKey(3L);
    }
}
