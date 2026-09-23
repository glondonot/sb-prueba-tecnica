package com.segurosbolivar.polizas.integration;

import com.segurosbolivar.polizas.config.RenovacionProperties;
import com.segurosbolivar.polizas.service.IpcProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Obtiene el IPC desde configuración ({@code polizas.renovacion.ipc-porcentaje} / variable IPC_PORCENTAJE). */
@Component
public class IpcConfiguradoProvider implements IpcProvider {

    private final RenovacionProperties properties;

    public IpcConfiguradoProvider(RenovacionProperties properties) {
        this.properties = properties;
    }

    @Override
    public BigDecimal ipcVigente() {
        return properties.ipcPorcentaje();
    }
}
