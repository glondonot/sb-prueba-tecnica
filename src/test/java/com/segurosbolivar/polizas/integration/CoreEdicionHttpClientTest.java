package com.segurosbolivar.polizas.integration;

import com.segurosbolivar.polizas.config.CoreProperties;
import com.segurosbolivar.polizas.config.SeguridadProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@RestClientTest(value = CoreEdicionHttpClient.class, properties = {
        "polizas.core.url=http://core.test/edicion",
        "polizas.seguridad.api-key-header=api-key",
        "polizas.seguridad.api-key=clave-core"
})
@EnableConfigurationProperties({CoreProperties.class, SeguridadProperties.class})
class CoreEdicionHttpClientTest {

    @Autowired
    private CoreEdicionHttpClient client;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void enviaElEventoConElContratoDelCore() {
        server.expect(requestTo("http://core.test/edicion/evento"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "clave-core"))
                .andExpect(content().json("""
                        {"evento":"ACTUALIZACION","polizaId":555,"operacion":"RENOVACION_POLIZA"}
                        """, JsonCompareMode.STRICT))
                .andRespond(withStatus(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON));

        client.enviarEdicion(new CoreEdicionEvento("ACTUALIZACION", 555L, null, "RENOVACION_POLIZA"));

        server.verify();
    }

    @Test
    void traduceLosErroresDelCoreAUnaExcepcionDeIntegracion() {
        server.expect(requestTo("http://core.test/edicion/evento")).andRespond(withServerError());

        assertThatThrownBy(() -> client.enviarEdicion(
                new CoreEdicionEvento("ACTUALIZACION", 555L, 10L, "CANCELACION_RIESGO")))
                .isInstanceOf(CoreIntegracionException.class)
                .hasMessageContaining("555");
    }
}
