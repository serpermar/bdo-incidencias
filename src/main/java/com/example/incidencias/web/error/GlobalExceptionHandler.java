package com.example.incidencias.web.error;

import com.example.incidencias.service.exception.IncidenciaNoEncontradaException;
import com.example.incidencias.service.exception.TransicionEstadoInvalidaException;
import com.example.incidencias.service.exception.UsuarioNoEncontradoException;
import com.example.incidencias.web.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Traduce las excepciones a un JSON de error homogéneo (ver {@link ApiError}).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({IncidenciaNoEncontradaException.class, UsuarioNoEncontradoException.class})
    public ResponseEntity<ApiError> noEncontrado(RuntimeException ex, HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(TransicionEstadoInvalidaException.class)
    public ResponseEntity<ApiError> conflicto(TransicionEstadoInvalidaException ex, HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::describirError)
                .toList();

        return construir(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos", request, detalles);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> cuerpoIlegible(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String mensaje = "El cuerpo de la petición no es válido. Revisa el JSON y los valores permitidos "
                + "(prioridad: BAJA, MEDIA, ALTA; estado: ABIERTA, EN_CURSO, CERRADA)";
        return construir(HttpStatus.BAD_REQUEST, mensaje, request, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> parametroInvalido(MethodArgumentTypeMismatchException ex,
                                                      HttpServletRequest request) {
        String mensaje = "El valor '" + ex.getValue() + "' no es válido para el parámetro '" + ex.getName() + "'";
        return construir(HttpStatus.BAD_REQUEST, mensaje, request, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> recursoNoEncontrado(NoResourceFoundException ex, HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, "El recurso solicitado no existe", request, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> metodoNoSoportado(HttpRequestMethodNotSupportedException ex,
                                                       HttpServletRequest request) {
        return construir(HttpStatus.METHOD_NOT_ALLOWED,
                "El método " + ex.getMethod() + " no está permitido en esta ruta", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> inesperado(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR,
                "Se ha producido un error interno. Inténtalo de nuevo más tarde.", request, List.of());
    }

    private static String describirError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private static ResponseEntity<ApiError> construir(HttpStatus estado, String mensaje,
                                                      HttpServletRequest request, List<String> detalles) {
        ApiError error = ApiError.of(estado, mensaje, request.getRequestURI(), detalles);
        return ResponseEntity.status(estado).body(error);
    }
}
