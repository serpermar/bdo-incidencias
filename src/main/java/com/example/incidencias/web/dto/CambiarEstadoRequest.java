package com.example.incidencias.web.dto;

import com.example.incidencias.domain.Estado;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRequest(

        @NotNull(message = "El estado es obligatorio")
        Estado estado
) {
}
