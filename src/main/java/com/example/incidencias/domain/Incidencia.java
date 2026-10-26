package com.example.incidencias.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incidencias")
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String titulo;

    @Column(length = 1000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Prioridad prioridad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Estado estado;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuarioAsignado;

    @Version
    @Column(nullable = false)
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Categoria categoria = Categoria.OTROS;

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Column(name = "auditado_por", length = 80)
    private String auditadoPor;

    @Column(name = "fecha_ultima_modificacion_estado")
    private LocalDateTime fechaUltimaModificacionEstado;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "incidencia_id")
    @OrderBy("fecha ASC")
    private List<ComentarioIncidencia> comentarios = new ArrayList<>();

    protected Incidencia() {
        // Requerido por JPA
    }

    /**
     * Toda incidencia nace ABIERTA: es una invariante del dominio, no algo que
     * el cliente de la API pueda elegir.
     */
    public Incidencia(String titulo, String descripcion, Prioridad prioridad, Usuario usuarioAsignado) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.prioridad = prioridad;
        this.usuarioAsignado = usuarioAsignado;
        this.estado = Estado.ABIERTA;
        this.fechaCreacion = LocalDateTime.now();
        this.categoria = Categoria.OTROS;
    }

    public Incidencia(String titulo, String descripcion, Prioridad prioridad, Usuario usuarioAsignado,
                      Categoria categoria, LocalDate fechaLimite) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.prioridad = prioridad;
        this.usuarioAsignado = usuarioAsignado;
        this.estado = Estado.ABIERTA;
        this.fechaCreacion = LocalDateTime.now();
        this.categoria = categoria == null ? Categoria.OTROS : categoria;
        this.fechaLimite = fechaLimite;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Prioridad getPrioridad() {
        return prioridad;
    }

    public Estado getEstado() {
        return estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public Usuario getUsuarioAsignado() {
        return usuarioAsignado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria == null ? Categoria.OTROS : categoria;
    }

    public LocalDate getFechaLimite() {
        return fechaLimite;
    }

    public void setFechaLimite(LocalDate fechaLimite) {
        this.fechaLimite = fechaLimite;
    }

    public String getAuditadoPor() {
        return auditadoPor;
    }

    public void setAuditadoPor(String auditadoPor) {
        this.auditadoPor = auditadoPor;
    }

    public LocalDateTime getFechaUltimaModificacionEstado() {
        return fechaUltimaModificacionEstado;
    }

    public void setFechaUltimaModificacionEstado(LocalDateTime fechaUltimaModificacionEstado) {
        this.fechaUltimaModificacionEstado = fechaUltimaModificacionEstado;
    }

    public List<ComentarioIncidencia> getComentarios() {
        return comentarios;
    }

    public void agregarComentario(ComentarioIncidencia comentario) {
        if (comentario != null) {
            this.comentarios.add(comentario);
        }
    }
}
