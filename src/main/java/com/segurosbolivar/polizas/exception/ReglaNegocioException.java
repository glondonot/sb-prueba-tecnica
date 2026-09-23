package com.segurosbolivar.polizas.exception;

/** La solicitud es válida sintácticamente pero viola una regla de negocio (HTTP 422). */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
