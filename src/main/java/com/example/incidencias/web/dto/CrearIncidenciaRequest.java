package com.example.incidencias.web.dto;

import com.example.incidencias.domain.Categoria;
import com.example.incidencias.domain.Prioridad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Datos de entrada para crear una incidencia.
 * El estado no se acepta en el alta: siempre se crea ABIERTA.
 */
public record CrearIncidenciaRequest(

        @NotBlank(message = "El título es obligatorio")
        @Size(max = 100, message = "El título no puede superar los 100 caracteres")
        String titulo,

        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        String descripcion,

        @NotNull(message = "La prioridad es obligatoria")
        Prioridad prioridad,

        Long usuarioAsignadoId,

        Categoria categoria,

        LocalDate fechaLimite
) {
}
