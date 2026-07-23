package com.epam.gym.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    /**
     * Name of the security scheme documenting the {@code X-Auth-Password}
     * header required by authenticated endpoints. Applied selectively via
     * {@code @SecurityRequirement(name = OpenApiConfig.AUTH_PASSWORD_SCHEME)}
     * on controller classes whose endpoints require it — intentionally NOT
     * set globally, since public/{@code @NoAuth} endpoints (registration,
     * login, addTraining, getTrainingTypes) must not advertise an auth
     * requirement they don't actually have.
     */
    public static final String AUTH_PASSWORD_SCHEME = "AuthPassword";

    @Bean
    public OpenAPI gymOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gym CRM REST API")
                        .version("1.0")
                        .description("Spring MVC Gym CRM — trainees, trainers, trainings"))
                .components(new Components()
                        .addSecuritySchemes(AUTH_PASSWORD_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-Auth-Password")
                                        .description("Password of the user identified by the {username} path "
                                                + "variable. Verified globally by AuthenticationInterceptor "
                                                + "before the request reaches the controller.")));
    }
}