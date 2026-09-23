package com.segurosbolivar.polizas.repository;

import com.segurosbolivar.polizas.domain.Poliza;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PolizaRepository extends JpaRepository<Poliza, Long>, JpaSpecificationExecutor<Poliza> {

    /**
     * Carga la póliza forzando el incremento de su versión al confirmar la transacción.
     * Se usa al modificar sus riesgos: así, agregar/cancelar un riesgo y cancelar la póliza
     * en paralelo no pueden confirmarse ambos (uno recibe conflicto de concurrencia).
     */
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select p from Poliza p where p.id = :id")
    Optional<Poliza> findByIdParaModificarRiesgos(Long id);
}
