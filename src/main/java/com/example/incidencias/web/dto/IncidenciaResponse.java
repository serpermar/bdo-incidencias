package com.example.incidencias.web.dto;

import com.example.incidencias.domain.Estado;
import com.example.incidencias.domain.Incidencia;
import com.example.incidencias.domain.Prioridad;

import java.time.LocalDateTime;

public record IncidenciaResponse(
        Long id,
        String titulo,
        String descripcion,
        Prioridad prioridad,
        Estado estado,
        LocalDateTime fechaCreacion,
        UsuarioResponse usuarioAsignado
) {

    public static IncidenciaResponse from(Incidencia incidencia) {
        return new IncidenciaResponse(
                incidencia.getId(),
                incidencia.getTitulo(),
                incidencia.getDescripcion(),
                incidencia.getPrioridad(),
                incidencia.getEstado(),
                incidencia.getFechaCreacion(),
                UsuarioResponse.from(incidencia.getUsuarioAsignado())
        );
    }
}
