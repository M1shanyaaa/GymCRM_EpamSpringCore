package com.epam.gym.workload.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private String secret;

    @BeforeEach
    void setUp() {
        // 256-bit base64 secret
        secret = "dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtMTIzNA==";
        JwtProperties props = new JwtProperties();
        props.setSecret(secret);
        props.setExpiration(3600000);
        jwtService = new JwtService(props);
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    private String buildToken(String subject, long ttlMillis) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + ttlMillis))
                .signWith(key())
                .compact();
    }

    @Test
    void extractUsername_returnsSubject() {
        String token = buildToken("john.doe", 3600000);
        assertThat(jwtService.extractUsername(token)).isEqualTo("john.doe");
    }

    @Test
    void isTokenValid_validToken_true() {
        String token = buildToken("john.doe", 3600000);
        UserDetails user = new User("john.doe", "", Collections.emptyList());
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void isTokenValid_expiredToken_throwsOrFalse() {
        String token = buildToken("john.doe", -1000); // вже прострочений
        UserDetails user = new User("john.doe", "", Collections.emptyList());

        // JJWT кидає ExpiredJwtException при парсингу
        assertThatThrownBy(() -> jwtService.isTokenValid(token, user))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    void extractUsername_tamperedSignature_throws() {
        String token = buildToken("john.doe", 3600000);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwtService.extractUsername(tampered))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }
}