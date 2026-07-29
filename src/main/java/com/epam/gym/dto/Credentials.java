package com.epam.gym.dto;

/**
 * Caller credentials extracted from request headers
 * Will be replaced by Spring Security principal later.
 */
public record Credentials(String username, String password) {
}