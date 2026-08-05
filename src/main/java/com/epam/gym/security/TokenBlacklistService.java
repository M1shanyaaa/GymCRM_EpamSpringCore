package com.epam.gym.security;

import io.jsonwebtoken.JwtException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    // MEDIUM FIX: Changed from Set<String> to Map<String, Date> to track expiration
    private final Map<String, Date> blacklistedTokens = new ConcurrentHashMap<>();

    private final JwtService jwtService;

    // Injecting JwtService to extract the expiration date from the token
    public TokenBlacklistService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public void blacklistToken(String token) {
        try {
            Date expirationDate = jwtService.getExpiration(token);
            blacklistedTokens.put(token, expirationDate);
            log.debug("Token blacklisted until: {}", expirationDate);
        } catch (JwtException e) {
            log.debug("Attempted to blacklist an invalid or already expired token");
        }
    }

    public boolean isBlacklisted(String token) {
        return blacklistedTokens.containsKey(token);
    }

    // Memory Leak Prevention
    @Scheduled(fixedRate = 3600000)
    public void cleanUpExpiredTokens() {
        Date now = new Date();
        int initialSize = blacklistedTokens.size();

        // Remove entry if its expiration date is before the current time
        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue().before(now));

        int removed = initialSize - blacklistedTokens.size();
        if (removed > 0) {
            log.info("Cleared {} expired tokens from blacklist", removed);
        }
    }
}