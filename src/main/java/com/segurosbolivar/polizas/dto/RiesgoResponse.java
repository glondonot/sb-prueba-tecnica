package com.segurosbolivar.polizas.dto;

import com.segurosbolivar.polizas.domain.EstadoRiesgo;
import com.segurosbolivar.polizas.domain.Riesgo;

import java.time.Instant;

public record RiesgoResponse(
        Long id,
        Long polizaId,
        String direccionInmueble,
        String ciudad,
        PersonaResponse arrendatario,
        PersonaResponse arrendador,
        EstadoRiesgo estado,
        Instant fechaCancelacion) {

    public static RiesgoResponse from(Riesgo riesgo) {
        return new RiesgoResponse(
                riesgo.getId(),
                riesgo.getPoliza().getId(),
                riesgo.getDireccionInmueble(),
                riesgo.getCiudad(),
                PersonaResponse.from(riesgo.getArrendatario()),
                PersonaResponse.from(riesgo.getArrendador()),
                riesgo.getEstado(),
                riesgo.getFechaCancelacion());
    }
}
