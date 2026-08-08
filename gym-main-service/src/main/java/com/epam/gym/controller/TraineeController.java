package com.epam.gym.controller;

import com.epam.gym.config.OpenApiConfig;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trainees")
@Tag(name = "Trainee", description = "Trainee management endpoints")
public class TraineeController {

    private static final Logger log = LoggerFactory.getLogger(TraineeController.class);
    private final TraineeService traineeService;
    private final TrainingService trainingService;

    public TraineeController(TraineeService traineeService,
                             TrainingService trainingService) {
        this.traineeService = traineeService;
        this.trainingService = trainingService;
    }

    // ---------- Endpoint 1: Register trainee ----------
    @PostMapping
    @Operation(summary = "Register a new trainee",
            description = "Creates a trainee profile; username and password are auto-generated. Public endpoint.",
            security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainee registered, credentials returned"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<CredentialsResponse> register(
            @Valid @RequestBody TraineeRegistrationRequest request) {
        log.info("POST /api/trainees — register '{} {}'",
                request.firstName(), request.lastName());
        CredentialsResponse credentials = traineeService.create(
                request.firstName(), request.lastName(),
                request.dateOfBirth(), request.address());
        return ResponseEntity.ok(credentials);
    }

    // ---------- Endpoint 5: Get trainee profile ----------
    @GetMapping("/{username}")
    @PreAuthorize("#username == authentication.name")
    @Operation(summary = "Get trainee profile",
            description = "Returns the trainee's profile. JWT required. User can only access their own profile.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Forbidden access (wrong user)"),
            @ApiResponse(responseCode = "404", description = "Trainee not found")
    })
    public ResponseEntity<TraineeProfileResponse> getProfile(
            @Parameter(description = "Trainee username") @PathVariable String username) {
        log.info("GET /api/trainees/{}", username);
        TraineeProfileResponse profile = traineeService.getProfile(username);
        return ResponseEntity.ok(profile);
    }

    // ---------- Endpoint 6: Update trainee profile ----------
    @PutMapping("/{username}")
    @PreAuthorize("#username == authentication.name")
    @Operation(summary = "Update trainee profile",
            description = "Updates the trainee's profile. User can only update their own profile.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Forbidden access (wrong user)")
    })
    public ResponseEntity<TraineeProfileResponse> update(
            @Parameter(description = "Trainee username") @PathVariable String username,
            @Valid @RequestBody UpdateTraineeRequest request) {
        log.info("PUT /api/trainees/{}", username);
        TraineeProfileResponse profile = traineeService.update(
                username,
                request.firstName(), request.lastName(),
                request.dateOfBirth(), request.address(), request.isActive());
        return ResponseEntity.ok(profile);
    }

    // ---------- Endpoint 7: Delete trainee ----------
    @DeleteMapping("/{username}")
    @PreAuthorize("#username == authentication.name")
    @Operation(summary = "Delete trainee profile",
            description = "Hard-deletes the trainee. User can only delete their own profile.")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Trainee username") @PathVariable String username) {
        log.info("DELETE /api/trainees/{}", username);
        traineeService.delete(username);
        return ResponseEntity.ok().build();
    }

    // ---------- Endpoint 15: Activate / deactivate ----------
    @PatchMapping("/{username}/status")
    @PreAuthorize("#username == authentication.name")
    @Operation(summary = "Activate/deactivate trainee",
            description = "Changes the trainee's active status.")
    public ResponseEntity<Void> setActive(
            @Parameter(description = "Trainee username") @PathVariable String username,
            @Valid @RequestBody ActivateRequest request) {
        log.info("PATCH /api/trainees/{}/status -> {}", username, request.isActive());
        traineeService.setActive(username, request.isActive());
        return ResponseEntity.ok().build();
    }

    // ---------- Endpoint 11: Update trainee's trainers list ----------
    @PutMapping("/{username}/trainers")
    @PreAuthorize("#username == authentication.name")
    @Operation(summary = "Update trainee's trainers list")
    public ResponseEntity<List<TrainerShortResponse>> updateTrainers(
            @Parameter(description = "Trainee username") @PathVariable String username,
            @Valid @RequestBody UpdateTraineeTrainersRequest request) {
        log.info("PUT /api/trainees/{}/trainers", username);
        List<TrainerShortResponse> trainers = trainingService.updateTraineeTrainers(
                username, request.trainerUsernames());
        return ResponseEntity.ok(trainers);
    }

    // ---------- Endpoint 12: Get trainee trainings ----------
    @GetMapping("/{username}/trainings")
    @PreAuthorize("#username == authentication.name")
    @Operation(summary = "Get trainee trainings")
    public ResponseEntity<List<TrainingResponse>> getTrainings(
            @Parameter(description = "Trainee username") @PathVariable String username,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String trainerName,
            @RequestParam(required = false) TrainingTypeName trainingType) {
        log.info("GET /api/trainees/{}/trainings", username);
        List<TrainingResponse> trainings = trainingService.getTraineeTrainings(
                username, from, to, trainerName, trainingType);
        return ResponseEntity.ok(trainings);
    }
}