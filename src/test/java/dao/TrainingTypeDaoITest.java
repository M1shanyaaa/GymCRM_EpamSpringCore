package dao;

import com.epam.gym.dao.TrainingTypeDao;
import com.epam.gym.model.TrainingType;
import com.epam.gym.model.TrainingTypeName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingTypeDaoITest extends AbstractDaoITest {

    @Autowired private TrainingTypeDao trainingTypeDao;

    @Test
    void save_andFindByName() {
        trainingTypeDao.save(new TrainingType(TrainingTypeName.YOGA));

        assertThat(trainingTypeDao.findByName(TrainingTypeName.YOGA)).isPresent();
    }

    @Test
    void findByName_shouldReturnEmpty_whenMissing() {
        assertThat(trainingTypeDao.findByName(TrainingTypeName.CARDIO)).isEmpty();
    }

    @Test
    void findAll_shouldReturnAllSaved() {
        trainingTypeDao.save(new TrainingType(TrainingTypeName.YOGA));
        trainingTypeDao.save(new TrainingType(TrainingTypeName.CARDIO));

        assertThat(trainingTypeDao.findAll()).hasSizeGreaterThanOrEqualTo(2);
    }
}
