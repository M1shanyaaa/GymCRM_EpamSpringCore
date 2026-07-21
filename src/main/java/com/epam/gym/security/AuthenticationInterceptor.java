package com.epam.gym.security;

import com.epam.gym.exception.AuthenticationException;
import com.epam.gym.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

/**
 * Central authentication gate for the whole API.
 * <p>
 * Reads {@code X-Auth-Username} / {@code X-Auth-Password} headers and verifies
 * them via {@link AuthService#authenticate(String, String)} BEFORE the request
 * reaches any controller method.
 * <p>
 * If the request path contains a {@code {username}} path variable, that value
 * is used as the identity instead of the {@code X-Auth-Username} header — this
 * guarantees the credentials always match the resource addressed by the URL
 * (see the earlier "path variable ignored" bug) and removes the need to send
 * a redundant username header on those endpoints.
 * <p>
 * Methods annotated with {@link NoAuth} are skipped entirely.
 */
public class AuthenticationInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationInterceptor.class);

    private static final String AUTH_USER_HEADER = "X-Auth-Username";
    private static final String AUTH_PASS_HEADER = "X-Auth-Password";
    private static final String PATH_USERNAME_VAR = "username";

    private final AuthService authService;

    public AuthenticationInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        if (handlerMethod.hasMethodAnnotation(NoAuth.class)) {
            return true;
        }

        String username = resolveUsername(request);
        String password = request.getHeader(AUTH_PASS_HEADER);

        if (username == null || username.isBlank()) {
            throw new AuthenticationException("Missing '" + AUTH_USER_HEADER + "' header");
        }
        if (password == null || password.isBlank()) {
            throw new AuthenticationException("Missing '" + AUTH_PASS_HEADER + "' header");
        }

        authService.authenticate(username, password);
        log.debug("Authenticated request for user '{}'", username);
        return true;
    }

    @SuppressWarnings("unchecked")
    private String resolveUsername(HttpServletRequest request) {
        Map<String, String> pathVariables =
                (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (pathVariables != null && pathVariables.containsKey(PATH_USERNAME_VAR)) {
            return pathVariables.get(PATH_USERNAME_VAR);
        }
        return request.getHeader(AUTH_USER_HEADER);
    }
}