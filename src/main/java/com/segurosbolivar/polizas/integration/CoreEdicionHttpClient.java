package com.segurosbolivar.polizas.integration;

import com.segurosbolivar.polizas.config.CoreProperties;
import com.segurosbolivar.polizas.config.CorrelationIdFilter;
import com.segurosbolivar.polizas.config.SeguridadProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptador HTTP hacia el servicio de edición del CORE ({@code POST {polizas.core.url}/evento}).
 * Los timeouts se configuran con {@code spring.http.clients.*} para no bloquear hilos si el CORE se degrada.
 */
@Component
public class CoreEdicionHttpClient implements CoreEdicionClient {

    private static final Logger log = LoggerFactory.getLogger(CoreEdicionHttpClient.class);

    private final RestClient restClient;

    public CoreEdicionHttpClient(RestClient.Builder builder, CoreProperties core, SeguridadProperties seguridad) {
        this.restClient = builder
                .baseUrl(core.url())
                .defaultHeader(seguridad.apiKeyHeader(), seguridad.apiKey())
                .build();
    }

    @Override
    public void enviarEdicion(CoreEdicionEvento evento) {
        try {
            restClient.post()
                    .uri("/evento")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
                        if (correlationId != null) {
                            headers.set(CorrelationIdFilter.HEADER, correlationId);
                        }
                    })
                    .body(evento)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Edición enviada al CORE polizaId={} operacion={}", evento.polizaId(), evento.operacion());
        } catch (RestClientException ex) {
            throw new CoreIntegracionException(
                    "No fue posible enviar la edición de la póliza %d al CORE".formatted(evento.polizaId()), ex);
        }
    }
}
