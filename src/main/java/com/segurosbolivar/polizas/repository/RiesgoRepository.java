package com.segurosbolivar.polizas.repository;

import com.segurosbolivar.polizas.domain.Riesgo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RiesgoRepository extends JpaRepository<Riesgo, Long> {

    List<Riesgo> findByPolizaIdOrderByIdAsc(Long polizaId);

    /** Carga el riesgo junto con su póliza (raíz del agregado) en una sola consulta. */
    @Query("select r from Riesgo r join fetch r.poliza where r.id = :id")
    Optional<Riesgo> findByIdConPoliza(Long id);
}
