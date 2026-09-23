package com.segurosbolivar.polizas.dto;

import com.segurosbolivar.polizas.domain.EstadoPoliza;
import com.segurosbolivar.polizas.domain.Poliza;
import com.segurosbolivar.polizas.domain.TipoPoliza;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PolizaResponse(
        Long id,
        String numero,
        TipoPoliza tipo,
        EstadoPoliza estado,
        PersonaResponse tomador,
        LocalDate fechaInicioVigencia,
        LocalDate fechaFinVigencia,
        int mesesVigencia,
        BigDecimal valorCanon,
        BigDecimal valorPrima,
        Instant fechaCancelacion) {

    public static PolizaResponse from(Poliza poliza) {
        return new PolizaResponse(
                poliza.getId(),
                poliza.getNumero(),
                poliza.getTipo(),
                poliza.getEstado(),
                PersonaResponse.from(poliza.getTomador()),
                poliza.getFechaInicioVigencia(),
                poliza.getFechaFinVigencia(),
                poliza.getMesesVigencia(),
                poliza.getValorCanon(),
                poliza.getValorPrima(),
                poliza.getFechaCancelacion());
    }
}
