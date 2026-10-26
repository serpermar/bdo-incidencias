package com.example.incidencias.web.dto;

import com.example.incidencias.domain.Categoria;
import com.example.incidencias.domain.Estado;
import com.example.incidencias.domain.Incidencia;
import com.example.incidencias.domain.Prioridad;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record IncidenciaResponse(
        Long id,
        String titulo,
        String descripcion,
        Prioridad prioridad,
        Estado estado,
        LocalDateTime fechaCreacion,
        UsuarioResponse usuarioAsignado,
        Categoria categoria,
        LocalDate fechaLimite,
        String auditadoPor,
        LocalDateTime fechaUltimaModificacionEstado
) {

    public static IncidenciaResponse from(Incidencia incidencia) {
        return new IncidenciaResponse(
                incidencia.getId(),
                incidencia.getTitulo(),
                incidencia.getDescripcion(),
                incidencia.getPrioridad(),
                incidencia.getEstado(),
                incidencia.getFechaCreacion(),
                UsuarioResponse.from(incidencia.getUsuarioAsignado()),
                incidencia.getCategoria(),
                incidencia.getFechaLimite(),
                incidencia.getAuditadoPor(),
                incidencia.getFechaUltimaModificacionEstado()
        );
    }
}
