package com.segurosbolivar.polizas.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/** Parámetros de renovación de pólizas ({@code polizas.renovacion.*}). */
@Validated
@ConfigurationProperties(prefix = "polizas.renovacion")
public record RenovacionProperties(
        @NotNull @DecimalMin("0.0") BigDecimal ipcPorcentaje) {
}
