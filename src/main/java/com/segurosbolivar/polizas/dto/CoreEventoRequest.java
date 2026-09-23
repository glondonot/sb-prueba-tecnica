package com.segurosbolivar.polizas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Cuerpo recibido por el mock del CORE: {@code {"evento": "ACTUALIZACION", "polizaId": 555}}. */
public record CoreEventoRequest(
        @NotBlank String evento,
        @NotNull @Positive Long polizaId,
        Long riesgoId,
        String operacion) {
}
