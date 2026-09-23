package com.segurosbolivar.polizas.exception;

/** La operación no es válida para el estado actual del recurso (HTTP 409). */
public class EstadoInvalidoException extends RuntimeException {

    public EstadoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
