package com.epam.gym.mapper;

import com.epam.gym.dto.response.TraineeShortResponse;
import com.epam.gym.dto.response.TrainerProfileResponse;
import com.epam.gym.dto.response.TrainerShortResponse;
import com.epam.gym.model.Trainee;
import com.epam.gym.model.Trainer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TrainerMapper {

    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.firstName", target = "firstName")
    @Mapping(source = "user.lastName", target = "lastName")
    @Mapping(source = "specialization.trainingTypeName", target = "specialization")
    TrainerShortResponse toShort(Trainer trainer);

    List<TrainerShortResponse> toShortList(List<Trainer> trainers);

    @Mapping(source = "user.firstName", target = "firstName")
    @Mapping(source = "user.lastName", target = "lastName")
    @Mapping(source = "specialization.trainingTypeName", target = "specialization")
    @Mapping(source = "user.active", target = "isActive")
    @Mapping(source = "trainees", target = "trainees")
    TrainerProfileResponse toProfile(Trainer trainer);

    // trainee -> short (local, avoids cyclic mapper dependency)
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.firstName", target = "firstName")
    @Mapping(source = "user.lastName", target = "lastName")
    TraineeShortResponse traineeToShort(Trainee trainee);

    List<TraineeShortResponse> traineesToShort(List<Trainee> trainees);
}