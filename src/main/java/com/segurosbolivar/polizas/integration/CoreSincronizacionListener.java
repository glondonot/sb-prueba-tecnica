package com.segurosbolivar.polizas.integration;

import com.segurosbolivar.polizas.service.PolizaModificadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sincroniza con el CORE cada cambio de estado de pólizas y riesgos.
 *
 * <p>Se ejecuta DESPUÉS del commit: el CORE nunca recibe un cambio que luego se revierte en la
 * base de datos local, y la transacción no queda abierta esperando a un sistema externo.
 * Si el CORE falla, la operación local se mantiene y el error queda registrado con su
 * correlation id. En producción este punto se reemplaza por un Transactional Outbox con
 * reintentos y DLQ (ver Módulo 1 del documento de diseño).</p>
 */
@Component
public class CoreSincronizacionListener {

    private static final Logger log = LoggerFactory.getLogger(CoreSincronizacionListener.class);

    private final CoreEdicionClient coreEdicionClient;

    public CoreSincronizacionListener(CoreEdicionClient coreEdicionClient) {
        this.coreEdicionClient = coreEdicionClient;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alModificarPoliza(PolizaModificadaEvent evento) {
        try {
            coreEdicionClient.enviarEdicion(CoreEdicionEvento.desde(evento));
        } catch (RuntimeException ex) {
            log.error("Sincronización con CORE pendiente polizaId={} riesgoId={} operacion={}: {}",
                    evento.polizaId(), evento.riesgoId(), evento.operacion(), ex.getMessage(), ex);
        }
    }
}
