package com.segurosbolivar.polizas.repository;

import com.segurosbolivar.polizas.domain.EstadoPoliza;
import com.segurosbolivar.polizas.domain.Poliza;
import com.segurosbolivar.polizas.domain.TipoPoliza;
import org.springframework.data.jpa.domain.Specification;

/** Filtros opcionales y combinables para el listado de pólizas. */
public final class PolizaSpecifications {

    private PolizaSpecifications() {
    }

    public static Specification<Poliza> conFiltros(TipoPoliza tipo, EstadoPoliza estado) {
        return (root, query, cb) -> {
            var predicado = cb.conjunction();
            if (tipo != null) {
                predicado = cb.and(predicado, cb.equal(root.get("tipo"), tipo));
            }
            if (estado != null) {
                predicado = cb.and(predicado, cb.equal(root.get("estado"), estado));
            }
            return predicado;
        };
    }
}
