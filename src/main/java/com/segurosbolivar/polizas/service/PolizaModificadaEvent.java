package com.segurosbolivar.polizas.service;

import java.util.Objects;

/**
 * Evento de dominio: el estado de una póliza o de uno de sus riesgos cambió.
 * Desacopla los casos de uso de sus efectos secundarios (sincronización con el CORE y,
 * en la arquitectura objetivo, notificaciones por correo/SMS).
 *
 * @param riesgoId riesgo afectado, o {@code null} si la operación es sobre la póliza
 */
public record PolizaModificadaEvent(Long polizaId, Long riesgoId, OperacionPoliza operacion) {

    public PolizaModificadaEvent {
        Objects.requireNonNull(polizaId, "polizaId");
        Objects.requireNonNull(operacion, "operacion");
    }

    public static PolizaModificadaEvent dePoliza(Long polizaId, OperacionPoliza operacion) {
        return new PolizaModificadaEvent(polizaId, null, operacion);
    }

    public static PolizaModificadaEvent deRiesgo(Long polizaId, Long riesgoId, OperacionPoliza operacion) {
        return new PolizaModificadaEvent(polizaId, riesgoId, operacion);
    }
}
