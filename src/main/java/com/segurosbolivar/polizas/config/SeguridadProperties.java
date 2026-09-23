package com.segurosbolivar.polizas.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Seguridad mínima por API key ({@code polizas.seguridad.*}).
 * El valor se inyecta por entorno (API_KEY); nunca debería quedar fijo en el código en producción.
 */
@Validated
@ConfigurationProperties(prefix = "polizas.seguridad")
public record SeguridadProperties(
        @NotBlank String apiKeyHeader,
        @NotBlank String apiKey) {
}
