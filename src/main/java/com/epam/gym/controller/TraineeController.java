package com.epam.gym.controller;

import com.epam.gym.dto.request.ActivateRequest;
import com.epam.gym.dto.request.TraineeRegistrationRequest;
import com.epam.gym.dto.request.UpdateTraineeRequest;
import com.epam.gym.dto.request.UpdateTraineeTrainersRequest;
import com.epam.gym.dto.response.CredentialsResponse;
import com.epam.gym.dto.response.TraineeProfileResponse;
import com.epam.gym.dto.response.TrainerShortResponse;
import com.epam.gym.dto.response.TrainingResponse;
import com.epam.gym.model.TrainingTypeName;
import com.epam.gym.service.TraineeService;
import com.epam.gym.service.TrainingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trainees")
public class TraineeController {

    private static final Logger log = LoggerFactory.getLogger(TraineeController.class);

    private static final String AUTH_USER = "X-Auth-Username";
    private static final String AUTH_PASS = "X-Auth-Password";

    private final TraineeService traineeService;
    private final TrainingService trainingService;

    public TraineeController(TraineeService traineeService,
                             TrainingService trainingService) {
        this.traineeService = traineeService;
        this.trainingService = trainingService;
    }

    // ---------- Endpoint 1: Register trainee (no auth) ----------
    @PostMapping
    public ResponseEntity<CredentialsResponse> register(
            @Valid @RequestBody TraineeRegistrationRequest request) {
        log.info("POST /api/trainees — register '{} {}'",
                request.firstName(), request.lastName());
        CredentialsResponse credentials = traineeService.create(
                request.firstName(), request.lastName(),
                request.dateOfBirth(), request.address());
        return ResponseEntity.status(HttpStatus.CREATED).body(credentials);
    }

    // ---------- Endpoint 5: Get trainee profile ----------
    @GetMapping("/{username}")
    public ResponseEntity<TraineeProfileResponse> getProfile(
            @PathVariable String username,
            @RequestHeader(AUTH_USER) String authUser,
            @RequestHeader(AUTH_PASS) String authPass) {
        log.info("GET /api/trainees/{}", username);
        TraineeProfileResponse profile = traineeService.getProfile(authUser, authPass);
        return ResponseEntity.ok(profile);
    }

    // ---------- Endpoint 6: Update trainee profile ----------
    @PutMapping("/{username}")
    public ResponseEntity<TraineeProfileResponse> update(
            @PathVariable String username,
            @Valid @RequestBody UpdateTraineeRequest request,
            @RequestHeader(AUTH_USER) String authUser,
            @RequestHeader(AUTH_PASS) String authPass) {
        log.info("PUT /api/trainees/{}", username);
        TraineeProfileResponse profile = traineeService.update(
                authUser, authPass,
                request.firstName(), request.lastName(),
                request.dateOfBirth(), request.address(), request.isActive());
        return ResponseEntity.ok(profile);
    }

    // ---------- Endpoint 7: Delete trainee ----------
    @DeleteMapping("/{username}")
    public ResponseEntity<Void> delete(
            @PathVariable String username,
            @RequestHeader(AUTH_USER) String authUser,
            @RequestHeader(AUTH_PASS) String authPass) {
        log.info("DELETE /api/trainees/{}", username);
        traineeService.delete(authUser, authPass);
        return ResponseEntity.noContent().build();
    }

    // ---------- Endpoint 15: Activate / deactivate ----------
    @PatchMapping("/{username}/status")
    public ResponseEntity<Void> setActive(
            @PathVariable String username,
            @Valid @RequestBody ActivateRequest request,
            @RequestHeader(AUTH_USER) String authUser,
            @RequestHeader(AUTH_PASS) String authPass) {
        log.info("PATCH /api/trainees/{}/status -> {}", username, request.isActive());
        traineeService.setActive(authUser, authPass, request.isActive());
        return ResponseEntity.ok().build();
    }

    // ---------- Endpoint 11: Update trainee's trainers list ----------
    @PutMapping("/{username}/trainers")
    public ResponseEntity<List<TrainerShortResponse>> updateTrainers(
            @PathVariable String username,
            @Valid @RequestBody UpdateTraineeTrainersRequest request,
            @RequestHeader(AUTH_USER) String authUser,
            @RequestHeader(AUTH_PASS) String authPass) {
        log.info("PUT /api/trainees/{}/trainers", username);
        List<TrainerShortResponse> trainers = trainingService.updateTraineeTrainers(
                authUser, authPass, request.trainerUsernames());
        return ResponseEntity.ok(trainers);
    }

    // ---------- Endpoint 12: Get trainee trainings ----------
    @GetMapping("/{username}/trainings")
    public ResponseEntity<List<TrainingResponse>> getTrainings(
            @PathVariable String username,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String trainerName,
            @RequestParam(required = false) TrainingTypeName trainingType,
            @RequestHeader(AUTH_USER) String authUser,
            @RequestHeader(AUTH_PASS) String authPass) {
        log.info("GET /api/trainees/{}/trainings", username);
        List<TrainingResponse> trainings = trainingService.getTraineeTrainings(
                authUser, authPass, from, to, trainerName, trainingType);
        return ResponseEntity.ok(trainings);
    }
}