package com.segurosbolivar.polizas.controller;

import com.segurosbolivar.polizas.domain.EstadoRiesgo;
import com.segurosbolivar.polizas.domain.Poliza;
import com.segurosbolivar.polizas.domain.Riesgo;
import com.segurosbolivar.polizas.repository.RiesgoRepository;
import com.segurosbolivar.polizas.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RiesgoControllerIT extends IntegrationTest {

    private static final String RIESGO_VALIDO = """
            {
              "direccionInmueble": "Calle 26 # 68-35 Apto 1502",
              "ciudad": "Bogotá",
              "arrendatario": { "documento": "1032456789", "nombre": "Camila Herrera" },
              "arrendador":   { "documento": "79555444",   "nombre": "Mauricio Peña" }
            }
            """;

    @Autowired
    private RiesgoRepository riesgoRepository;

    @Test
    void agregaUnRiesgoAUnaPolizaColectiva() throws Exception {
        Poliza poliza = crearColectiva("2000000", 1);
        long versionInicial = poliza.getVersion();

        mockMvc.perform(post("/polizas/{id}/riesgos", poliza.getId()).header(API_KEY_HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(RIESGO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.polizaId", is(poliza.getId().intValue())))
                .andExpect(jsonPath("$.estado", is("ACTIVO")))
                .andExpect(jsonPath("$.arrendatario.nombre", is("Camila Herrera")));

        assertThat(riesgoRepository.findByPolizaIdOrderByIdAsc(poliza.getId())).hasSize(2);
        assertThat(polizaRepository.findById(poliza.getId()).orElseThrow().getVersion())
                .as("modificar riesgos incrementa la versión de la póliza (control de concurrencia)")
                .isGreaterThan(versionInicial);
    }

    @Test
    void noAgregaRiesgosAPolizaIndividual() throws Exception {
        Poliza poliza = crearIndividual("1500000");

        mockMvc.perform(post("/polizas/{id}/riesgos", poliza.getId()).header(API_KEY_HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(RIESGO_VALIDO))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail", containsString("colectivas")));

        assertThat(riesgoRepository.findByPolizaIdOrderByIdAsc(poliza.getId())).hasSize(1);
    }

    @Test
    void noAgregaRiesgosAPolizaCancelada() throws Exception {
        Poliza poliza = cancelada(crearColectiva("2000000", 1));

        mockMvc.perform(post("/polizas/{id}/riesgos", poliza.getId()).header(API_KEY_HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(RIESGO_VALIDO))
                .andExpect(status().isConflict());
    }

    @Test
    void validaElCuerpoDeLaSolicitud() throws Exception {
        Poliza poliza = crearColectiva("2000000", 1);
        String invalido = """
                { "direccionInmueble": "", "ciudad": "Bogotá",
                  "arrendatario": { "documento": "abc", "nombre": "X" } }
                """;

        mockMvc.perform(post("/polizas/{id}/riesgos", poliza.getId()).header(API_KEY_HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("direccionInmueble")))
                .andExpect(jsonPath("$.errores[*].campo", hasItem("arrendatario.documento")))
                .andExpect(jsonPath("$.errores[*].campo", hasItem("arrendador")));
    }

    @Test
    void respondeNotFoundAlAgregarAPolizaInexistente() throws Exception {
        mockMvc.perform(post("/polizas/{id}/riesgos", 999_999).header(API_KEY_HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(RIESGO_VALIDO))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelaUnRiesgoDeColectivaSinAfectarLosDemas() throws Exception {
        Poliza poliza = crearColectiva("2000000", 2);
        List<Riesgo> riesgos = riesgoRepository.findByPolizaIdOrderByIdAsc(poliza.getId());

        mockMvc.perform(post("/riesgos/{id}/cancelar", riesgos.get(0).getId()).header(API_KEY_HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("CANCELADO")))
                .andExpect(jsonPath("$.fechaCancelacion", notNullValue()));

        assertThat(riesgoRepository.findById(riesgos.get(1).getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoRiesgo.ACTIVO);
    }

    @Test
    void noCancelaElRiesgoUnicoDeUnaPolizaIndividual() throws Exception {
        Poliza poliza = crearIndividual("1500000");
        Long riesgoId = riesgoRepository.findByPolizaIdOrderByIdAsc(poliza.getId()).getFirst().getId();

        mockMvc.perform(post("/riesgos/{id}/cancelar", riesgoId).header(API_KEY_HEADER, API_KEY))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void noCancelaDosVecesElMismoRiesgo() throws Exception {
        Poliza poliza = crearColectiva("2000000", 2);
        Long riesgoId = riesgoRepository.findByPolizaIdOrderByIdAsc(poliza.getId()).getFirst().getId();
        mockMvc.perform(post("/riesgos/{id}/cancelar", riesgoId).header(API_KEY_HEADER, API_KEY))
                .andExpect(status().isOk());

        mockMvc.perform(post("/riesgos/{id}/cancelar", riesgoId).header(API_KEY_HEADER, API_KEY))
                .andExpect(status().isConflict());
    }

    @Test
    void respondeNotFoundAlCancelarRiesgoInexistente() throws Exception {
        mockMvc.perform(post("/riesgos/{id}/cancelar", 999_999).header(API_KEY_HEADER, API_KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", containsString("Riesgo")));
    }
}
