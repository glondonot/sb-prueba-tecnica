package com.segurosbolivar.polizas.dto;

import com.segurosbolivar.polizas.domain.Persona;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PersonaRequest(
        @NotBlank @Pattern(regexp = "\\d{5,20}", message = "debe contener entre 5 y 20 dígitos") String documento,
        @NotBlank @Size(max = 150) String nombre) {

    public Persona toDomain() {
        return new Persona(documento, nombre.strip());
    }
}
