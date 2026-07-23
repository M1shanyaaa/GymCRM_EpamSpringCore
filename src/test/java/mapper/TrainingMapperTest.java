package mapper;

import com.epam.gym.dto.response.TrainingResponse;
import com.epam.gym.dto.response.TrainingTypeResponse;
import com.epam.gym.mapper.TrainingMapper;
import com.epam.gym.mapper.TrainingMapperImpl;
import com.epam.gym.model.Trainee;
import com.epam.gym.model.Trainer;
import com.epam.gym.model.Training;
import com.epam.gym.model.TrainingType;
import com.epam.gym.model.TrainingTypeName;
import com.epam.gym.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingMapperTest {

    private TrainingMapper mapper;
    private final TrainingTypeName anyType = TrainingTypeName.values()[0];

    @BeforeEach
    void setUp() {
        mapper = new TrainingMapperImpl();
    }

    // ---------- toResponse ----------

    @Test
    void toResponse_shouldMapAllFields() {
        Trainer trainer = Trainer.builder()
                .user(User.builder().firstName("Anna").build())
                .build();
        Trainee trainee = Trainee.builder()
                .user(User.builder().firstName("John").build())
                .build();

        Training training = Training.builder()
                .trainingName("Morning Yoga")
                .trainingDate(LocalDate.of(2024, 6, 1))
                .trainingDuration(60)
                .trainingType(new TrainingType(anyType))
                .trainer(trainer)
                .trainee(trainee)
                .build();

        TrainingResponse result = mapper.toResponse(training);

        assertThat(result.trainingName()).isEqualTo("Morning Yoga");
        assertThat(result.trainingDate()).isEqualTo(LocalDate.of(2024, 6, 1));
        assertThat(result.trainingDuration()).isEqualTo(60);
        assertThat(result.trainingType()).isEqualTo(anyType);
        assertThat(result.trainerName()).isEqualTo("Anna");
        assertThat(result.traineeName()).isEqualTo("John");
    }

    @Test
    void toResponse_shouldHandleNullTrainerAndTrainee() {
        Training training = Training.builder()
                .trainingName("Test")
                .trainingDate(LocalDate.now())
                .trainingDuration(45)
                .trainingType(new TrainingType(anyType))
                .trainer(null)
                .trainee(null)
                .build();

        TrainingResponse result = mapper.toResponse(training);

        assertThat(result.trainerName()).isNull();
        assertThat(result.traineeName()).isNull();
    }

    @Test
    void toResponse_shouldHandleNullTrainingType() {
        Training training = Training.builder()
                .trainingName("Test")
                .trainingDate(LocalDate.now())
                .trainingDuration(45)
                .trainer(Trainer.builder().user(User.builder().firstName("A").build()).build())
                .trainee(Trainee.builder().user(User.builder().firstName("B").build()).build())
                .trainingType(null)
                .build();

        TrainingResponse result = mapper.toResponse(training);

        assertThat(result.trainingType()).isNull();
    }

    @Test
    void toResponse_shouldReturnNull_whenTrainingIsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    void toResponseList_shouldMapAllElements() {
        Training training = Training.builder()
                .trainingName("Cardio")
                .trainingDate(LocalDate.now())
                .trainingDuration(30)
                .trainingType(new TrainingType(anyType))
                .trainer(Trainer.builder().user(User.builder().firstName("A").build()).build())
                .trainee(Trainee.builder().user(User.builder().firstName("B").build()).build())
                .build();

        List<TrainingResponse> result = mapper.toResponseList(List.of(training));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).trainingName()).isEqualTo("Cardio");
    }

    @Test
    void toResponseList_shouldReturnNull_whenInputIsNull() {
        assertThat(mapper.toResponseList(null)).isNull();
    }

    // ---------- toTypeResponse ----------

    @Test
    void toTypeResponse_shouldMapIdAndName() {
        TrainingType type = new TrainingType(anyType);
        ReflectionTestUtils.setField(type, "id", 42L);

        TrainingTypeResponse result = mapper.toTypeResponse(type);

        assertThat(result.trainingType()).isEqualTo(anyType);
        assertThat(result.trainingTypeId()).isEqualTo(42L);
    }

    @Test
    void toTypeResponse_shouldReturnNull_whenInputIsNull() {
        assertThat(mapper.toTypeResponse(null)).isNull();
    }

    @Test
    void toTypeResponseList_shouldMapAllElements() {
        TrainingType type1 = new TrainingType(anyType);
        ReflectionTestUtils.setField(type1, "id", 1L);

        List<TrainingTypeResponse> result = mapper.toTypeResponseList(List.of(type1));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).trainingTypeId()).isEqualTo(1L);
    }

    @Test
    void toTypeResponseList_shouldReturnNull_whenInputIsNull() {
        assertThat(mapper.toTypeResponseList(null)).isNull();
    }
}