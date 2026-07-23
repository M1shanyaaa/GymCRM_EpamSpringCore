package mapper;

import com.epam.gym.dto.response.TraineeShortResponse;
import com.epam.gym.dto.response.TrainerProfileResponse;
import com.epam.gym.dto.response.TrainerShortResponse;
import com.epam.gym.mapper.TrainerMapper;
import com.epam.gym.mapper.TrainerMapperImpl;
import com.epam.gym.model.Trainee;
import com.epam.gym.model.Trainer;
import com.epam.gym.model.TrainingType;
import com.epam.gym.model.TrainingTypeName;
import com.epam.gym.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TrainerMapperTest {

    private TrainerMapper mapper;
    private final TrainingTypeName anyType = TrainingTypeName.values()[0];

    @BeforeEach
    void setUp() {
        mapper = new TrainerMapperImpl();
    }

    // ---------- toShort ----------

    @Test
    void toShort_shouldMapAllFields() {
        Trainer trainer = Trainer.builder()
                .user(User.builder().username("t").firstName("Anna").lastName("Smith").build())
                .specialization(new TrainingType(anyType))
                .build();

        TrainerShortResponse result = mapper.toShort(trainer);

        assertThat(result.username()).isEqualTo("t");
        assertThat(result.firstName()).isEqualTo("Anna");
        assertThat(result.lastName()).isEqualTo("Smith");
        assertThat(result.specialization()).isEqualTo(anyType);
    }

    @Test
    void toShort_shouldReturnNull_whenTrainerIsNull() {
        assertThat(mapper.toShort(null)).isNull();
    }

    @Test
    void toShortList_shouldMapAllElements() {
        Trainer trainer = Trainer.builder()
                .user(User.builder().username("t").build())
                .specialization(new TrainingType(anyType))
                .build();

        assertThat(mapper.toShortList(List.of(trainer))).hasSize(1);
    }

    @Test
    void toShortList_shouldReturnNull_whenInputIsNull() {
        assertThat(mapper.toShortList(null)).isNull();
    }

    // ---------- toProfile ----------

    @Test
    void toProfile_shouldMapScalarFieldsAndActiveFlag() {
        User user = User.builder()
                .firstName("Anna")
                .lastName("Smith")
                .isActive(true)
                .build();

        Trainer trainer = Trainer.builder()
                .user(user)
                .specialization(new TrainingType(anyType))
                .build();

        TrainerProfileResponse result = mapper.toProfile(trainer);

        assertThat(result.firstName()).isEqualTo("Anna");
        assertThat(result.lastName()).isEqualTo("Smith");
        assertThat(result.specialization()).isEqualTo(anyType);
        assertThat(result.isActive()).isTrue();
        assertThat(result.trainees()).isEmpty();
    }

    @Test
    void toProfile_shouldMapTraineesSetToShortList() {
        Trainee trainee = Trainee.builder()
                .user(User.builder().username("tr1").firstName("John").lastName("Doe").build())
                .build();

        Trainer trainer = Trainer.builder()
                .user(User.builder().build())
                .specialization(new TrainingType(anyType))
                .trainees(Set.of(trainee))
                .build();

        TrainerProfileResponse result = mapper.toProfile(trainer);

        assertThat(result.trainees()).hasSize(1);
        TraineeShortResponse mapped = result.trainees().get(0);
        assertThat(mapped.username()).isEqualTo("tr1");
        assertThat(mapped.firstName()).isEqualTo("John");
        assertThat(mapped.lastName()).isEqualTo("Doe");
    }

    @Test
    void toProfile_shouldReturnNull_whenTrainerIsNull() {
        assertThat(mapper.toProfile(null)).isNull();
    }

    // ---------- traineeToShort (local mapping) ----------

    @Test
    void traineeToShort_shouldMapAllFields() {
        Trainee trainee = Trainee.builder()
                .user(User.builder().username("tr").firstName("John").lastName("Doe").build())
                .build();

        TraineeShortResponse result = mapper.traineeToShort(trainee);

        assertThat(result.username()).isEqualTo("tr");
        assertThat(result.firstName()).isEqualTo("John");
        assertThat(result.lastName()).isEqualTo("Doe");
    }

    @Test
    void traineeToShort_shouldReturnNull_whenTraineeIsNull() {
        assertThat(mapper.traineeToShort(null)).isNull();
    }

    @Test
    void traineesToShort_shouldMapList() {
        Trainee trainee = Trainee.builder()
                .user(User.builder().username("tr").build())
                .build();

        assertThat(mapper.traineesToShort(List.of(trainee))).hasSize(1);
    }

    @Test
    void traineesToShort_shouldReturnNull_whenInputIsNull() {
        assertThat(mapper.traineesToShort(null)).isNull();
    }
}