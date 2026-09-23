package com.segurosbolivar.polizas.integration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.segurosbolivar.polizas.service.PolizaModificadaEvent;

/**
 * Contrato del servicio de edición del CORE. Mantiene los campos del enunciado
 * ({@code evento}, {@code polizaId}) y agrega contexto opcional de la operación.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CoreEdicionEvento(String evento, Long polizaId, Long riesgoId, String operacion) {

    static final String ACTUALIZACION = "ACTUALIZACION";

    public static CoreEdicionEvento desde(PolizaModificadaEvent evento) {
        return new CoreEdicionEvento(ACTUALIZACION, evento.polizaId(), evento.riesgoId(), evento.operacion().name());
    }
}
