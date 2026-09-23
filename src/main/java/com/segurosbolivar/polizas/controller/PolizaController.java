package com.segurosbolivar.polizas.controller;

import com.segurosbolivar.polizas.domain.EstadoPoliza;
import com.segurosbolivar.polizas.domain.TipoPoliza;
import com.segurosbolivar.polizas.dto.PaginaResponse;
import com.segurosbolivar.polizas.dto.PolizaResponse;
import com.segurosbolivar.polizas.dto.RiesgoResponse;
import com.segurosbolivar.polizas.service.PolizaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/polizas")
@Tag(name = "Pólizas", description = "Consulta, renovación y cancelación de pólizas")
public class PolizaController {

    private final PolizaService polizaService;

    public PolizaController(PolizaService polizaService) {
        this.polizaService = polizaService;
    }

    @GetMapping
    @Operation(summary = "Listar pólizas", description = "Filtros opcionales por tipo y estado, con paginación")
    public PaginaResponse<PolizaResponse> listar(
            @Parameter(description = "INDIVIDUAL o COLECTIVA") @RequestParam(required = false) TipoPoliza tipo,
            @Parameter(description = "ACTIVA, RENOVADA o CANCELADA") @RequestParam(required = false) EstadoPoliza estado,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return polizaService.listar(tipo, estado, pageable);
    }

    @GetMapping("/{id}/riesgos")
    @Operation(summary = "Listar los riesgos de una póliza")
    public List<RiesgoResponse> listarRiesgos(@PathVariable Long id) {
        return polizaService.listarRiesgos(id);
    }

    @PostMapping("/{id}/renovar")
    @Operation(summary = "Renovar póliza",
            description = "Incrementa canon y prima según el IPC vigente, extiende la vigencia por el mismo periodo "
                    + "y deja la póliza en estado RENOVADA. No aplica para pólizas canceladas.")
    public PolizaResponse renovar(@PathVariable Long id) {
        return polizaService.renovar(id);
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar póliza", description = "Cancela la póliza y todos sus riesgos activos")
    public PolizaResponse cancelar(@PathVariable Long id) {
        return polizaService.cancelar(id);
    }
}
