package com.dudev.datingapp.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация OpenAPI/Swagger для API‑gateway.
 *
 * <p>Определяет базовую информацию о сервисе и схему безопасности
 * {@code bearer-key} (JWT) для всех эндпоинтов.
 *
 * @since 1.0.0
 */

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Dating Service", version = "1.0.0"),
        security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearer-key")
)
@io.swagger.v3.oas.annotations.security.SecurityScheme(
        name = "bearer-key",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class SwaggerConfiguration {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI();
    }
}
