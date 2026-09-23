package com.segurosbolivar.polizas.service;

import com.segurosbolivar.polizas.domain.EstadoPoliza;
import com.segurosbolivar.polizas.domain.Persona;
import com.segurosbolivar.polizas.domain.Poliza;
import com.segurosbolivar.polizas.exception.RecursoNoEncontradoException;
import com.segurosbolivar.polizas.repository.PolizaRepository;
import com.segurosbolivar.polizas.repository.RiesgoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolizaServiceTest {

    @Mock
    private PolizaRepository polizaRepository;

    @Mock
    private RiesgoRepository riesgoRepository;

    @Mock
    private IpcProvider ipcProvider;

    @InjectMocks
    private PolizaService service;

    private static Poliza colectiva() {
        return Poliza.colectiva("COL-1", new Persona("900", "Inmobiliaria"), LocalDate.of(2026, 1, 1),
                12, new BigDecimal("2000000"));
    }

    @Test
    void renuevaConElIpcDelProveedor() {
        when(polizaRepository.findById(1L)).thenReturn(Optional.of(colectiva()));
        when(ipcProvider.ipcVigente()).thenReturn(new BigDecimal("10"));

        var respuesta = service.renovar(1L);

        assertThat(respuesta.estado()).isEqualTo(EstadoPoliza.RENOVADA);
        assertThat(respuesta.valorCanon()).isEqualByComparingTo("2200000.00");
        assertThat(respuesta.valorPrima()).isEqualByComparingTo("26400000.00");
    }

    @Test
    void cancelaLaPoliza() {
        when(polizaRepository.findById(1L)).thenReturn(Optional.of(colectiva()));

        assertThat(service.cancelar(1L).estado()).isEqualTo(EstadoPoliza.CANCELADA);
    }

    @Test
    void fallaSiLaPolizaNoExiste() {
        when(polizaRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.renovar(7L)).isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(ipcProvider);
    }

    @Test
    void noConsultaRiesgosSiLaPolizaNoExiste() {
        when(polizaRepository.existsById(7L)).thenReturn(false);

        assertThatThrownBy(() -> service.listarRiesgos(7L)).isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(riesgoRepository);
    }
}
