package com.segurosbolivar.polizas.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_API_KEY = "apiKey";

    @Bean
    OpenAPI polizasOpenApi(SeguridadProperties seguridad) {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Gestión de Pólizas")
                        .version("1.0.0")
                        .description("Pólizas de arrendamiento individuales y colectivas: consulta, renovación, "
                                + "cancelación y gestión de riesgos. Use el botón Authorize con el api-key."))
                .components(new Components().addSecuritySchemes(ESQUEMA_API_KEY, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name(seguridad.apiKeyHeader())))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_API_KEY));
    }
}
