package com.segurosbolivar.polizas.exception;

/** El recurso solicitado no existe (HTTP 404). */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso, Long id) {
        super("%s con id %d no existe".formatted(recurso, id));
    }
}
