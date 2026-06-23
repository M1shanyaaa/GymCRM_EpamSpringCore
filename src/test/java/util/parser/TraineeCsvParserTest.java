package util.parser;

import com.epam.gym.model.Trainee;
import com.epam.gym.util.TraineeCsvParser;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TraineeCsvParserTest {

    private final TraineeCsvParser parser = new TraineeCsvParser();

    @Test
    void parse_shouldMapValidRowToTrainee() {
        String[] row = {"1", "John", "Smith", "John.Smith", "pass123456",
                "true", "1990-05-15", "Kyiv Main St 10"};

        Trainee result = parser.parse(row);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Smith");
        assertThat(result.getUsername()).isEqualTo("John.Smith");
        assertThat(result.getPassword()).isEqualTo("pass123456");
        assertThat(result.isActive()).isTrue();
        assertThat(result.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
        assertThat(result.getAddress()).isEqualTo("Kyiv Main St 10");
    }

    @Test
    void parse_shouldTrimWhitespace() {
        String[] row = {" 2 ", " Anna ", " Brown ", " Anna.Brown ", " pwd ",
                " false ", " 1995-08-22 ", " Lviv "};

        Trainee result = parser.parse(row);

        assertThat(result.getUserId()).isEqualTo(2L);
        assertThat(result.getFirstName()).isEqualTo("Anna");
        assertThat(result.isActive()).isFalse();
    }

    @Test
    void parse_shouldReturnNull_whenRowIsMalformed() {
        String[] row = {"not-a-number", "John", "Smith"};

        Trainee result = parser.parse(row);

        assertThat(result).isNull();
    }

    @Test
    void parse_shouldReturnNull_whenRowTooShort() {
        String[] row = {"1", "John"};

        Trainee result = parser.parse(row);

        assertThat(result).isNull();
    }
}
