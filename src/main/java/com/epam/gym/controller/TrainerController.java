package com.epam.gym.controller;

import com.epam.gym.dto.request.ActivateRequest;
import com.epam.gym.dto.request.TrainerRegistrationRequest;
import com.epam.gym.dto.request.UpdateTrainerRequest;
import com.epam.gym.dto.response.CredentialsResponse;
import com.epam.gym.dto.response.TrainerProfileResponse;
import com.epam.gym.dto.response.TrainerShortResponse;
import com.epam.gym.dto.response.TrainingResponse;
import com.epam.gym.service.TrainerService;
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
@RequestMapping("/api/trainers")
public class TrainerController {

    private static final Logger log = LoggerFactory.getLogger(TrainerController.class);

    private static final String AUTH_USER = "X-Auth-Username";
    private static final String AUTH_PASS = "X-Auth-Password";

    private final TrainerService trainerService;
    private final TrainingService trainingService;

    public TrainerController(TrainerService trainerService,
                             TrainingService trainingService) {
        this.trainerService = trainerService;
        this.trainingService = trainingService;
    }

    // ---------- Endpoint 2: Register trainer (no auth needed) ----------
    @PostMapping
    public ResponseEntity<CredentialsResponse> register(
            @Valid @RequestBody TrainerRegistrationRequest request) {
        log.info("POST /api/trainers — register '{} {}'",
                request.firstName(), request.lastName());
        CredentialsResponse credentials = trainerService.create(
                request.firstName(), request.lastName(), request.specialization());
        return ResponseEntity.status(HttpStatus.CREATED).body(credentials);
    }

    // ---------- Endpoint 8: Get trainer profile ----------
    @GetMapping("/{username}")
    public ResponseEntity<TrainerProfileResponse> getProfile(
            @PathVariable String username,
            @RequestHeader(AUTH_USER) String authUser,
            @RequestHeader(AUTH_PASS) String authPass) {
        log.info("GET /api/trainers/{}", username);
        TrainerProfileResponse profile = trainerService.getProfile(authUser, authPass);
        return ResponseEntity.ok(profile);
    }

    // ---------- Endpoint 9: Update trainer profile ----------
    @PutMapping("/{username}")
    public ResponseEntity<TrainerProfileResponse> update(
            @PathVariable String username,
            @Valid @RequestBody UpdateTrainerRequest request,
            @RequestHeader(AUTH_USER) String authUser,
            @RequestHeader(AUTH_PASS) String authPass) {
        log.info("PUT /api/trainers/{}", username);
        TrainerProfileResponse profile = trainerService.update(
                authUser, authPass,
                request.firstName(), request.lastName(), request.isActive());
        return ResponseEntity.ok(profile);
    }

    // ---------- Endpoint 16: Activate / deactivate ----------
    @PatchMapping("/{username}/status")
    public ResponseEntity<Void> setActive(
            @PathVariable String username,
            @Valid @RequestBody ActivateRequest request,
            @RequestHeader(AUTH_USER) String authUser,
            @RequestHeader(AUTH_PASS) String authPass) {
        log.info("PATCH /api/trainers/{}/status -> {}", username, request.isActive());
        trainerService.setActive(authUser, authPass, request.isActive());
        return ResponseEntity.ok().build();
    }

    // ---------- Endpoint 10: Get not-assigned active trainers ----------
    @GetMapping("/unassigned")
    public ResponseEntity<List<TrainerShortResponse>> getUnassigned(
            @RequestHeader(AUTH_USER) String authUser,
            @RequestHeader(AUTH_PASS) String authPass) {
        log.info("GET /api/trainers/unassigned (trainee='{}')", authUser);
        List<TrainerShortResponse> result =
                trainerService.findUnassignedTrainers(authUser, authPass);
        return ResponseEntity.ok(result);
    }

    // ---------- Endpoint 13: Get trainer trainings ----------
    @GetMapping("/{username}/trainings")
    public ResponseEntity<List<TrainingResponse>> getTrainings(
            @PathVariable String username,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String traineeName,
            @RequestHeader(AUTH_USER) String authUser,
            @RequestHeader(AUTH_PASS) String authPass) {
        log.info("GET /api/trainers/{}/trainings", username);
        List<TrainingResponse> trainings = trainingService.getTrainerTrainings(
                authUser, authPass, from, to, traineeName);
        return ResponseEntity.ok(trainings);
    }
}