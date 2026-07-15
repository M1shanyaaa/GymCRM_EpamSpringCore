package com.epam.gym.controller;

import com.epam.gym.dto.request.ChangePasswordRequest;
import com.epam.gym.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ---------- Endpoint 3: Login (GET) ----------
    @GetMapping("/login")
    public ResponseEntity<Void> login(
            @RequestHeader("X-Auth-Username") String username,
            @RequestHeader("X-Auth-Password") String password) {
        log.info("GET /api/login — user '{}'", username);
        authService.authenticate(username, password);
        return ResponseEntity.ok().build();
    }

    // ---------- Endpoint 4: Change password ----------
    @PutMapping("/users/{username}/password")
    public ResponseEntity<Void> changePassword(
            @PathVariable String username,
            @Valid @RequestBody ChangePasswordRequest request) {
        log.info("PUT /api/users/{}/password", username);
        authService.changePassword(
                request.username(), request.oldPassword(), request.newPassword());
        return ResponseEntity.ok().build();
    }
}