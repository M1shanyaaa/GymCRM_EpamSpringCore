package com.epam.gym.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    /**
     * Name of the security scheme documenting the JWT Authorization header.
     */
    public static final String BEARER_AUTH_SCHEME = "BearerAuth";

    @Bean
    public OpenAPI gymOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gym CRM REST API")
                        .version("1.0")
                        .description("Spring MVC Gym CRM — trainees, trainers, trainings with JWT Security"))
                // 1. Apply the security requirement globally to all endpoints.
                // (Endpoints explicitly ignoring security can still bypass this via Spring Security config).
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME))

                // 2. Define the security scheme as HTTP Bearer.
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH_SCHEME,
                                new SecurityScheme()
                                        .name(BEARER_AUTH_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your bare JWT token here. Swagger will automatically prepend 'Bearer '.")));
    }
}