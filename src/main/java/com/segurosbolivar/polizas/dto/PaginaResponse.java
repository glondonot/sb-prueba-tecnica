package com.segurosbolivar.polizas.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Contrato de paginación estable, independiente de la serialización interna de Spring Data. */
public record PaginaResponse<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas) {

    public static <E, T> PaginaResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PaginaResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
