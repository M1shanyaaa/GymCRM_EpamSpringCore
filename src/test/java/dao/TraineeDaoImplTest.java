package dao;

import com.epam.gym.dao.impl.TraineeDaoImpl;
import com.epam.gym.model.Trainee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TraineeDaoImplTest {

    private Map<Long, Trainee> storage;
    private TraineeDaoImpl dao;

    @BeforeEach
    void setUp() {
        storage = new HashMap<>();
        dao = new TraineeDaoImpl(storage);
    }

    @Test
    void save_shouldAssignIncrementingIds() {
        Trainee t1 = dao.save(Trainee.builder().firstName("A").lastName("A").build());
        Trainee t2 = dao.save(Trainee.builder().firstName("B").lastName("B").build());

        assertThat(t1.getUserId()).isEqualTo(1L);
        assertThat(t2.getUserId()).isEqualTo(2L);
    }

    @Test
    void findById_shouldReturnSaved() {
        Trainee saved = dao.save(Trainee.builder().firstName("John").lastName("Smith").build());
        assertThat(dao.findById(saved.getUserId())).isPresent();
    }

    @Test
    void findById_shouldReturnEmpty_whenMissing() {
        assertThat(dao.findById(99L)).isEmpty();
    }

    @Test
    void findAll_shouldReturnAll() {
        dao.save(Trainee.builder().firstName("A").lastName("A").build());
        dao.save(Trainee.builder().firstName("B").lastName("B").build());
        assertThat(dao.findAll()).hasSize(2);
    }

    @Test
    void update_shouldModifyExisting() {
        Trainee saved = dao.save(Trainee.builder().firstName("John").lastName("Smith").build());
        saved.setFirstName("Johnny");
        dao.update(saved);
        assertThat(dao.findById(saved.getUserId()).get().getFirstName()).isEqualTo("Johnny");
    }

    @Test
    void deleteById_shouldRemove() {
        Trainee saved = dao.save(Trainee.builder().firstName("John").lastName("Smith").build());
        dao.deleteById(saved.getUserId());
        assertThat(dao.findById(saved.getUserId())).isEmpty();
    }

    @Test
    void syncIdCounter_shouldContinueFromMax() {
        storage.put(5L, Trainee.builder().userId(5L).firstName("X").lastName("Y").build());
        dao.syncIdCounter();
        Trainee saved = dao.save(Trainee.builder().firstName("N").lastName("N").build());
        assertThat(saved.getUserId()).isEqualTo(6L);
    }
}
