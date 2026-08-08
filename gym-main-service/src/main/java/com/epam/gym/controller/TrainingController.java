package com.epam.gym.controller;

import com.epam.gym.config.OpenApiConfig;
import com.epam.gym.dto.request.AddTrainingRequest;
import com.epam.gym.dto.response.TrainingTypeResponse;
import com.epam.gym.service.TrainingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trainings")
@Tag(name = "Training", description = "Training session and type management endpoints")
public class TrainingController {

    private static final Logger log = LoggerFactory.getLogger(TrainingController.class);

    private final TrainingService trainingService;

    public TrainingController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    // ---------- Endpoint 14: Add training ----------
    @PostMapping
    @PreAuthorize("#request.traineeUsername() == authentication.name")
    @Operation(summary = "Add a new training session",
            description = "Creates a new training record associating a trainee and a trainer. Requires JWT authentication.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Training added successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Forbidden access"),
            @ApiResponse(responseCode = "404", description = "Trainer or Trainee not found")
    })
    public ResponseEntity<Void> addTraining(@Valid @RequestBody AddTrainingRequest request) {
        log.info("POST /api/trainings — add training '{}'", request.trainingName());

        // Removed request.username() and request.password()
        trainingService.addTraining(
                request.traineeUsername(),
                request.trainerUsername(),
                request.trainingName(),
                request.trainingDate(),
                request.trainingDuration()
        );

        return ResponseEntity.ok().build();
    }

    // ---------- Endpoint 17: Get training types ----------
    @GetMapping("/types")
    @Operation(summary = "Get all training types",
            description = "Returns a constant list of all available training types in the system. Public endpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of training types returned")
    })
    public ResponseEntity<List<TrainingTypeResponse>> getTrainingTypes() {
        log.info("GET /api/trainings/types");
        return ResponseEntity.ok(trainingService.getTrainingTypes());
    }
}