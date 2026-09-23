package com.segurosbolivar.polizas.integration;

import com.segurosbolivar.polizas.service.OperacionPoliza;
import com.segurosbolivar.polizas.service.PolizaModificadaEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CoreSincronizacionListenerTest {

    private final CoreEdicionClient client = mock(CoreEdicionClient.class);
    private final CoreSincronizacionListener listener = new CoreSincronizacionListener(client);

    @Test
    void traduceElEventoDeDominioAlContratoDelCore() {
        listener.alModificarPoliza(PolizaModificadaEvent.deRiesgo(7L, 21L, OperacionPoliza.ADICION_RIESGO));

        verify(client).enviarEdicion(new CoreEdicionEvento("ACTUALIZACION", 7L, 21L, "ADICION_RIESGO"));
    }

    @Test
    void unaFallaDelCoreNoSePropagaALaOperacionYaConfirmada() {
        doThrow(new CoreIntegracionException("CORE no disponible", null)).when(client).enviarEdicion(any());

        assertThatCode(() -> listener.alModificarPoliza(
                PolizaModificadaEvent.dePoliza(7L, OperacionPoliza.CANCELACION_POLIZA)))
                .doesNotThrowAnyException();
    }
}
