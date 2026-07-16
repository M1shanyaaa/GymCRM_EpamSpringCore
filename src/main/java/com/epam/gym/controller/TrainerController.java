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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trainers")
@Tag(name = "Trainer", description = "Trainer management endpoints")
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
    @Operation(summary = "Register a new trainer",
            description = "Creates a trainer profile; username and password are auto-generated. No authentication required.")
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
    @Operation(summary = "Get trainer profile",
            description = "Returns the authenticated trainer's profile including assigned trainees.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "404", description = "Trainer not found")
    })
    public ResponseEntity<TrainerProfileResponse> getProfile(
            @Parameter(description = "Trainer username") @PathVariable String username,
            @Parameter(description = "Auth username", required = true) @RequestHeader(AUTH_USER) String authUser,
            @Parameter(description = "Auth password", required = true) @RequestHeader(AUTH_PASS) String authPass) {
        log.info("GET /api/trainers/{}", username);
        TrainerProfileResponse profile = trainerService.getProfile(authUser, authPass);
        return ResponseEntity.ok(profile);
    }

    // ---------- Endpoint 9: Update trainer profile ----------
    @PutMapping("/{username}")
    @Operation(summary = "Update trainer profile",
            description = "Updates the trainer's profile. Specialization is read-only and username cannot be changed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "404", description = "Trainer not found")
    })
    public ResponseEntity<TrainerProfileResponse> update(
            @Parameter(description = "Trainer username") @PathVariable String username,
            @Valid @RequestBody UpdateTrainerRequest request,
            @Parameter(description = "Auth username", required = true) @RequestHeader(AUTH_USER) String authUser,
            @Parameter(description = "Auth password", required = true) @RequestHeader(AUTH_PASS) String authPass) {
        log.info("PUT /api/trainers/{}", username);
        TrainerProfileResponse profile = trainerService.update(
                authUser, authPass,
                request.firstName(), request.lastName(), request.isActive());
        return ResponseEntity.ok(profile);
    }

    // ---------- Endpoint 16: Activate / deactivate ----------
    @PatchMapping("/{username}/status")
    @Operation(summary = "Activate/deactivate trainer",
            description = "Changes the trainer's active status. Not idempotent.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status changed"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "404", description = "Trainer not found")
    })
    public ResponseEntity<Void> setActive(
            @Parameter(description = "Trainer username") @PathVariable String username,
            @Valid @RequestBody ActivateRequest request,
            @Parameter(description = "Auth username", required = true) @RequestHeader(AUTH_USER) String authUser,
            @Parameter(description = "Auth password", required = true) @RequestHeader(AUTH_PASS) String authPass) {
        log.info("PATCH /api/trainers/{}/status -> {}", username, request.isActive());
        trainerService.setActive(authUser, authPass, request.isActive());
        return ResponseEntity.ok().build();
    }

    // ---------- Endpoint 10: Get not-assigned active trainers ----------
    @GetMapping("/unassigned")
    @Operation(summary = "Get unassigned active trainers",
            description = "Returns a list of active trainers who are not currently assigned to the authenticated trainee.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of unassigned trainers returned"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<List<TrainerShortResponse>> getUnassigned(
            @Parameter(description = "Auth username (acting as Trainee)", required = true) @RequestHeader(AUTH_USER) String authUser,
            @Parameter(description = "Auth password", required = true) @RequestHeader(AUTH_PASS) String authPass) {
        log.info("GET /api/trainers/unassigned (trainee='{}')", authUser);
        List<TrainerShortResponse> result =
                trainerService.findUnassignedTrainers(authUser, authPass);
        return ResponseEntity.ok(result);
    }

    // ---------- Endpoint 13: Get trainer trainings ----------
    @GetMapping("/{username}/trainings")
    @Operation(summary = "Get trainer trainings",
            description = "Returns the trainer's trainings with optional filters by date and trainee name.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainings returned"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "404", description = "Trainer not found")
    })
    public ResponseEntity<List<TrainingResponse>> getTrainings(
            @Parameter(description = "Trainer username") @PathVariable String username,
            @Parameter(description = "Period from (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Period to (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Filter by trainee name") @RequestParam(required = false) String traineeName,
            @Parameter(description = "Auth username", required = true) @RequestHeader(AUTH_USER) String authUser,
            @Parameter(description = "Auth password", required = true) @RequestHeader(AUTH_PASS) String authPass) {
        log.info("GET /api/trainers/{}/trainings", username);
        List<TrainingResponse> trainings = trainingService.getTrainerTrainings(
                authUser, authPass, from, to, traineeName);
        return ResponseEntity.ok(trainings);
    }
}