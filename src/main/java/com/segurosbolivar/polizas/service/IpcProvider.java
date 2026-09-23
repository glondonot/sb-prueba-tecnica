package com.segurosbolivar.polizas.service;

import java.math.BigDecimal;

/**
 * Fuente del IPC vigente para renovaciones (puerto de salida).
 * Hoy se resuelve por configuración; en producción se reemplaza por un adaptador
 * que consulte la fuente oficial (DANE / servicio corporativo de parámetros) sin tocar el servicio.
 */
public interface IpcProvider {

    /** IPC anual en porcentaje (p. ej. 5.20 = 5,20 %). */
    BigDecimal ipcVigente();
}
