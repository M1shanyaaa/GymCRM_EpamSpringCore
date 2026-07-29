package com.epam.gym.controller;

import com.epam.gym.dto.request.ChangePasswordRequest;
import com.epam.gym.dto.request.LoginRequest;
import com.epam.gym.dto.response.JwtResponse;
import com.epam.gym.security.NoAuth;
import com.epam.gym.security.TokenBlacklistService;
import com.epam.gym.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Authentication", description = "Authentication, logout and password management endpoints")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthController(AuthService authService, TokenBlacklistService tokenBlacklistService) {
        this.authService = authService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    // ---------- Endpoint 3: Login (POST) ----------
    @PostMapping("/login")
    @NoAuth
    @Operation(summary = "User login",
            description = "Authenticates user using credentials and returns a JWT token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully authenticated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/login — user '{}'", request.username());
        JwtResponse response = authService.login(request.username(), request.password());
        return ResponseEntity.ok(response);
    }

    // ---------- Logout (POST) ----------
    @PostMapping("/logout")
    @Operation(summary = "User logout",
            description = "Invalidates the current JWT token by adding it to the blacklist. Requires authentication.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully logged out"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated")
    })
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        log.info("POST /api/logout");
        String authHeader = request.getHeader("Authorization");

        // Extract token and add it to the blacklist
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            tokenBlacklistService.blacklistToken(token);
        }

        return ResponseEntity.ok().build();
    }

    // ---------- Endpoint 4: Change password ----------
    @PutMapping("/users/{username}/password")
    // Note: @NoAuth has been removed! This endpoint is now protected by JWT filter.
    @Operation(summary = "Change password",
            description = "Changes the user's password using the old password for verification. Requires JWT authentication.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Invalid old password or unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Forbidden access"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> changePassword(
            @Parameter(description = "Target username") @PathVariable String username,
            @Valid @RequestBody ChangePasswordRequest request) {
        log.info("PUT /api/users/{}/password", username);
        authService.changePassword(username, request.oldPassword(), request.newPassword());
        return ResponseEntity.ok().build();
    }
}