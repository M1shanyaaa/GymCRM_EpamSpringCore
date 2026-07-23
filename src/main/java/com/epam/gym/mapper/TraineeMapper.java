package com.epam.gym.mapper;

import com.epam.gym.dto.response.TraineeProfileResponse;
import com.epam.gym.dto.response.TraineeShortResponse;
import com.epam.gym.dto.response.TrainerShortResponse;
import com.epam.gym.model.Trainee;
import com.epam.gym.model.Trainer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TraineeMapper {

    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.firstName", target = "firstName")
    @Mapping(source = "user.lastName", target = "lastName")
    TraineeShortResponse toShort(Trainee trainee);

    List<TraineeShortResponse> toShortList(List<Trainee> trainees);

    @Mapping(source = "user.firstName", target = "firstName")
    @Mapping(source = "user.lastName", target = "lastName")
    @Mapping(source = "user.active", target = "isActive")
    @Mapping(source = "trainers", target = "trainers")
    TraineeProfileResponse toProfile(Trainee trainee);

    // trainer -> short (local, avoids cyclic mapper dependency)
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.firstName", target = "firstName")
    @Mapping(source = "user.lastName", target = "lastName")
    @Mapping(source = "specialization.trainingTypeName", target = "specialization")
    TrainerShortResponse trainerToShort(Trainer trainer);

    List<TrainerShortResponse> trainersToShort(List<Trainer> trainers);
}