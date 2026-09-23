package com.segurosbolivar.polizas.controller;

import com.segurosbolivar.polizas.dto.CoreEventoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simula el servicio agnóstico de edición del CORE (capa media WebLogic).
 * Su único propósito es dejar registro en logs de que la operación se intentó enviar al CORE.
 */
@RestController
@RequestMapping("/core-mock")
@Tag(name = "CORE (mock)", description = "Simulación del servicio de edición del CORE transaccional")
public class CoreMockController {

    private static final Logger log = LoggerFactory.getLogger(CoreMockController.class);

    @PostMapping("/evento")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Registrar evento de edición en el CORE (mock)")
    public void registrarEvento(@Valid @RequestBody CoreEventoRequest request) {
        log.info("[CORE-MOCK] Evento recibido: evento={} polizaId={} riesgoId={} operacion={}",
                request.evento(), request.polizaId(), request.riesgoId(), request.operacion());
    }
}
