package com.epam.gym.controller;

import com.epam.gym.dto.request.ChangePasswordRequest;
import com.epam.gym.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Authentication", description = "Authentication and password management endpoints")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ---------- Endpoint 3: Login (GET) ----------
    @GetMapping("/login")
    @Operation(summary = "User login",
            description = "Authenticates user using headers and returns 200 OK if credentials are correct.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully authenticated"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<Void> login(
            @Parameter(description = "Auth username", required = true) @RequestHeader("X-Auth-Username") String username,
            @Parameter(description = "Auth password", required = true) @RequestHeader("X-Auth-Password") String password) {
        log.info("GET /api/login — user '{}'", username);
        authService.authenticate(username, password);
        return ResponseEntity.ok().build();
    }

    // ---------- Endpoint 4: Change password ----------
    @PutMapping("/users/{username}/password")
    @Operation(summary = "Change password",
            description = "Changes the user's password using the old password for verification.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Invalid old password or unauthenticated"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> changePassword(
            @Parameter(description = "Target username") @PathVariable String username,
            @Valid @RequestBody ChangePasswordRequest request) {
        log.info("PUT /api/users/{}/password", username);
        authService.changePassword(
                request.username(), request.oldPassword(), request.newPassword());
        return ResponseEntity.ok().build();
    }
}