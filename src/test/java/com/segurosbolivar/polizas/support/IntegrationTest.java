package com.segurosbolivar.polizas.support;

import com.segurosbolivar.polizas.domain.Persona;
import com.segurosbolivar.polizas.domain.Poliza;
import com.segurosbolivar.polizas.domain.Riesgo;
import com.segurosbolivar.polizas.integration.CoreEdicionClient;
import com.segurosbolivar.polizas.repository.PolizaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Base para pruebas de integración: contexto completo + MockMvc sobre H2.
 * Cada prueba crea sus propios datos (números de póliza únicos), por lo que no dependen
 * del orden de ejecución ni de los datos semilla. El cliente del CORE se reemplaza por un mock
 * para verificar qué se le envía; el flujo HTTP real se prueba en {@code CoreSincronizacionE2EIT}.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTest {

    protected static final String API_KEY_HEADER = "api-key";
    protected static final String API_KEY = "123456";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected PolizaRepository polizaRepository;

    @MockitoBean
    protected CoreEdicionClient coreEdicionClient;

    @Autowired
    private TransactionTemplate transactionTemplate;

    protected Poliza crearIndividual(String canon) {
        Riesgo riesgo = riesgo("Calle 100 # 10-10");
        return polizaRepository.save(Poliza.individual(numero("IND"), riesgo,
                LocalDate.of(2026, 1, 1), 12, new BigDecimal(canon)));
    }

    protected Poliza crearColectiva(String canon, int cantidadRiesgos) {
        Poliza poliza = Poliza.colectiva(numero("COL"), new Persona("900123456", "Inmobiliaria de Prueba S.A.S."),
                LocalDate.of(2026, 1, 1), 12, new BigDecimal(canon));
        for (int i = 1; i <= cantidadRiesgos; i++) {
            poliza.agregarRiesgo(riesgo("Carrera " + i + " # 20-30"));
        }
        return polizaRepository.save(poliza);
    }

    protected Poliza cancelada(Poliza poliza) {
        return transactionTemplate.execute(status -> {
            Poliza gestionada = polizaRepository.findById(poliza.getId()).orElseThrow();
            gestionada.cancelar();
            return gestionada;
        });
    }

    protected static Riesgo riesgo(String direccion) {
        return new Riesgo(direccion, "Bogotá", new Persona("1015000111", "Arrendatario Prueba"),
                new Persona("80111222", "Arrendador Prueba"));
    }

    private static String numero(String prefijo) {
        return prefijo + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
