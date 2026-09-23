package com.segurosbolivar.polizas.domain;

import com.segurosbolivar.polizas.exception.EstadoInvalidoException;
import com.segurosbolivar.polizas.exception.ReglaNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolizaTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 1, 1);

    static Riesgo riesgo(String direccion) {
        return new Riesgo(direccion, "Bogotá",
                new Persona("1020304050", "Arrendatario Prueba"),
                new Persona("19333444", "Arrendador Prueba"));
    }

    static Poliza individual() {
        return Poliza.individual("IND-TEST", riesgo("Calle 1 # 2-3"), INICIO, 12, new BigDecimal("1000000"));
    }

    static Poliza colectiva() {
        return Poliza.colectiva("COL-TEST", new Persona("900123456", "Inmobiliaria Prueba"),
                INICIO, 12, new BigDecimal("1000000"));
    }

    @Nested
    @DisplayName("Creación")
    class Creacion {

        @Test
        void calculaPrimaYFinDeVigencia() {
            Poliza poliza = Poliza.colectiva("COL-1", new Persona("900", "Inmobiliaria"),
                    INICIO, 6, new BigDecimal("2500000"));

            assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.ACTIVA);
            assertThat(poliza.getValorPrima()).isEqualByComparingTo("15000000.00");
            assertThat(poliza.getFechaFinVigencia()).isEqualTo(LocalDate.of(2026, 7, 1));
        }

        @Test
        void individualTomaAlArrendatarioComoTomadorYTieneUnRiesgo() {
            Poliza poliza = individual();

            assertThat(poliza.getTipo()).isEqualTo(TipoPoliza.INDIVIDUAL);
            assertThat(poliza.getTomador().getDocumento()).isEqualTo("1020304050");
            assertThat(poliza.getRiesgos()).hasSize(1);
            assertThat(poliza.getRiesgos().getFirst().getPoliza()).isSameAs(poliza);
        }

        @Test
        void rechazaCanonNoPositivo() {
            assertThatThrownBy(() -> Poliza.colectiva("COL-1", new Persona("900", "X"), INICIO, 12, BigDecimal.ZERO))
                    .isInstanceOf(ReglaNegocioException.class);
        }

        @Test
        void rechazaMesesDeVigenciaNoPositivos() {
            assertThatThrownBy(() -> Poliza.colectiva("COL-1", new Persona("900", "X"), INICIO, 0, BigDecimal.TEN))
                    .isInstanceOf(ReglaNegocioException.class);
        }
    }

    @Nested
    @DisplayName("Renovación")
    class Renovacion {

        @Test
        void incrementaCanonYPrimaPorIpcYDesplazaVigencia() {
            Poliza poliza = individual();

            poliza.renovar(new BigDecimal("5.20"));

            assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.RENOVADA);
            assertThat(poliza.getValorCanon()).isEqualByComparingTo("1052000.00");
            assertThat(poliza.getValorPrima()).isEqualByComparingTo("12624000.00");
            assertThat(poliza.getFechaInicioVigencia()).isEqualTo(LocalDate.of(2027, 1, 1));
            assertThat(poliza.getFechaFinVigencia()).isEqualTo(LocalDate.of(2028, 1, 1));
        }

        @Test
        void redondeaCanonHalfUpADosDecimales() {
            Poliza poliza = Poliza.colectiva("COL-1", new Persona("900", "X"), INICIO, 12, new BigDecimal("1234567"));

            poliza.renovar(new BigDecimal("5.17"));

            // 1.234.567 x 1,0517 = 1.298.394,1139 -> 1.298.394,11
            assertThat(poliza.getValorCanon()).isEqualByComparingTo("1298394.11");
            assertThat(poliza.getValorPrima()).isEqualByComparingTo("15580729.32");
        }

        @Test
        void unaPolizaRenovadaPuedeVolverASerRenovada() {
            Poliza poliza = individual();
            poliza.renovar(new BigDecimal("10"));

            poliza.renovar(new BigDecimal("10"));

            assertThat(poliza.getValorCanon()).isEqualByComparingTo("1210000.00");
            assertThat(poliza.getFechaFinVigencia()).isEqualTo(LocalDate.of(2029, 1, 1));
        }

        @Test
        void noPermiteRenovarUnaPolizaCancelada() {
            Poliza poliza = individual();
            poliza.cancelar();

            assertThatThrownBy(() -> poliza.renovar(new BigDecimal("5")))
                    .isInstanceOf(EstadoInvalidoException.class)
                    .hasMessageContaining("cancelada");
        }

        @Test
        void rechazaIpcNegativo() {
            assertThatThrownBy(() -> individual().renovar(new BigDecimal("-1")))
                    .isInstanceOf(ReglaNegocioException.class);
        }
    }

    @Nested
    @DisplayName("Cancelación")
    class Cancelacion {

        @Test
        void cancelaLaPolizaYTodosSusRiesgos() {
            Poliza poliza = colectiva();
            poliza.agregarRiesgo(riesgo("Dirección 1"));
            poliza.agregarRiesgo(riesgo("Dirección 2"));

            poliza.cancelar();

            assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.CANCELADA);
            assertThat(poliza.getFechaCancelacion()).isNotNull();
            assertThat(poliza.getRiesgos())
                    .allSatisfy(r -> {
                        assertThat(r.getEstado()).isEqualTo(EstadoRiesgo.CANCELADO);
                        assertThat(r.getFechaCancelacion()).isEqualTo(poliza.getFechaCancelacion());
                    });
        }

        @Test
        void noPermiteCancelarDosVeces() {
            Poliza poliza = individual();
            poliza.cancelar();

            assertThatThrownBy(poliza::cancelar).isInstanceOf(EstadoInvalidoException.class);
        }
    }

    @Nested
    @DisplayName("Riesgos")
    class Riesgos {

        @Test
        void agregaRiesgosAPolizaColectiva() {
            Poliza poliza = colectiva();

            poliza.agregarRiesgo(riesgo("Dirección 1"));
            poliza.agregarRiesgo(riesgo("Dirección 2"));

            assertThat(poliza.getRiesgos()).hasSize(2).allMatch(Riesgo::estaActivo);
        }

        @Test
        void noPermiteAgregarRiesgosAPolizaIndividual() {
            Poliza poliza = individual();

            assertThatThrownBy(() -> poliza.agregarRiesgo(riesgo("Otra dirección")))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("colectivas");
            assertThat(poliza.getRiesgos()).hasSize(1);
        }

        @Test
        void noPermiteAgregarRiesgosAPolizaCancelada() {
            Poliza poliza = colectiva();
            poliza.cancelar();

            assertThatThrownBy(() -> poliza.agregarRiesgo(riesgo("Dirección")))
                    .isInstanceOf(EstadoInvalidoException.class);
        }

        @Test
        void cancelaUnRiesgoDeColectivaSinAfectarLosDemas() {
            Poliza poliza = colectiva();
            Riesgo primero = riesgo("Dirección 1");
            Riesgo segundo = riesgo("Dirección 2");
            poliza.agregarRiesgo(primero);
            poliza.agregarRiesgo(segundo);

            poliza.cancelarRiesgo(primero);

            assertThat(primero.getEstado()).isEqualTo(EstadoRiesgo.CANCELADO);
            assertThat(segundo.getEstado()).isEqualTo(EstadoRiesgo.ACTIVO);
            assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.ACTIVA);
        }

        @Test
        void noPermiteCancelarDosVecesElMismoRiesgo() {
            Poliza poliza = colectiva();
            Riesgo riesgo = riesgo("Dirección 1");
            poliza.agregarRiesgo(riesgo);
            poliza.cancelarRiesgo(riesgo);

            assertThatThrownBy(() -> poliza.cancelarRiesgo(riesgo)).isInstanceOf(EstadoInvalidoException.class);
        }

        @Test
        void noPermiteCancelarElRiesgoUnicoDeUnaIndividual() {
            Poliza poliza = individual();
            Riesgo unico = poliza.getRiesgos().getFirst();

            assertThatThrownBy(() -> poliza.cancelarRiesgo(unico))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("cancele la póliza");
        }

        @Test
        void noPermiteCancelarUnRiesgoDeOtraPoliza() {
            Poliza poliza = colectiva();
            Poliza otra = colectiva();
            Riesgo ajeno = riesgo("Dirección");
            otra.agregarRiesgo(ajeno);

            assertThatThrownBy(() -> poliza.cancelarRiesgo(ajeno)).isInstanceOf(ReglaNegocioException.class);
        }
    }
}
