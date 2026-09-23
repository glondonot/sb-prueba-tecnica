package com.segurosbolivar.polizas.controller;

import com.segurosbolivar.polizas.dto.CrearRiesgoRequest;
import com.segurosbolivar.polizas.dto.RiesgoResponse;
import com.segurosbolivar.polizas.service.RiesgoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Riesgos", description = "Gestión de riesgos de pólizas colectivas")
public class RiesgoController {

    private final RiesgoService riesgoService;

    public RiesgoController(RiesgoService riesgoService) {
        this.riesgoService = riesgoService;
    }

    @PostMapping("/polizas/{id}/riesgos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agregar riesgo a una póliza",
            description = "Solo permitido para pólizas COLECTIVAS que no estén canceladas")
    public RiesgoResponse agregar(@PathVariable Long id, @Valid @RequestBody CrearRiesgoRequest request) {
        return riesgoService.agregar(id, request);
    }

    @PostMapping("/riesgos/{id}/cancelar")
    @Operation(summary = "Cancelar riesgo",
            description = "Cancela un riesgo de una póliza colectiva. El riesgo único de una póliza individual "
                    + "no se cancela por separado: se debe cancelar la póliza.")
    public RiesgoResponse cancelar(@PathVariable Long id) {
        return riesgoService.cancelar(id);
    }
}
