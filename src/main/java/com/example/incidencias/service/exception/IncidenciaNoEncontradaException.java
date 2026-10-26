package com.example.incidencias.service.exception;

public class IncidenciaNoEncontradaException extends RuntimeException {

    public IncidenciaNoEncontradaException(Long id) {
        super("No existe la incidencia con id " + id);
    }
}
