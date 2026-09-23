package com.segurosbolivar.polizas.dto;

import com.segurosbolivar.polizas.domain.Persona;

public record PersonaResponse(String documento, String nombre) {

    public static PersonaResponse from(Persona persona) {
        return new PersonaResponse(persona.getDocumento(), persona.getNombre());
    }
}
