package com.segurosbolivar.polizas.integration;

import com.segurosbolivar.polizas.domain.Persona;
import com.segurosbolivar.polizas.domain.Poliza;
import com.segurosbolivar.polizas.repository.PolizaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de punta a punta con servidor HTTP real: una operación de negocio dispara, después del
 * commit, la llamada HTTP del cliente del CORE hacia {@code /core-mock/evento}, que la registra en logs.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = "server.port=18089")
@ExtendWith(OutputCaptureExtension.class)
class CoreSincronizacionE2EIT {

    @Autowired
    private PolizaRepository polizaRepository;

    @Test
    void cancelarUnaPolizaNotificaAlCoreMock(CapturedOutput output) {
        Poliza poliza = polizaRepository.save(Poliza.colectiva("COL-E2E-1", new Persona("900", "Inmobiliaria E2E"),
                LocalDate.of(2026, 1, 1), 12, new BigDecimal("2000000")));

        var respuesta = RestClient.create("http://localhost:18089")
                .post().uri("/polizas/{id}/cancelar", poliza.getId())
                .header("api-key", "123456")
                .header("X-Correlation-Id", "e2e-core-123")
                .retrieve()
                .toBodilessEntity();

        assertThat(respuesta.getStatusCode().value()).isEqualTo(200);
        assertThat(output)
                .contains("[CORE-MOCK] Evento recibido: evento=ACTUALIZACION polizaId=" + poliza.getId()
                        + " riesgoId=null operacion=CANCELACION_POLIZA")
                .contains("[e2e-core-123]");
    }
}
