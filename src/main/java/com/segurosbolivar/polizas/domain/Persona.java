package com.segurosbolivar.polizas.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/**
 * Tercero que participa en la póliza (tomador, arrendatario o arrendador).
 * Se modela como Value Object embebido: en esta versión simplificada no se requiere
 * un maestro de terceros (ver diseño completo en el Módulo 1).
 */
@Embeddable
public class Persona {

    @Column(name = "documento", nullable = false, length = 20)
    private String documento;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    protected Persona() {
        // requerido por JPA
    }

    public Persona(String documento, String nombre) {
        this.documento = Objects.requireNonNull(documento, "documento");
        this.nombre = Objects.requireNonNull(nombre, "nombre");
    }

    public String getDocumento() {
        return documento;
    }

    public String getNombre() {
        return nombre;
    }
}
