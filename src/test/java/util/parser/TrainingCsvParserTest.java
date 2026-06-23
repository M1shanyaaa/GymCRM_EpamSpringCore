package util.parser;

import com.epam.gym.model.Training;
import com.epam.gym.model.TrainingTypeName;
import com.epam.gym.util.TrainingCsvParser;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingCsvParserTest {

    private final TrainingCsvParser parser = new TrainingCsvParser();

    @Test
    void parse_shouldMapValidRowToTraining() {
        String[] row = {"1", "2", "Morning Fitness", "FITNESS",
                "2024-01-15", "PT1H"};

        Training result = parser.parse(row);

        assertThat(result).isNotNull();
        assertThat(result.getTraineeId()).isEqualTo(1L);
        assertThat(result.getTrainerId()).isEqualTo(2L);
        assertThat(result.getTrainingName()).isEqualTo("Morning Fitness");
        assertThat(result.getTrainingType().getTrainingTypeName())
                .isEqualTo(TrainingTypeName.FITNESS);
        assertThat(result.getTrainingDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(result.getTrainingDuration()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void parse_shouldHandleLowercaseTrainingType() {
        String[] row = {"1", "2", "Evening Yoga", "yoga",
                "2024-01-16", "PT1H30M"};

        Training result = parser.parse(row);

        assertThat(result.getTrainingType().getTrainingTypeName())
                .isEqualTo(TrainingTypeName.YOGA);
        assertThat(result.getTrainingDuration())
                .isEqualTo(Duration.ofMinutes(90));
    }

    @Test
    void parse_shouldReturnNull_whenDurationInvalid() {
        String[] row = {"1", "2", "Session", "FITNESS",
                "2024-01-15", "not-a-duration"};

        Training result = parser.parse(row);

        assertThat(result).isNull();
    }

    @Test
    void parse_shouldReturnNull_whenRowMalformed() {
        String[] row = {"bad", "2"};

        Training result = parser.parse(row);

        assertThat(result).isNull();
    }
}