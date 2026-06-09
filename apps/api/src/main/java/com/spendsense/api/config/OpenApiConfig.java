package com.spendsense.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI spendsenseOpenApi(SpendSenseProperties properties) {
        return new OpenAPI()
                .info(new Info()
                        .title("SpendSense API")
                        .version(properties.api().version())
                        .description("Foundation API contract for SpendSense platform services.")
                        .contact(new Contact().name("SpendSense Engineering")))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
