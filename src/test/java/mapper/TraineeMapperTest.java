package mapper;

import com.epam.gym.dto.response.TraineeProfileResponse;
import com.epam.gym.dto.response.TraineeShortResponse;
import com.epam.gym.dto.response.TrainerShortResponse;
import com.epam.gym.mapper.TraineeMapper;
import com.epam.gym.mapper.TraineeMapperImpl;
import com.epam.gym.model.Trainee;
import com.epam.gym.model.Trainer;
import com.epam.gym.model.TrainingType;
import com.epam.gym.model.TrainingTypeName;
import com.epam.gym.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TraineeMapperTest {

    private TraineeMapper mapper;
    private final TrainingTypeName anyType = TrainingTypeName.values()[0];

    @BeforeEach
    void setUp() {
        mapper = new TraineeMapperImpl();
    }

    // ---------- toShort ----------

    @Test
    void toShort_shouldMapUserFields() {
        User user = User.builder()
                .username("John.Doe")
                .firstName("John")
                .lastName("Doe")
                .build();
        Trainee trainee = Trainee.builder().user(user).build();

        TraineeShortResponse result = mapper.toShort(trainee);

        assertThat(result.username()).isEqualTo("John.Doe");
        assertThat(result.firstName()).isEqualTo("John");
        assertThat(result.lastName()).isEqualTo("Doe");
    }

    @Test
    void toShort_shouldReturnNull_whenTraineeIsNull() {
        assertThat(mapper.toShort(null)).isNull();
    }

    @Test
    void toShortList_shouldMapAllElements() {
        Trainee t1 = Trainee.builder().user(User.builder().username("a").build()).build();
        Trainee t2 = Trainee.builder().user(User.builder().username("b").build()).build();

        List<TraineeShortResponse> result = mapper.toShortList(List.of(t1, t2));

        assertThat(result).extracting(TraineeShortResponse::username)
                .containsExactly("a", "b");
    }

    @Test
    void toShortList_shouldReturnNull_whenInputIsNull() {
        assertThat(mapper.toShortList(null)).isNull();
    }

    @Test
    void toShortList_shouldReturnEmptyList_whenInputIsEmpty() {
        assertThat(mapper.toShortList(List.of())).isEmpty();
    }

    // ---------- toProfile ----------

    @Test
    void toProfile_shouldMapScalarFieldsAndActiveFlag() {
        User user = User.builder()
                .firstName("John")
                .lastName("Doe")
                .isActive(true)
                .build();

        Trainee trainee = Trainee.builder()
                .user(user)
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .address("Kyiv")
                .build();

        TraineeProfileResponse result = mapper.toProfile(trainee);

        assertThat(result.firstName()).isEqualTo("John");
        assertThat(result.lastName()).isEqualTo("Doe");
        assertThat(result.dateOfBirth()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(result.address()).isEqualTo("Kyiv");
        assertThat(result.isActive()).isTrue();
        assertThat(result.trainers()).isEmpty();
    }

    @Test
    void toProfile_shouldMapTrainersSetToShortList() {
        Trainer trainer = Trainer.builder()
                .user(User.builder().username("t1").firstName("Anna").lastName("Smith").build())
                .specialization(new TrainingType(anyType))
                .build();

        Trainee trainee = Trainee.builder()
                .user(User.builder().build())
                .trainers(Set.of(trainer))
                .build();

        TraineeProfileResponse result = mapper.toProfile(trainee);

        assertThat(result.trainers()).hasSize(1);
        TrainerShortResponse mappedTrainer = result.trainers().get(0);
        assertThat(mappedTrainer.username()).isEqualTo("t1");
        assertThat(mappedTrainer.firstName()).isEqualTo("Anna");
        assertThat(mappedTrainer.lastName()).isEqualTo("Smith");
        assertThat(mappedTrainer.specialization()).isEqualTo(anyType);
    }

    @Test
    void toProfile_shouldReturnNull_whenTraineeIsNull() {
        assertThat(mapper.toProfile(null)).isNull();
    }

    // ---------- trainerToShort (local mapping) ----------

    @Test
    void trainerToShort_shouldMapAllFieldsIncludingSpecialization() {
        Trainer trainer = Trainer.builder()
                .user(User.builder().username("t").firstName("Anna").lastName("Smith").build())
                .specialization(new TrainingType(anyType))
                .build();

        TrainerShortResponse result = mapper.trainerToShort(trainer);

        assertThat(result.username()).isEqualTo("t");
        assertThat(result.firstName()).isEqualTo("Anna");
        assertThat(result.lastName()).isEqualTo("Smith");
        assertThat(result.specialization()).isEqualTo(anyType);
    }

    @Test
    void trainerToShort_shouldReturnNull_whenTrainerIsNull() {
        assertThat(mapper.trainerToShort(null)).isNull();
    }

    @Test
    void trainersToShort_shouldMapList() {
        Trainer trainer = Trainer.builder()
                .user(User.builder().username("t").build())
                .specialization(new TrainingType(anyType))
                .build();

        assertThat(mapper.trainersToShort(List.of(trainer))).hasSize(1);
    }

    @Test
    void trainersToShort_shouldReturnNull_whenInputIsNull() {
        assertThat(mapper.trainersToShort(null)).isNull();
    }
}