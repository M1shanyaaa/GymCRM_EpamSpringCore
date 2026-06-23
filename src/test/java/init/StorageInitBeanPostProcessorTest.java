package init;

import com.epam.gym.init.StorageInitBeanPostProcessor;
import com.epam.gym.model.Trainee;
import com.epam.gym.model.Trainer;
import com.epam.gym.model.Training;
import com.epam.gym.util.CsvReader;
import com.epam.gym.util.TraineeCsvParser;
import com.epam.gym.util.TrainerCsvParser;
import com.epam.gym.util.TrainingCsvParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageInitBeanPostProcessorTest {

    @Mock
    private CsvReader csvReader;
    @Mock
    private TraineeCsvParser traineeParser;
    @Mock
    private TrainerCsvParser trainerParser;
    @Mock
    private TrainingCsvParser trainingParser;

    @InjectMocks
    private StorageInitBeanPostProcessor processor;

    private final String traineeFile = "trainees.csv";
    private final String trainerFile = "trainers.csv";
    private final String trainingFile = "trainings.csv";

    @BeforeEach
    void setUp() {
        // Використовуємо ReflectionTestUtils для ін'єкції значень @Value
        ReflectionTestUtils.setField(processor, "traineeFile", traineeFile);
        ReflectionTestUtils.setField(processor, "trainerFile", trainerFile);
        ReflectionTestUtils.setField(processor, "trainingFile", trainingFile);
    }

    @Test
    void postProcessAfterInitialization_shouldInitTraineeStorage() {
        // Arrange
        Map<Long, Trainee> traineeStorage = new HashMap<>();

        // ВИПРАВЛЕНО: Використовуємо Collections.singletonList
        List<String[]> mockRows = Collections.singletonList(new String[]{"John", "Doe"});
        Trainee mockTrainee = Trainee.builder().userId(1L).firstName("John").build();

        when(csvReader.readAll(traineeFile)).thenReturn(mockRows);
        when(traineeParser.parse(mockRows.get(0))).thenReturn(mockTrainee);

        // Act
        Object result = processor.postProcessAfterInitialization(traineeStorage, "traineeStorage");

        // Assert
        assertThat(result).isSameAs(traineeStorage);
        assertThat(traineeStorage).hasSize(1);
        assertThat(traineeStorage.get(1L)).isEqualTo(mockTrainee);
        verify(csvReader).readAll(traineeFile);
    }

    @Test
    void postProcessAfterInitialization_shouldInitTrainerStorage() {
        // Arrange
        Map<Long, Trainer> trainerStorage = new HashMap<>();

        // ВИПРАВЛЕНО
        List<String[]> mockRows = Collections.singletonList(new String[]{"Jane", "Smith"});
        Trainer mockTrainer = Trainer.builder().userId(2L).firstName("Jane").build();

        when(csvReader.readAll(trainerFile)).thenReturn(mockRows);
        when(trainerParser.parse(mockRows.get(0))).thenReturn(mockTrainer);

        // Act
        processor.postProcessAfterInitialization(trainerStorage, "trainerStorage");

        // Assert
        assertThat(trainerStorage).hasSize(1);
        assertThat(trainerStorage.get(2L)).isEqualTo(mockTrainer);
    }

    @Test
    void postProcessAfterInitialization_shouldInitTrainingStorage() {
        // Arrange
        Map<Long, Training> trainingStorage = new HashMap<>();

        // ВИПРАВЛЕНО
        List<String[]> mockRows = Collections.singletonList(new String[]{"Yoga", "2023-10-10"});
        Training mockTraining = Training.builder().trainingName("Yoga").build();

        when(csvReader.readAll(trainingFile)).thenReturn(mockRows);
        when(trainingParser.parse(mockRows.get(0))).thenReturn(mockTraining);

        // Act
        processor.postProcessAfterInitialization(trainingStorage, "trainingStorage");

        // Assert
        assertThat(trainingStorage).hasSize(1);
        assertThat(trainingStorage.get(1L)).isEqualTo(mockTraining);
    }

    @Test
    void postProcessAfterInitialization_shouldIgnoreNullParses() {
        // Arrange
        Map<Long, Trainee> traineeStorage = new HashMap<>();

        // ВИПРАВЛЕНО
        List<String[]> mockRows = Collections.singletonList(new String[]{"Invalid", "Data"});

        when(csvReader.readAll(traineeFile)).thenReturn(mockRows);
        when(traineeParser.parse(any())).thenReturn(null); // Парсер повернув null

        // Act
        processor.postProcessAfterInitialization(traineeStorage, "traineeStorage");

        // Assert
        assertThat(traineeStorage).isEmpty(); // Мапа має залишитись порожньою
    }

    @Test
    void postProcessAfterInitialization_shouldIgnoreUnknownBeans() {
        // Arrange
        Object randomBean = new Object();

        // Act
        Object result = processor.postProcessAfterInitialization(randomBean, "someRandomBeanName");

        // Assert
        assertThat(result).isSameAs(randomBean);
        // Перевіряємо, що жоден з моків взагалі не викликався
        verifyNoInteractions(csvReader, traineeParser, trainerParser, trainingParser);
    }
}