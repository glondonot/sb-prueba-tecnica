package com.segurosbolivar.polizas.service;

import com.segurosbolivar.polizas.domain.Poliza;
import com.segurosbolivar.polizas.domain.Riesgo;
import com.segurosbolivar.polizas.dto.CrearRiesgoRequest;
import com.segurosbolivar.polizas.dto.RiesgoResponse;
import com.segurosbolivar.polizas.exception.RecursoNoEncontradoException;
import com.segurosbolivar.polizas.repository.PolizaRepository;
import com.segurosbolivar.polizas.repository.RiesgoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso sobre riesgos. Toda regla se delega a la póliza (raíz del agregado). */
@Service
@Transactional
public class RiesgoService {

    private static final Logger log = LoggerFactory.getLogger(RiesgoService.class);

    private final PolizaRepository polizaRepository;
    private final RiesgoRepository riesgoRepository;

    public RiesgoService(PolizaRepository polizaRepository, RiesgoRepository riesgoRepository) {
        this.polizaRepository = polizaRepository;
        this.riesgoRepository = riesgoRepository;
    }

    public RiesgoResponse agregar(Long polizaId, CrearRiesgoRequest request) {
        Poliza poliza = polizaRepository.findByIdParaModificarRiesgos(polizaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Póliza", polizaId));
        Riesgo riesgo = new Riesgo(request.direccionInmueble().strip(), request.ciudad().strip(),
                request.arrendatario().toDomain(), request.arrendador().toDomain());

        poliza.agregarRiesgo(riesgo);
        riesgoRepository.saveAndFlush(riesgo);

        log.info("Riesgo agregado polizaId={} riesgoId={}", polizaId, riesgo.getId());
        return RiesgoResponse.from(riesgo);
    }

    public RiesgoResponse cancelar(Long riesgoId) {
        Riesgo riesgo = riesgoRepository.findByIdConPoliza(riesgoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Riesgo", riesgoId));
        Poliza poliza = polizaRepository.findByIdParaModificarRiesgos(riesgo.getPoliza().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Póliza", riesgo.getPoliza().getId()));

        poliza.cancelarRiesgo(riesgo);

        log.info("Riesgo cancelado polizaId={} riesgoId={}", poliza.getId(), riesgoId);
        return RiesgoResponse.from(riesgo);
    }
}
