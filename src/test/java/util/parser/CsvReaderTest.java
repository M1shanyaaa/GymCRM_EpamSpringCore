package util.parser;

import com.epam.gym.util.CsvReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvReaderTest {

    private final CsvReader csvReader = new CsvReader();

    @Test
    void readAll_shouldReturnDataRows_skippingHeader() {
        List<String[]> rows = csvReader.readAll("test-data/valid.csv");

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsExactly("a", "b", "c");
        assertThat(rows.get(1)).containsExactly("d", "e", "f");
    }

    @Test
    void readAll_shouldReturnEmptyList_whenOnlyHeader() {
        List<String[]> rows = csvReader.readAll("test-data/header-only.csv");

        assertThat(rows).isEmpty();
    }

    @Test
    void readAll_shouldReturnEmptyList_whenFileNotFound() {
        List<String[]> rows = csvReader.readAll("test-data/does-not-exist.csv");

        assertThat(rows).isEmpty();
    }
}
