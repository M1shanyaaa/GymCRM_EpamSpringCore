package util.parser;

import com.epam.gym.model.Trainer;
import com.epam.gym.model.TrainingTypeName;
import com.epam.gym.util.TrainerCsvParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TrainerCsvParserTest {

    private final TrainerCsvParser parser = new TrainerCsvParser();

    @Test
    void parse_shouldMapValidRowToTrainer() {
        String[] row = {"1", "Mike", "Jordan", "Mike.Jordan", "pass123456",
                "true", "FITNESS"};

        Trainer result = parser.parse(row);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getFirstName()).isEqualTo("Mike");
        assertThat(result.getLastName()).isEqualTo("Jordan");
        assertThat(result.isActive()).isTrue();
        assertThat(result.getSpecialization().getTrainingTypeName())
                .isEqualTo(TrainingTypeName.FITNESS);
    }

    @Test
    void parse_shouldHandleLowercaseSpecialization() {
        String[] row = {"2", "Sara", "Connor", "Sara.Connor", "pwd",
                "true", "yoga"};

        Trainer result = parser.parse(row);

        assertThat(result.getSpecialization().getTrainingTypeName())
                .isEqualTo(TrainingTypeName.YOGA);
    }

    @Test
    void parse_shouldReturnNull_whenSpecializationInvalid() {
        String[] row = {"1", "Mike", "Jordan", "Mike.Jordan", "pwd",
                "true", "UNKNOWN_TYPE"};

        Trainer result = parser.parse(row);

        assertThat(result).isNull();
    }

    @Test
    void parse_shouldReturnNull_whenRowMalformed() {
        String[] row = {"bad-id", "Mike", "Jordan"};

        Trainer result = parser.parse(row);

        assertThat(result).isNull();
    }
}
