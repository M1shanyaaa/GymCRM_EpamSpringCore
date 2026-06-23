package dao;

import com.epam.gym.dao.impl.TrainerDaoImpl;
import com.epam.gym.model.Trainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainerDaoImplTest {

    private Map<Long, Trainer> storage;
    private TrainerDaoImpl dao;

    @BeforeEach
    void setUp() {
        storage = new HashMap<>();
        dao = new TrainerDaoImpl(storage);
    }

    @Test
    void save_shouldAssignIncrementingIds() {
        Trainer t1 = dao.save(Trainer.builder().firstName("A").lastName("A").build());
        Trainer t2 = dao.save(Trainer.builder().firstName("B").lastName("B").build());

        assertThat(t1.getUserId()).isEqualTo(1L);
        assertThat(t2.getUserId()).isEqualTo(2L);
    }

    @Test
    void findById_shouldReturnTrainer_whenExists() {
        Trainer saved = dao.save(Trainer.builder().firstName("Mike").lastName("Jordan").build());

        assertThat(dao.findById(saved.getUserId()))
                .isPresent()
                .get()
                .extracting(Trainer::getFirstName)
                .isEqualTo("Mike");
    }

    @Test
    void findById_shouldReturnEmpty_whenNotExists() {
        assertThat(dao.findById(99L)).isEmpty();
    }

    @Test
    void findAll_shouldReturnAllStored() {
        dao.save(Trainer.builder().firstName("A").lastName("A").build());
        dao.save(Trainer.builder().firstName("B").lastName("B").build());

        assertThat(dao.findAll()).hasSize(2);
    }

    @Test
    void update_shouldReplaceExisting() {
        Trainer saved = dao.save(Trainer.builder().firstName("Mike").lastName("Jordan").build());
        saved.setFirstName("Michael");

        Trainer updated = dao.update(saved);

        assertThat(updated.getFirstName()).isEqualTo("Michael");
        assertThat(dao.findById(saved.getUserId()).orElseThrow().getFirstName())
                .isEqualTo("Michael");
    }

    @Test
    void update_shouldThrow_whenIdNotFound() {
        Trainer notSaved = Trainer.builder().userId(99L).firstName("X").lastName("Y").build();

        assertThatThrownBy(() -> dao.update(notSaved))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void update_shouldThrow_whenIdIsNull() {
        Trainer noId = Trainer.builder().firstName("X").lastName("Y").build();

        assertThatThrownBy(() -> dao.update(noId))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void syncIdCounter_shouldContinueFromMaxExistingId() {
        storage.put(3L, Trainer.builder().userId(3L).firstName("X").lastName("Y").build());

        dao.syncIdCounter();
        Trainer saved = dao.save(Trainer.builder().firstName("New").lastName("One").build());

        assertThat(saved.getUserId()).isEqualTo(4L);
    }
}
