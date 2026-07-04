package com.epam.gym.dao;

import com.epam.gym.model.Training;
import com.epam.gym.model.TrainingTypeName;

import java.time.LocalDate;
import java.util.List;

public interface TrainingDao {

    Training save(Training training);

    // Function 14: trainee trainings with criteria
    List<Training> findTraineeTrainings(String traineeUsername,
                                        LocalDate fromDate,
                                        LocalDate toDate,
                                        String trainerName,
                                        TrainingTypeName trainingType);

    // Function 15: trainer trainings with criteria
    List<Training> findTrainerTrainings(String trainerUsername,
                                        LocalDate fromDate,
                                        LocalDate toDate,
                                        String traineeName);
}