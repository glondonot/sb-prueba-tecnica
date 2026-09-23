package com.segurosbolivar.polizas.domain;

import com.segurosbolivar.polizas.exception.EstadoInvalidoException;
import com.segurosbolivar.polizas.exception.ReglaNegocioException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Póliza de arrendamiento. Es la raíz del agregado: toda modificación de sus riesgos
 * pasa por aquí para que las reglas de negocio se validen en un solo lugar.
 */
@Entity
@Table(name = "polizas")
public class Poliza {

    static final int ESCALA_MONETARIA = 2;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TipoPoliza tipo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoPoliza estado;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "documento", column = @Column(name = "tomador_documento", nullable = false, length = 20)),
            @AttributeOverride(name = "nombre", column = @Column(name = "tomador_nombre", nullable = false, length = 150))
    })
    private Persona tomador;

    @Column(name = "fecha_inicio_vigencia", nullable = false)
    private LocalDate fechaInicioVigencia;

    @Column(name = "fecha_fin_vigencia", nullable = false)
    private LocalDate fechaFinVigencia;

    @Column(name = "meses_vigencia", nullable = false)
    private int mesesVigencia;

    @Column(name = "valor_canon", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorCanon;

    @Column(name = "valor_prima", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorPrima;

    @Column(name = "fecha_cancelacion")
    private Instant fechaCancelacion;

    @OneToMany(mappedBy = "poliza", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("id ASC")
    private List<Riesgo> riesgos = new ArrayList<>();

    /** Bloqueo optimista: evita que dos operaciones concurrentes se pisen (p. ej. renovar y cancelar). */
    @Version
    private Long version;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    protected Poliza() {
        // requerido por JPA
    }

    private Poliza(String numero, TipoPoliza tipo, Persona tomador, LocalDate fechaInicioVigencia,
                   int mesesVigencia, BigDecimal valorCanon) {
        if (mesesVigencia <= 0) {
            throw new ReglaNegocioException("Los meses de vigencia deben ser mayores a cero");
        }
        if (valorCanon == null || valorCanon.signum() <= 0) {
            throw new ReglaNegocioException("El valor del canon debe ser mayor a cero");
        }
        this.numero = Objects.requireNonNull(numero, "numero");
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.tomador = Objects.requireNonNull(tomador, "tomador");
        this.estado = EstadoPoliza.ACTIVA;
        this.mesesVigencia = mesesVigencia;
        this.fechaInicioVigencia = Objects.requireNonNull(fechaInicioVigencia, "fechaInicioVigencia");
        this.fechaFinVigencia = fechaInicioVigencia.plusMonths(mesesVigencia);
        this.valorCanon = valorCanon.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
        this.valorPrima = calcularPrima(this.valorCanon, mesesVigencia);
    }

    /** Póliza individual: el arrendatario es tomador y asegurado; nace con su único riesgo. */
    public static Poliza individual(String numero, Riesgo riesgo, LocalDate fechaInicioVigencia,
                                    int mesesVigencia, BigDecimal valorCanon) {
        Poliza poliza = new Poliza(numero, TipoPoliza.INDIVIDUAL, riesgo.getArrendatario(),
                fechaInicioVigencia, mesesVigencia, valorCanon);
        poliza.vincular(riesgo);
        return poliza;
    }

    /** Póliza colectiva: el tomador es la inmobiliaria o la administración de la copropiedad. */
    public static Poliza colectiva(String numero, Persona tomador, LocalDate fechaInicioVigencia,
                                   int mesesVigencia, BigDecimal valorCanon) {
        return new Poliza(numero, TipoPoliza.COLECTIVA, tomador, fechaInicioVigencia, mesesVigencia, valorCanon);
    }

    /** Prima = canon mensual x número de meses de la vigencia. */
    static BigDecimal calcularPrima(BigDecimal canon, int meses) {
        return canon.multiply(BigDecimal.valueOf(meses)).setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
    }

    /**
     * Renueva por el mismo periodo de la vigencia inicial ajustando el canon por IPC.
     *
     * @param ipcPorcentaje IPC expresado en porcentaje (p. ej. 5.20 = 5,20 %)
     */
    public void renovar(BigDecimal ipcPorcentaje) {
        if (estado == EstadoPoliza.CANCELADA) {
            throw new EstadoInvalidoException("No se puede renovar la póliza %s porque está cancelada".formatted(numero));
        }
        if (ipcPorcentaje == null || ipcPorcentaje.signum() < 0) {
            throw new ReglaNegocioException("El IPC para la renovación debe ser un valor positivo");
        }
        BigDecimal factor = BigDecimal.ONE.add(ipcPorcentaje.divide(CIEN, 6, RoundingMode.HALF_UP));
        this.valorCanon = valorCanon.multiply(factor).setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
        this.valorPrima = calcularPrima(valorCanon, mesesVigencia);
        this.fechaInicioVigencia = fechaFinVigencia;
        this.fechaFinVigencia = fechaFinVigencia.plusMonths(mesesVigencia);
        this.estado = EstadoPoliza.RENOVADA;
    }

    /** Cancela la póliza y, en cascada, todos sus riesgos activos. */
    public void cancelar() {
        if (estado == EstadoPoliza.CANCELADA) {
            throw new EstadoInvalidoException("La póliza %s ya se encuentra cancelada".formatted(numero));
        }
        Instant ahora = Instant.now();
        this.estado = EstadoPoliza.CANCELADA;
        this.fechaCancelacion = ahora;
        riesgos.stream().filter(Riesgo::estaActivo).forEach(riesgo -> riesgo.marcarCancelado(ahora));
    }

    /** Agrega un riesgo. Solo aplica para pólizas colectivas no canceladas. */
    public void agregarRiesgo(Riesgo riesgo) {
        if (tipo != TipoPoliza.COLECTIVA) {
            throw new ReglaNegocioException(
                    "Solo se pueden agregar riesgos a pólizas colectivas; la póliza %s es %s".formatted(numero, tipo));
        }
        validarNoCancelada("agregar riesgos a");
        vincular(riesgo);
    }

    /**
     * Cancela un riesgo de la póliza. En una póliza individual el riesgo es único y
     * no puede quedar sin él: en ese caso se debe cancelar la póliza completa.
     */
    public void cancelarRiesgo(Riesgo riesgo) {
        if (!riesgos.contains(riesgo)) {
            throw new ReglaNegocioException("El riesgo no pertenece a la póliza %s".formatted(numero));
        }
        if (tipo == TipoPoliza.INDIVIDUAL) {
            throw new ReglaNegocioException(
                    "La póliza individual %s tiene un único riesgo; para retirarlo cancele la póliza".formatted(numero));
        }
        validarNoCancelada("cancelar riesgos de");
        if (!riesgo.estaActivo()) {
            throw new EstadoInvalidoException("El riesgo %d ya se encuentra cancelado".formatted(riesgo.getId()));
        }
        riesgo.marcarCancelado(Instant.now());
    }

    private void vincular(Riesgo riesgo) {
        Objects.requireNonNull(riesgo, "riesgo");
        if (tipo == TipoPoliza.INDIVIDUAL && !riesgos.isEmpty()) {
            throw new ReglaNegocioException("Una póliza individual solo puede tener un riesgo");
        }
        riesgo.asignarPoliza(this);
        riesgos.add(riesgo);
    }

    private void validarNoCancelada(String accion) {
        if (estado == EstadoPoliza.CANCELADA) {
            throw new EstadoInvalidoException("No se puede %s la póliza %s porque está cancelada".formatted(accion, numero));
        }
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

    public String getNumero() {
        return numero;
    }

    public TipoPoliza getTipo() {
        return tipo;
    }

    public EstadoPoliza getEstado() {
        return estado;
    }

    public Persona getTomador() {
        return tomador;
    }

    public LocalDate getFechaInicioVigencia() {
        return fechaInicioVigencia;
    }

    public LocalDate getFechaFinVigencia() {
        return fechaFinVigencia;
    }

    public int getMesesVigencia() {
        return mesesVigencia;
    }

    public BigDecimal getValorCanon() {
        return valorCanon;
    }

    public BigDecimal getValorPrima() {
        return valorPrima;
    }

    public Instant getFechaCancelacion() {
        return fechaCancelacion;
    }

    public List<Riesgo> getRiesgos() {
        return Collections.unmodifiableList(riesgos);
    }

    public Long getVersion() {
        return version;
    }
}
