package com.example.incidencias.web;

import com.example.incidencias.service.IncidenciaService;
import com.example.incidencias.web.dto.UsuarioResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Listado de usuarios para poder asignar incidencias desde el formulario de alta.
 * Es una cosa opcional que realize y lo necesita el frontend.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final IncidenciaService servicio;

    public UsuarioController(IncidenciaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return servicio.listarUsuarios();
    }
}
