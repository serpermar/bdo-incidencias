package com.example.incidencias.service.exception;

public class TransicionEstadoInvalidaException extends RuntimeException {

    public TransicionEstadoInvalidaException(Long id) {
        super("La incidencia " + id + " está CERRADA y no puede reabrirse");
    }
}
