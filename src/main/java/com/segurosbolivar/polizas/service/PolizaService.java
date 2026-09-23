package com.segurosbolivar.polizas.service;

import com.segurosbolivar.polizas.domain.EstadoPoliza;
import com.segurosbolivar.polizas.domain.Poliza;
import com.segurosbolivar.polizas.domain.TipoPoliza;
import com.segurosbolivar.polizas.dto.PaginaResponse;
import com.segurosbolivar.polizas.dto.PolizaResponse;
import com.segurosbolivar.polizas.dto.RiesgoResponse;
import com.segurosbolivar.polizas.exception.RecursoNoEncontradoException;
import com.segurosbolivar.polizas.repository.PolizaRepository;
import com.segurosbolivar.polizas.repository.PolizaSpecifications;
import com.segurosbolivar.polizas.repository.RiesgoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Casos de uso sobre pólizas. Orquesta transacción, persistencia e integración;
 * las reglas de negocio viven en el agregado {@link Poliza}.
 */
@Service
@Transactional(readOnly = true)
public class PolizaService {

    private static final Logger log = LoggerFactory.getLogger(PolizaService.class);

    private final PolizaRepository polizaRepository;
    private final RiesgoRepository riesgoRepository;
    private final IpcProvider ipcProvider;
    private final ApplicationEventPublisher eventPublisher;

    public PolizaService(PolizaRepository polizaRepository, RiesgoRepository riesgoRepository,
                         IpcProvider ipcProvider, ApplicationEventPublisher eventPublisher) {
        this.polizaRepository = polizaRepository;
        this.riesgoRepository = riesgoRepository;
        this.ipcProvider = ipcProvider;
        this.eventPublisher = eventPublisher;
    }

    public PaginaResponse<PolizaResponse> listar(TipoPoliza tipo, EstadoPoliza estado, Pageable pageable) {
        var pagina = polizaRepository.findAll(PolizaSpecifications.conFiltros(tipo, estado), pageable);
        return PaginaResponse.from(pagina, PolizaResponse::from);
    }

    public List<RiesgoResponse> listarRiesgos(Long polizaId) {
        if (!polizaRepository.existsById(polizaId)) {
            throw new RecursoNoEncontradoException("Póliza", polizaId);
        }
        return riesgoRepository.findByPolizaIdOrderByIdAsc(polizaId).stream()
                .map(RiesgoResponse::from)
                .toList();
    }

    @Transactional
    public PolizaResponse renovar(Long polizaId) {
        Poliza poliza = buscar(polizaId);
        BigDecimal canonAnterior = poliza.getValorCanon();
        BigDecimal ipc = ipcProvider.ipcVigente();

        poliza.renovar(ipc);
        eventPublisher.publishEvent(PolizaModificadaEvent.dePoliza(poliza.getId(), OperacionPoliza.RENOVACION_POLIZA));

        log.info("Póliza renovada polizaId={} numero={} ipc={} canonAnterior={} canonNuevo={}",
                poliza.getId(), poliza.getNumero(), ipc, canonAnterior, poliza.getValorCanon());
        return PolizaResponse.from(poliza);
    }

    @Transactional
    public PolizaResponse cancelar(Long polizaId) {
        Poliza poliza = buscar(polizaId);

        poliza.cancelar();
        eventPublisher.publishEvent(PolizaModificadaEvent.dePoliza(poliza.getId(), OperacionPoliza.CANCELACION_POLIZA));

        log.info("Póliza cancelada polizaId={} numero={}", poliza.getId(), poliza.getNumero());
        return PolizaResponse.from(poliza);
    }

    private Poliza buscar(Long polizaId) {
        return polizaRepository.findById(polizaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Póliza", polizaId));
    }
}
