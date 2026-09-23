package com.segurosbolivar.polizas.service;

/** Operaciones que modifican el estado de pólizas o riesgos y deben reflejarse en el CORE. */
public enum OperacionPoliza {
    RENOVACION_POLIZA,
    CANCELACION_POLIZA,
    ADICION_RIESGO,
    CANCELACION_RIESGO
}
