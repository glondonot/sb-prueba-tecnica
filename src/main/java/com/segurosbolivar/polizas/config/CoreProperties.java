package com.segurosbolivar.polizas.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Conexión con el servicio agnóstico de edición del CORE ({@code polizas.core.*}).
 * Por defecto apunta al mock expuesto por esta misma aplicación.
 */
@Validated
@ConfigurationProperties(prefix = "polizas.core")
public record CoreProperties(
        @NotBlank String url) {
}
