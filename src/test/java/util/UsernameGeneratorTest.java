package util;

import com.epam.gym.dao.UserDao;
import com.epam.gym.util.UsernameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsernameGeneratorTest {

    @Mock
    private UserDao userDao;

    @InjectMocks
    private UsernameGenerator usernameGenerator;

    @Test
    void generate_shouldReturnBaseUsername_whenNoCollision() {
        when(userDao.existsByUsername("John.Smith")).thenReturn(false);

        String username = usernameGenerator.generate("John", "Smith");

        assertThat(username).isEqualTo("John.Smith");
    }

    @Test
    void generate_shouldAppendSerial_whenBaseExists() {
        when(userDao.existsByUsername("John.Smith")).thenReturn(true);
        when(userDao.existsByUsername("John.Smith1")).thenReturn(false);

        String username = usernameGenerator.generate("John", "Smith");

        assertThat(username).isEqualTo("John.Smith1");
    }

    @Test
    void generate_shouldFindNextFreeSerial_whenMultipleExist() {
        when(userDao.existsByUsername("John.Smith")).thenReturn(true);
        when(userDao.existsByUsername("John.Smith1")).thenReturn(true);
        when(userDao.existsByUsername("John.Smith2")).thenReturn(false);

        String username = usernameGenerator.generate("John", "Smith");

        assertThat(username).isEqualTo("John.Smith2");
    }
}