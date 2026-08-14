package com.epam.gym.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private UserDetails userDetails;

    // Valid Base64 string for HS256 (minimum 256 bits)
    private static final String MOCK_SECRET = "NmQ4Zjg3ZWM1YTEyNDlkZWE4Y2NiZThjZDFkMWM1MzI2ZDhmODdlYzVhMTI0OWRlYThjY2JlOGNkMWQxYzUzMg==";
    private static final long MOCK_EXPIRATION = 3600000L; // 1 hour in milliseconds

    @BeforeEach
    void setUp() {
        // Setup default mock behavior for properties used in most tests
        when(jwtProperties.getSecret()).thenReturn(MOCK_SECRET);

        jwtService = new JwtService(jwtProperties);
    }

    @Test
    void generateToken_shouldReturnValidString() {
        when(jwtProperties.getExpiration()).thenReturn(MOCK_EXPIRATION);
        when(userDetails.getUsername()).thenReturn("John.Doe");

        String token = jwtService.generateToken(userDetails);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        // JWT consists of three parts separated by a dot
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void extractUsername_shouldReturnCorrectUsername() {
        when(jwtProperties.getExpiration()).thenReturn(MOCK_EXPIRATION);
        when(userDetails.getUsername()).thenReturn("Bruce.Wayne");

        String token = jwtService.generateToken(userDetails);
        String extractedUsername = jwtService.extractUsername(token);

        assertEquals("Bruce.Wayne", extractedUsername);
    }

    @Test
    void isTokenValid_shouldReturnTrue_whenTokenBelongsToUser() {
        when(jwtProperties.getExpiration()).thenReturn(MOCK_EXPIRATION);
        when(userDetails.getUsername()).thenReturn("Clark.Kent");

        String token = jwtService.generateToken(userDetails);
        boolean isValid = jwtService.isTokenValid(token, userDetails);

        assertTrue(isValid);
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenUsernameDoesNotMatch() {
        when(jwtProperties.getExpiration()).thenReturn(MOCK_EXPIRATION);
        when(userDetails.getUsername()).thenReturn("Clark.Kent");

        String token = jwtService.generateToken(userDetails);

        // Create a different mocked user to simulate a mismatch
        UserDetails wrongUser = org.mockito.Mockito.mock(UserDetails.class);
        when(wrongUser.getUsername()).thenReturn("Lex.Luthor");

        boolean isValid = jwtService.isTokenValid(token, wrongUser);

        assertFalse(isValid);
    }

    @Test
    void getExpiration_shouldReturnFutureDate() {
        when(jwtProperties.getExpiration()).thenReturn(MOCK_EXPIRATION);
        when(userDetails.getUsername()).thenReturn("Diana.Prince");

        String token = jwtService.generateToken(userDetails);
        Date expirationDate = jwtService.getExpiration(token);

        assertNotNull(expirationDate);
        assertTrue(expirationDate.after(new Date()));
    }

    @Test
    void extractUsername_shouldThrowExpiredJwtException_whenTokenIsExpired() {
        // Set negative expiration time so the token expires immediately
        when(jwtProperties.getExpiration()).thenReturn(-1000L);
        when(userDetails.getUsername()).thenReturn("Barry.Allen");

        String expiredToken = jwtService.generateToken(userDetails);

        // JJWT throws an exception during the parsing stage of an expired token
        assertThrows(ExpiredJwtException.class, () -> jwtService.extractUsername(expiredToken));
    }

    @Test
    void extractUsername_shouldThrowSignatureException_whenSignatureIsInvalid() {
        when(jwtProperties.getExpiration()).thenReturn(MOCK_EXPIRATION);
        when(userDetails.getUsername()).thenReturn("Arthur.Curry");

        String token = jwtService.generateToken(userDetails);
        // Tamper with the last character of the signature to make it invalid
        String tamperedToken = token.substring(0, token.length() - 1) + "a";

        assertThrows(SignatureException.class, () -> jwtService.extractUsername(tamperedToken));
    }

    @Test
    void extractUsername_shouldThrowMalformedJwtException_whenTokenIsMalformed() {
        String invalidToken = "this.is.not.a.valid.jwt";

        assertThrows(MalformedJwtException.class, () -> jwtService.extractUsername(invalidToken));
    }
}