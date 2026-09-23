package com.segurosbolivar.polizas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearRiesgoRequest(
        @NotBlank @Size(max = 200) String direccionInmueble,
        @NotBlank @Size(max = 80) String ciudad,
        @NotNull @Valid PersonaRequest arrendatario,
        @NotNull @Valid PersonaRequest arrendador) {
}
