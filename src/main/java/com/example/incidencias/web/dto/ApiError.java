package com.example.incidencias.web.dto;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Formato único de error para toda la API. Todos los errores (400, 404, 409, 500)
 * devuelven exactamente esta estructura.
 */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> detalles
) {

    public static ApiError of(HttpStatus status, String message, String path, List<String> detalles) {
        return new ApiError(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                detalles == null ? List.of() : List.copyOf(detalles)
        );
    }
}
