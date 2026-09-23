package com.segurosbolivar.polizas.repository;

import com.segurosbolivar.polizas.domain.EstadoPoliza;
import com.segurosbolivar.polizas.domain.EstadoRiesgo;
import com.segurosbolivar.polizas.domain.Persona;
import com.segurosbolivar.polizas.domain.Poliza;
import com.segurosbolivar.polizas.domain.Riesgo;
import com.segurosbolivar.polizas.domain.TipoPoliza;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica el mapeo JPA contra el esquema real de Flyway (ddl-auto=validate) y las consultas.
 * Usa el datasource configurado (H2 en modo PostgreSQL) en lugar de la BD embebida genérica.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PolizaRepositoryTest {

    @Autowired
    private PolizaRepository polizaRepository;

    @Autowired
    private RiesgoRepository riesgoRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    void filtraPorTipoYEstado() {
        var colectivasActivas = polizaRepository.findAll(
                PolizaSpecifications.conFiltros(TipoPoliza.COLECTIVA, EstadoPoliza.ACTIVA));

        assertThat(colectivasActivas).isNotEmpty()
                .allSatisfy(p -> {
                    assertThat(p.getTipo()).isEqualTo(TipoPoliza.COLECTIVA);
                    assertThat(p.getEstado()).isEqualTo(EstadoPoliza.ACTIVA);
                });
    }

    @Test
    void sinFiltrosDevuelveTodas() {
        assertThat(polizaRepository.findAll(PolizaSpecifications.conFiltros(null, null)))
                .hasSize((int) polizaRepository.count());
    }

    @Test
    void persisteLaPolizaConSusRiesgosEnCascadaYCancelaEnCascada() {
        Poliza poliza = Poliza.colectiva("COL-REPO-1", new Persona("900", "Inmobiliaria"),
                LocalDate.of(2026, 1, 1), 12, new BigDecimal("1500000"));
        poliza.agregarRiesgo(new Riesgo("Calle 1", "Bogotá", new Persona("1", "A"), new Persona("2", "B")));
        poliza.agregarRiesgo(new Riesgo("Calle 2", "Bogotá", new Persona("3", "C"), new Persona("4", "D")));
        Long id = em.persistFlushFind(poliza).getId();

        polizaRepository.findById(id).orElseThrow().cancelar();
        em.flush();
        em.clear();

        assertThat(polizaRepository.findById(id).orElseThrow().getEstado()).isEqualTo(EstadoPoliza.CANCELADA);
        assertThat(riesgoRepository.findByPolizaIdOrderByIdAsc(id))
                .hasSize(2)
                .allMatch(r -> r.getEstado() == EstadoRiesgo.CANCELADO);
    }

    @Test
    void cargaElRiesgoConSuPoliza() {
        Long riesgoId = riesgoRepository.findAll().getFirst().getId();
        em.clear();

        Riesgo riesgo = riesgoRepository.findByIdConPoliza(riesgoId).orElseThrow();

        assertThat(riesgo.getPoliza().getNumero()).isNotBlank();
    }
}
