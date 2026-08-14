package com.epam.gym.controller;

import com.epam.gym.config.OpenApiConfig;
import com.epam.gym.dto.request.ActivateRequest;
import com.epam.gym.dto.request.TrainerRegistrationRequest;
import com.epam.gym.dto.request.UpdateTrainerRequest;
import com.epam.gym.dto.response.CredentialsResponse;
import com.epam.gym.dto.response.TrainerProfileResponse;
import com.epam.gym.dto.response.TrainerShortResponse;
import com.epam.gym.dto.response.TrainingResponse;
import com.epam.gym.service.TrainerService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trainers")
@Tag(name = "Trainer", description = "Trainer management endpoints")
public class TrainerController {

    private static final Logger log = LoggerFactory.getLogger(TrainerController.class);

    private final TrainerService trainerService;
    private final TrainingService trainingService;

    public TrainerController(TrainerService trainerService,
                             TrainingService trainingService) {
        this.trainerService = trainerService;
        this.trainingService = trainingService;
    }

    // ---------- Endpoint 2: Register trainer ----------
    @PostMapping
    @Operation(summary = "Register a new trainer",
            description = "Creates a trainer profile; username and password are auto-generated. Public endpoint.",
            security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainer registered, credentials returned"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<CredentialsResponse> register(@Valid @RequestBody TrainerRegistrationRequest request) {
        log.info("POST /api/trainers — register '{} {}'", request.firstName(), request.lastName());
        CredentialsResponse credentials = trainerService.create(
                request.firstName(), request.lastName(), request.specialization());
        return ResponseEntity.ok(credentials);
    }

    // ---------- Endpoint 8: Get trainer profile ----------
    @GetMapping("/{username}")
    @PreAuthorize("#username == authentication.name")
    @Operation(summary = "Get trainer profile",
            description = "Returns the trainer's profile. User can only access their own profile.")
    public ResponseEntity<TrainerProfileResponse> getProfile(
            @Parameter(description = "Trainer username") @PathVariable String username) {
        log.info("GET /api/trainers/{}", username);
        TrainerProfileResponse profile = trainerService.getProfile(username);
        return ResponseEntity.ok(profile);
    }

    // ---------- Endpoint 9: Update trainer profile ----------
    @PutMapping("/{username}")
    @PreAuthorize("#username == authentication.name")
    @Operation(summary = "Update trainer profile",
            description = "Updates the trainer's profile. Specialization is read-only.")
    public ResponseEntity<TrainerProfileResponse> update(
            @Parameter(description = "Trainer username") @PathVariable String username,
            @Valid @RequestBody UpdateTrainerRequest request) {
        log.info("PUT /api/trainers/{}", username);
        TrainerProfileResponse profile = trainerService.update(
                username,
                request.firstName(), request.lastName(), request.isActive());
        return ResponseEntity.ok(profile);
    }

    // ---------- Endpoint 16: Activate / deactivate ----------
    @PatchMapping("/{username}/status")
    @PreAuthorize("#username == authentication.name")
    @Operation(summary = "Activate/deactivate trainer")
    public ResponseEntity<Void> setActive(
            @Parameter(description = "Trainer username") @PathVariable String username,
            @Valid @RequestBody ActivateRequest request) {
        log.info("PATCH /api/trainers/{}/status -> {}", username, request.isActive());
        trainerService.setActive(username, request.isActive());
        return ResponseEntity.ok().build();
    }

    // ---------- Endpoint 10: Get not-assigned active trainers ----------
    @GetMapping("/unassigned")
    @Operation(summary = "Get unassigned active trainers",
            description = "Returns a list of active trainers who are not currently assigned to the authenticated trainee.")
    public ResponseEntity<List<TrainerShortResponse>> getUnassigned(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        // Extracts the username directly from the JWT token! No headers needed.
        String authUser = userDetails.getUsername();

        log.info("GET /api/trainers/unassigned (trainee='{}')", authUser);
        List<TrainerShortResponse> result = trainerService.findUnassignedTrainers(authUser);
        return ResponseEntity.ok(result);
    }

    // ---------- Endpoint 13: Get trainer trainings ----------
    @GetMapping("/{username}/trainings")
    @PreAuthorize("#username == authentication.name")
    @Operation(summary = "Get trainer trainings")
    public ResponseEntity<List<TrainingResponse>> getTrainings(
            @Parameter(description = "Trainer username") @PathVariable String username,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String traineeName) {
        log.info("GET /api/trainers/{}/trainings", username);
        List<TrainingResponse> trainings = trainingService.getTrainerTrainings(
                username, from, to, traineeName);
        return ResponseEntity.ok(trainings);
    }
}