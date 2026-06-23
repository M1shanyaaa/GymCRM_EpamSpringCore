package util;

import com.epam.gym.util.PasswordGenerator;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordGeneratorTest {

    private final PasswordGenerator passwordGenerator = new PasswordGenerator();

    @Test
    void generate_shouldReturnPasswordOfLength10() {
        String password = passwordGenerator.generate();

        assertThat(password).isNotNull();
        assertThat(password).hasSize(10);
    }

    @Test
    void generate_shouldContainOnlyAlphanumericCharacters() {
        String password = passwordGenerator.generate();

        assertThat(password).matches("[A-Za-z0-9]+");
    }

    @Test
    void generate_shouldProduceDifferentPasswords_onConsecutiveCalls() {
        String p1 = passwordGenerator.generate();
        String p2 = passwordGenerator.generate();

        // Two random 10-char strings are virtually never identical
        assertThat(p1).isNotEqualTo(p2);
    }
}
