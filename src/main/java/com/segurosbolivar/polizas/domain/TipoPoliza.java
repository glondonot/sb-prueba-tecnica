package com.segurosbolivar.polizas.domain;

public enum TipoPoliza {
    /** Tomador y asegurado = arrendatario; beneficiario = arrendador. Un único riesgo. */
    INDIVIDUAL,
    /** Tomador = inmobiliaria o administración de copropiedad. Uno o muchos riesgos. */
    COLECTIVA
}
