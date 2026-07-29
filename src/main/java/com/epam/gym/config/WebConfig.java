package com.epam.gym.config;

import com.epam.gym.security.AuthenticationInterceptor;
import com.epam.gym.service.AuthService;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration for the REST layer.
 * Registers the global {@link AuthenticationInterceptor} that enforces
 * authentication for every endpoint not explicitly marked with {@code @NoAuth}.
 *
 * Note: @EnableWebMvc is intentionally omitted to preserve Spring Boot's
 * auto-configuration for Jackson, static resources, and Swagger.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthService authService;

    public WebConfig(@Lazy AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthenticationInterceptor(authService))
                .excludePathPatterns(
                        // Exclude Swagger UI and API docs from authentication
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml",
                        "/webjars/**"
                );
    }
}