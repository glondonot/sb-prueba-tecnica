package com.segurosbolivar.polizas.integration;

/** Falla técnica al comunicarse con el CORE (no disponible, timeout o respuesta de error). */
public class CoreIntegracionException extends RuntimeException {

    public CoreIntegracionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
