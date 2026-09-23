package com.segurosbolivar.polizas.repository;

import com.segurosbolivar.polizas.domain.Poliza;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PolizaRepository extends JpaRepository<Poliza, Long>, JpaSpecificationExecutor<Poliza> {
}
