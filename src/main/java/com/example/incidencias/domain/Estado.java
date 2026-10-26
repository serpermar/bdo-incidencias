package com.example.incidencias.domain;

/**
 * Estado de una incidencia. El flujo permitido es ABIERTA -> EN_CURSO -> CERRADA,
 * y una incidencia CERRADA no puede volver a abrirse (regla de negocio del servicio).
 */
public enum Estado {
    ABIERTA,
    EN_CURSO,
    CERRADA
}
