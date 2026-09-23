package com.segurosbolivar.polizas.config;

import com.segurosbolivar.polizas.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SeguridadIT extends IntegrationTest {

    @Test
    void rechazaPeticionesSinApiKey() throws Exception {
        mockMvc.perform(get("/polizas"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.detail", containsString("api-key")));
    }

    @Test
    void rechazaApiKeyInvalida() throws Exception {
        mockMvc.perform(post("/polizas/1/cancelar").header(API_KEY_HEADER, "000000"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail", containsString("no es válido")));
    }

    @Test
    void elMockDelCoreTambienExigeApiKey() throws Exception {
        mockMvc.perform(post("/core-mock/evento").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evento\":\"ACTUALIZACION\",\"polizaId\":555}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthCheckYDocumentacionSonPublicos() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.apiKey.name", is("api-key")));
    }

    @Test
    void generaCorrelationIdSiNoSeEnvia() throws Exception {
        mockMvc.perform(get("/polizas").header(API_KEY_HEADER, API_KEY))
                .andExpect(header().string(CorrelationIdFilter.HEADER, matchesPattern("[0-9a-f-]{36}")));
    }

    @Test
    void propagaElCorrelationIdRecibido() throws Exception {
        mockMvc.perform(get("/polizas").header(API_KEY_HEADER, API_KEY)
                        .header(CorrelationIdFilter.HEADER, "prueba-123"))
                .andExpect(header().string(CorrelationIdFilter.HEADER, "prueba-123"));
    }
}
