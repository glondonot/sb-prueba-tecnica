package com.segurosbolivar.polizas.controller;

import com.segurosbolivar.polizas.domain.EstadoRiesgo;
import com.segurosbolivar.polizas.domain.Poliza;
import com.segurosbolivar.polizas.repository.RiesgoRepository;
import com.segurosbolivar.polizas.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PolizaControllerIT extends IntegrationTest {

    @Autowired
    private RiesgoRepository riesgoRepository;

    @Test
    void listaPolizasFiltradasPorTipoYEstado() throws Exception {
        crearColectiva("2000000", 1);

        mockMvc.perform(get("/polizas").header(API_KEY_HEADER, API_KEY)
                        .param("tipo", "COLECTIVA").param("estado", "ACTIVA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.contenido[*].tipo", everyItem(is("COLECTIVA"))))
                .andExpect(jsonPath("$.contenido[*].estado", everyItem(is("ACTIVA"))));
    }

    @Test
    void listaPolizasPaginadas() throws Exception {
        mockMvc.perform(get("/polizas").header(API_KEY_HEADER, API_KEY).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamano", is(2)))
                .andExpect(jsonPath("$.contenido", hasSize(lessThanOrEqualTo(2))));
    }

    @Test
    void rechazaTipoInexistenteConMensajeClaro() throws Exception {
        mockMvc.perform(get("/polizas").header(API_KEY_HEADER, API_KEY).param("tipo", "GRUPAL"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("INDIVIDUAL, COLECTIVA")));
    }

    @Test
    void listaLosRiesgosDeUnaPoliza() throws Exception {
        Poliza poliza = crearColectiva("2000000", 3);

        mockMvc.perform(get("/polizas/{id}/riesgos", poliza.getId()).header(API_KEY_HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].polizaId", is(poliza.getId().intValue())))
                .andExpect(jsonPath("$[0].estado", is("ACTIVO")));
    }

    @Test
    void respondeNotFoundSiLaPolizaNoExiste() throws Exception {
        mockMvc.perform(get("/polizas/{id}/riesgos", 999_999).header(API_KEY_HEADER, API_KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Recurso no encontrado")));
    }

    @Test
    void renuevaIncrementandoCanonYPrimaPorIpc() throws Exception {
        Poliza poliza = crearIndividual("1000000");

        mockMvc.perform(post("/polizas/{id}/renovar", poliza.getId()).header(API_KEY_HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("RENOVADA")))
                .andExpect(jsonPath("$.valorCanon", is(1052000.00)))
                .andExpect(jsonPath("$.valorPrima", is(12624000.00)))
                .andExpect(jsonPath("$.fechaInicioVigencia", is("2027-01-01")))
                .andExpect(jsonPath("$.fechaFinVigencia", is("2028-01-01")));

        assertThat(polizaRepository.findById(poliza.getId()).orElseThrow().getValorCanon())
                .isEqualByComparingTo("1052000.00");
    }

    @Test
    void noRenuevaUnaPolizaCancelada() throws Exception {
        Poliza poliza = cancelada(crearIndividual("1000000"));

        mockMvc.perform(post("/polizas/{id}/renovar", poliza.getId()).header(API_KEY_HEADER, API_KEY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("cancelada")));
    }

    @Test
    void cancelaLaPolizaYSusRiesgos() throws Exception {
        Poliza poliza = crearColectiva("3000000", 2);

        mockMvc.perform(post("/polizas/{id}/cancelar", poliza.getId()).header(API_KEY_HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("CANCELADA")))
                .andExpect(jsonPath("$.fechaCancelacion").isNotEmpty());

        assertThat(riesgoRepository.findByPolizaIdOrderByIdAsc(poliza.getId()))
                .hasSize(2)
                .allMatch(r -> r.getEstado() == EstadoRiesgo.CANCELADO);
    }

    @Test
    void noCancelaDosVecesLaMismaPoliza() throws Exception {
        Poliza poliza = cancelada(crearIndividual("1000000"));

        mockMvc.perform(post("/polizas/{id}/cancelar", poliza.getId()).header(API_KEY_HEADER, API_KEY))
                .andExpect(status().isConflict());
    }
}
