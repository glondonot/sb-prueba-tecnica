package com.segurosbolivar.polizas.integration;

/**
 * Puerto de salida hacia el servicio agnóstico de edición (capa media WebLogic) que mantiene
 * actualizado el CORE de seguros. Los servicios no conocen el protocolo ni el formato del CORE:
 * cambiar el mock por el servicio real (REST, SOAP, cola) solo implica otro adaptador.
 */
public interface CoreEdicionClient {

    /**
     * Informa al CORE una edición de póliza o riesgo.
     *
     * @throws CoreIntegracionException si el CORE no está disponible o rechaza el evento
     */
    void enviarEdicion(CoreEdicionEvento evento);
}
