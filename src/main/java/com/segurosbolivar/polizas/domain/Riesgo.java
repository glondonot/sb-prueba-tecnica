package com.segurosbolivar.polizas.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;

/**
 * Riesgo asegurado: el contrato de arrendamiento de un inmueble. El arrendatario es el
 * asegurado y el arrendador es el beneficiario en caso de siniestro.
 * Su ciclo de vida lo gobierna la {@link Poliza} (raíz del agregado).
 */
@Entity
@Table(name = "riesgos")
public class Riesgo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poliza_id", nullable = false)
    private Poliza poliza;

    @Column(name = "direccion_inmueble", nullable = false, length = 200)
    private String direccionInmueble;

    @Column(nullable = false, length = 80)
    private String ciudad;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "documento", column = @Column(name = "arrendatario_documento", nullable = false, length = 20)),
            @AttributeOverride(name = "nombre", column = @Column(name = "arrendatario_nombre", nullable = false, length = 150))
    })
    private Persona arrendatario;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "documento", column = @Column(name = "arrendador_documento", nullable = false, length = 20)),
            @AttributeOverride(name = "nombre", column = @Column(name = "arrendador_nombre", nullable = false, length = 150))
    })
    private Persona arrendador;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoRiesgo estado;

    @Column(name = "fecha_cancelacion")
    private Instant fechaCancelacion;

    @Version
    private Long version;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    protected Riesgo() {
        // requerido por JPA
    }

    public Riesgo(String direccionInmueble, String ciudad, Persona arrendatario, Persona arrendador) {
        this.direccionInmueble = Objects.requireNonNull(direccionInmueble, "direccionInmueble");
        this.ciudad = Objects.requireNonNull(ciudad, "ciudad");
        this.arrendatario = Objects.requireNonNull(arrendatario, "arrendatario");
        this.arrendador = Objects.requireNonNull(arrendador, "arrendador");
        this.estado = EstadoRiesgo.ACTIVO;
    }

    void asignarPoliza(Poliza poliza) {
        this.poliza = poliza;
    }

    void marcarCancelado(Instant fecha) {
        this.estado = EstadoRiesgo.CANCELADO;
        this.fechaCancelacion = fecha;
    }

    public boolean estaActivo() {
        return estado == EstadoRiesgo.ACTIVO;
    }

    @PrePersist
    void alCrear() {
        creadoEn = Instant.now();
        actualizadoEn = creadoEn;
    }

    @PreUpdate
    void alActualizar() {
        actualizadoEn = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Poliza getPoliza() {
        return poliza;
    }

    public String getDireccionInmueble() {
        return direccionInmueble;
    }

    public String getCiudad() {
        return ciudad;
    }

    public Persona getArrendatario() {
        return arrendatario;
    }

    public Persona getArrendador() {
        return arrendador;
    }

    public EstadoRiesgo getEstado() {
        return estado;
    }

    public Instant getFechaCancelacion() {
        return fechaCancelacion;
    }
}
