package com.epam.gym.mapper;

import com.epam.gym.dto.response.TrainingResponse;
import com.epam.gym.dto.response.TrainingTypeResponse;
import com.epam.gym.model.Training;
import com.epam.gym.model.TrainingType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TrainingMapper {

    @Mapping(source = "trainingType.trainingTypeName", target = "trainingType")
    @Mapping(source = "trainer.user.firstName", target = "trainerName")
    @Mapping(source = "trainee.user.firstName", target = "traineeName")
    TrainingResponse toResponse(Training training);

    List<TrainingResponse> toResponseList(List<Training> trainings);

    @Mapping(source = "trainingTypeName", target = "trainingType")
    @Mapping(source = "id", target = "trainingTypeId")
    TrainingTypeResponse toTypeResponse(TrainingType trainingType);

    List<TrainingTypeResponse> toTypeResponseList(List<TrainingType> types);
}