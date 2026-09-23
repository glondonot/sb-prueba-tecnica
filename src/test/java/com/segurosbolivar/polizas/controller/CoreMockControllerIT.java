package com.segurosbolivar.polizas.controller;

import com.segurosbolivar.polizas.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
class CoreMockControllerIT extends IntegrationTest {

    @Test
    void registraEnLogsElEventoRecibido(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/core-mock/evento").header(API_KEY_HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evento\":\"ACTUALIZACION\",\"polizaId\":555}"))
                .andExpect(status().isAccepted());

        assertThat(output).contains("[CORE-MOCK] Evento recibido: evento=ACTUALIZACION polizaId=555");
    }

    @Test
    void rechazaEventosIncompletos() throws Exception {
        mockMvc.perform(post("/core-mock/evento").header(API_KEY_HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evento\":\"ACTUALIZACION\"}"))
                .andExpect(status().isBadRequest());
    }
}
