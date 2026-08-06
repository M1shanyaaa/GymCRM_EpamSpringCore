package com.epam.gym.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.Setter;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 3;
    private static final int BLOCK_DURATION_MINUTES = 5;

    private final Map<String, LoginAttempt> attemptsCache = new ConcurrentHashMap<>();

    public void loginSucceeded(String username) {
        attemptsCache.remove(username);
    }

    public void loginFailed(String username) {
        attemptsCache.compute(username, (key, attempt) -> {
            if (attempt == null) {
                return new LoginAttempt(1, null, LocalDateTime.now());
            }
            attempt.increment();
            attempt.setLastAttemptTime(LocalDateTime.now());
            if (attempt.getAttempts() >= MAX_ATTEMPTS && attempt.getLockTime() == null) {
                attempt.setLockTime(LocalDateTime.now());
            }
            return attempt;
        });
    }

    public boolean isBlocked(String username) {
        LoginAttempt attempt = attemptsCache.get(username);

        if (attempt == null) {
            return false;
        }

        if (attempt.getAttempts() >= MAX_ATTEMPTS) {
            if (attempt.getLockTime() != null &&
                    attempt.getLockTime().plusMinutes(BLOCK_DURATION_MINUTES).isAfter(LocalDateTime.now())) {
                return true;
            } else {
                attemptsCache.remove(username);
                return false;
            }
        }
        return false;
    }

    @Scheduled(fixedRate = 1800000)
    public void clearOldAttempts() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        attemptsCache.entrySet().removeIf(entry ->
                entry.getValue().getLastAttemptTime().isBefore(threshold)
        );
    }

    @Getter
    private static class LoginAttempt {
        private int attempts;
        @Setter
        private LocalDateTime lockTime;

        @Setter
        private LocalDateTime lastAttemptTime;

        public LoginAttempt(int attempts, LocalDateTime lockTime, LocalDateTime lastAttemptTime) {
            this.attempts = attempts;
            this.lockTime = lockTime;
            this.lastAttemptTime = lastAttemptTime;
        }

        public void increment() {
            this.attempts++;
        }

    }
}